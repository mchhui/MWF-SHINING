package com.modularwarfare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.modularwarfare.addon.AddonLoaderManager;
import com.modularwarfare.addon.LibClassLoader;
import com.modularwarfare.api.ItemRegisterEvent;
import com.modularwarfare.client.customplayer.CPEventHandler;
import com.modularwarfare.client.customplayer.CustomPlayerConfig;
import com.modularwarfare.client.fpp.enhanced.AnimationType.AnimationTypeJsonAdapter.AnimationTypeException;
import com.modularwarfare.common.CommonProxy;
import com.modularwarfare.common.MWTab;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.commands.CommandClear;
import com.modularwarfare.common.commands.CommandDebug;
import com.modularwarfare.common.commands.CommandNBT;
import com.modularwarfare.common.commands.CommandPlay;
import com.modularwarfare.common.commands.kits.CommandKit;
import com.modularwarfare.common.entity.EntityExplosiveProjectile;
import com.modularwarfare.common.entity.decals.EntityBulletHole;
import com.modularwarfare.common.entity.decals.EntityShell;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.entity.grenades.EntityStunGrenade;
import com.modularwarfare.common.entity.item.EntityItemLoot;
import com.modularwarfare.common.extra.ItemLight;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.CommonEventHandler;
import com.modularwarfare.common.handler.GuiHandler;
import com.modularwarfare.common.hitbox.playerdata.PlayerDataHandler;
import com.modularwarfare.common.network.NetworkHandler;
import com.modularwarfare.common.playerstate.PlayerStateManager;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.common.type.ContentTypes;
import com.modularwarfare.common.type.TypeEntry;
import com.modularwarfare.raycast.DefaultRayCasting;
import com.modularwarfare.raycast.RayCasting;
import com.modularwarfare.script.ScriptHost;
import com.modularwarfare.utility.GSONUtils;
import com.modularwarfare.utility.ModUtil;
import com.modularwarfare.utility.ZipContentPack;
import mchhui.modularmovements.ModularMovements;
import moe.komi.mwprotect.IZip;
import moe.komi.mwprotect.IZipEntry;
import moe.komi.mwprotect.LegacyZip;
import net.minecraft.item.Item;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.registries.IForgeRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

import static com.modularwarfare.common.CommonProxy.zipJar;

@Mod(
    modid = ModularWarfare.MOD_ID,
    name = ModularWarfare.MOD_NAME,
    version = ModularWarfare.MOD_VERSION,
    acceptedMinecraftVersions = "[1.12,1.13)"
)
public class ModularWarfare {

    // Mod Info
    public static final String MOD_ID = "modularwarfare";
    public static final String MOD_NAME = "ModularWarfare";
    public static final String MOD_VERSION = "2024.2.4.5f";
    public static final String MOD_PREFIX = TextFormatting.GRAY+"["+TextFormatting.RED+"ModularWarfare"+TextFormatting.GRAY+"]"+TextFormatting.GRAY;

    // Main instance
    @Instance(ModularWarfare.MOD_ID)
    public static ModularWarfare INSTANCE;

    @SidedProxy(clientSide = "com.modularwarfare.client.ClientProxy", serverSide = "com.modularwarfare.common.CommonProxy")
    public static CommonProxy PROXY;

    public static boolean DEV_ENV = true;


    public static Logger LOGGER;

    public static NetworkHandler NETWORK;

    public static PlayerDataHandler PLAYER_HANDLER = new PlayerDataHandler();

    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static HashMap<String, ZipContentPack> zipContentsPack = new HashMap<>();

    // The ModularWarfare directory
    public static File CONTENT_DIR;
    public static List<File> contentPacks = new ArrayList<>();

    // Arrays for the varied types
    public static HashMap<String, ItemGun> gunTypes = new HashMap<>();
    public static HashMap<String, ItemAmmo> ammoTypes = new HashMap<>();
    public static HashMap<String, ItemAttachment> attachmentTypes = new HashMap<>();
    public static LinkedHashMap<String, ItemMWArmor> armorTypes = new LinkedHashMap<>();
    public static LinkedHashMap<String, ItemSpecialArmor> specialArmorTypes = new LinkedHashMap<>();
    public static HashMap<String, ItemBullet> bulletTypes = new HashMap<>();
    public static HashMap<String, ItemSpray> sprayTypes = new HashMap<>();
    public static HashMap<String, ItemBackpack> backpackTypes = new HashMap<>();
    public static HashMap<String, ItemGrenade> grenadeTypes = new HashMap<>();
    public static HashMap<String, TextureType> textureTypes = new HashMap<>();

    public static ArrayList<BaseType> baseTypes = new ArrayList<>();

    public static ArrayList<String> contentPackHashList= new ArrayList<>();
    public static boolean usingDirectoryContentPack=false;

    public static HashMap<String, MWTab> MODS_TABS = new HashMap<>();
    
    public static ArrayList<Runnable> preloadTasklist= new ArrayList<>();

    /**
     * Custom RayCasting
     */
    public RayCasting RAY_CASTING;

    public static final LibClassLoader LOADER = new LibClassLoader(ModularWarfare.class.getClassLoader());

    /**
     * ModularWarfare Addon System
     */
    public static File addonDir;
    public static AddonLoaderManager loaderManager;

    public static boolean isLoadedModularMovements = false;

    public static IZip getiZip(File file) throws IOException {
        try {
            Class<?> protectorClass = Class.forName("moe.komi.mwprotect.cipher.ProtectZip");
            Constructor<?> constructor = protectorClass.getConstructor(File.class);
            return (IZip) constructor.newInstance(file);
        }
        catch (InvocationTargetException e) {
            LOGGER.error("Failed to construct ProtectZip", e);
        }
        catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException ignored) {
            // Pass
        }
        return new LegacyZip(file);
    }


    public static void loadContent() {
        usingDirectoryContentPack = false;
        contentPacks.forEach(file -> {
            if(file.isDirectory()) {
                usingDirectoryContentPack=true;
            } else {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer, 0, 1024)) != -1) {
                        md.update(buffer, 0, length);
                    }
                    StringBuilder md5 = new StringBuilder();
                    for(byte b : md.digest()) {
                        md5.append(b);
                    }
                    contentPackHashList.add(md5.toString());
                } catch (IOException | NoSuchAlgorithmException ignored) {
                    // Pass
                }
            }
        });
        contentPacks.forEach(file -> {
            if (!MODS_TABS.containsKey(file.getName())) {
                MODS_TABS.put(file.getName(), new MWTab(file.getName()));
            }
            if (zipJar.matcher(file.getName()).matches()) {
                if (!zipContentsPack.containsKey(file.getName())) {
                    try {
                        IZip izip;
                        if (FMLCommonHandler.instance().getSide().isClient()) {
                            izip = getiZip(file);
                        } else {
                            izip = new LegacyZip(file);
                        }
                        ZipContentPack zipContentPack = new ZipContentPack(file.getName(), izip.getFileList(), izip);
                        zipContentsPack.put(file.getName(), zipContentPack);
                        ModularWarfare.LOGGER.info("Registered content pack");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        getTypeFiles(contentPacks);
    }

    /**
     * Sorts all type files into their proper arraylist
     */
    public static void loadContentPacks(boolean reload) {

        loadContent();

        if (DEV_ENV) {
            PROXY.generateJsonModels(baseTypes);
        }

        for(TextureType type : textureTypes.values()){
            type.loadExtraValues();
        }

        for (BaseType baseType : baseTypes) {
            baseType.loadExtraValues();
            ContentTypes.values.get(baseType.id).typeAssignFunction.accept(baseType, reload);
        }

        if (DEV_ENV) {
            if (reload)
                return;
            //PROXY.generateJsonSounds(gunTypes.values(), DEV_ENV);
            PROXY.generateLangFiles(baseTypes, DEV_ENV);
        }
        System.gc();
    }

    /**
     * Gets all the render config json for each gun
     */
    public static <T> T getRenderConfig(BaseType baseType, Class<T> typeClass) {
        if (baseType.isInDirectory) {
            try {
                File contentPackDir = new File(ModularWarfare.CONTENT_DIR, baseType.contentPack);
                if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                    File renderConfig = new File(contentPackDir, "/" + baseType.getAssetDir() + "/render");
                    File typeRender = new File(renderConfig, baseType.internalName + ".render.json");
                    JsonReader jsonReader = new JsonReader(new FileReader(typeRender));
                    return GSONUtils.fromJson(gson, jsonReader, typeClass, baseType.internalName + ".render.json");
                }
            } catch (JsonParseException | FileNotFoundException e){
                e.printStackTrace();
            } catch (AnimationTypeException err) {
                ModularWarfare.LOGGER.info(baseType.internalName + " was loaded. But something was wrong.");
                err.printStackTrace();
            }
        } else {
            if (zipContentsPack.containsKey(baseType.contentPack)) {
                String typeName = baseType.getAssetDir();

                IZipEntry foundFile = zipContentsPack.get(baseType.contentPack).fileHeaders.stream().filter(fileHeader -> fileHeader.getFileName().startsWith(typeName + "/" + "render/") && fileHeader.getFileName().replace(typeName + "/render/", "").equalsIgnoreCase(baseType.internalName + ".render.json")).findFirst().orElse(null);
                if (foundFile != null) {
                    try {
                        InputStream stream = foundFile.getInputStream();
                        JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));
                        return GSONUtils.fromJson(gson, jsonReader, typeClass, baseType.internalName + ".render.json");
                    } catch (JsonParseException | IOException e){
                        e.printStackTrace();
                    } catch (AnimationTypeException err) {
                        ModularWarfare.LOGGER.info(baseType.internalName + " was loaded. But something was wrong.");
                        err.printStackTrace();
                    }
                } else {
                    ModularWarfare.LOGGER.info(baseType.internalName + ".render.json not found. Aborting");
                }
            }
        }
        return null;
    }

    /**
     * Gets all type files from the content packs
     */
    private static void getTypeFiles(List<File> contentPacks) {
        ScriptHost.INSTANCE.reset();
        
        for (File file : contentPacks) {
            if (file.getName().contains("cache")) {
                continue;
            }

            if (file.isDirectory()) {
                for (TypeEntry type : ContentTypes.values) {
                    File subFolder = new File(file, "/" + type.name + "/");
                    if (subFolder.exists()) {
                        for (File typeFile : subFolder.listFiles()) {
                            try {
                                if (typeFile.isFile()) {
                                    JsonReader jsonReader = new JsonReader(new FileReader(typeFile));
                                    BaseType parsedType = GSONUtils.fromJson(gson, jsonReader, type.typeClass, typeFile.getName());

                                    parsedType.id = type.id;
                                    parsedType.contentPack = file.getName();
                                    parsedType.isInDirectory = true;
                                    baseTypes.add(parsedType);

                                    if (parsedType instanceof TextureType) {
                                        textureTypes.put(parsedType.internalName, (TextureType) parsedType);
                                    }
                                }
                            } catch (JsonParseException | FileNotFoundException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
                /*
                 * LOAD SCRIPT START
                 */
                File scriptFolder = new File(file, "/sciprt/");
                if (scriptFolder.exists()) {
                    for (File typeFile : scriptFolder.listFiles()) {
                        if(typeFile.getName().endsWith(".js")) {
                            String text="";
                            try {
                                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(file),Charset.forName("UTF-8")));
                                String temp;
                                while((temp=bufferedReader.readLine())!=null) {
                                    text+=temp;
                                }
                                bufferedReader.close();
                                ScriptHost.INSTANCE.initScript(new ResourceLocation(ModularWarfare.MOD_ID,"script/"+typeFile.getName()+".js"), text);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
                /*
                 * LOAD SCRIPT END
                 */
                /*
                 * LOAD CUSTOM PLAYER START
                 */
                CPEventHandler.cpConfig.clear();
                File cpFolder = new File(file, "/customplayer/");
                if (cpFolder.exists()) {
                    for (File typeFile : cpFolder.listFiles()) {
                        System.out.println("test1:"+typeFile.getName());
                        if(typeFile.getName().endsWith(".json")) {
                            String text="";
                            try {
                                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(new FileInputStream(file),Charset.forName("UTF-8")));
                                String temp;
                                while((temp=bufferedReader.readLine())!=null) {
                                    text+=temp;
                                }
                                bufferedReader.close();
                                CustomPlayerConfig cp=gson.fromJson(text, CustomPlayerConfig.class);
                                CPEventHandler.cpConfig.put(cp.name, cp);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
                /*
                 * LOAD CUSTOM PLAYER END
                 */
            } else {
                if (zipContentsPack.containsKey(file.getName())) {
                    for (IZipEntry fileHeader : zipContentsPack.get(file.getName()).fileHeaders) {
                        for (TypeEntry type : ContentTypes.values) {
                            final String zipName = fileHeader.getFileName();
                            final String typeName = type.toString();
                            if (zipName.startsWith(typeName + "/") && zipName.split(typeName + "/").length > 1 && zipName.split(typeName + "/")[1].length() > 0 && !zipName.contains("render")) {
                                InputStream stream = null;
                                try {
                                    stream = fileHeader.getInputStream();
                                    JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));

                                    try {
                                        BaseType parsedType = (BaseType) GSONUtils.fromJson(gson, jsonReader, type.typeClass, fileHeader.getFileName());
                                        if (parsedType.internalName.equals("siz_bg.scope_win94_texture")) {
                                            FMLLog.log.info("found - " + parsedType.internalName + " - " + file.getName() + " - " + fileHeader.getFileName() + " - " + fileHeader.getHandle());
                                        }
                                        parsedType.id = type.id;
                                        parsedType.contentPack = file.getName();
                                        parsedType.isInDirectory = false;
                                        baseTypes.add(parsedType);

                                        if(parsedType instanceof TextureType){
                                            textureTypes.put(parsedType.internalName, (TextureType) parsedType);
                                        }
                                    } catch (JsonParseException ignored) {
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        /*
                         * LOAD SCRIPT START
                         */
                        String zipName = fileHeader.getFileName();
                        if(zipName.startsWith("script/")&&zipName.endsWith(".js")) {
                            String typeFile=zipName.replaceFirst("script/", "").replace(".js", "");
                            String text="";
                            try {
                                InputStream inputStream=fileHeader.getInputStream();
                                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream,Charset.forName("UTF-8")));
                                String temp;
                                while((temp=bufferedReader.readLine())!=null) {
                                    text+=temp;
                                }
                                bufferedReader.close();
                                ScriptHost.INSTANCE.initScript(new ResourceLocation(ModularWarfare.MOD_ID,"script/"+typeFile+".js"), text);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        /*
                         * LOAD SCRIPT END
                         */
                        /*
                         * LOAD CUSTOM PLAYER START
                         */
                        zipName = fileHeader.getFileName();
                        if(zipName.startsWith("customplayer/")&&zipName.endsWith(".json")) {
                            String typeFile=zipName.replaceFirst("customplayer/", "").replace(".json", "");
                            String text="";
                            try {
                                InputStream inputStream=fileHeader.getInputStream();
                                BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream,Charset.forName("UTF-8")));
                                String temp;
                                while((temp=bufferedReader.readLine())!=null) {
                                    text+=temp;
                                }
                                bufferedReader.close();
                                CustomPlayerConfig cp=gson.fromJson(text, CustomPlayerConfig.class);
                                CPEventHandler.cpConfig.put(cp.name, cp);
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        /*
                         * LOAD CUSTOM PLAYER END
                         */
                    }
                }
            }
        }
    }

    // Registers items, blocks, renders, etc
    @Mod.EventHandler
    private void onPreInitialization(FMLPreInitializationEvent event) {
        
        // JACKSON 兼容处理
        if (getClass().getClassLoader() instanceof LaunchClassLoader) {
            LaunchClassLoader loader = (LaunchClassLoader) getClass().getClassLoader();
            loader.addTransformerExclusion("com.fasterxml.jackson.");
            try {
                Field f = LaunchClassLoader.class.getDeclaredField("invalidClasses");
                f.setAccessible(true);
                Set<String> invalidClasses = (Set<String>) f.get(getClass().getClassLoader());
                invalidClasses.remove("com.fasterxml.jackson.databind.ObjectMapper");
            } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        
        PROXY.preload();

        if (FMLCommonHandler.instance().getSide().isServer()) {
            // Creates directory if it doesn't exist
            CONTENT_DIR = new File(event.getModConfigurationDirectory().getParentFile(), "ModularWarfare");
            if (!CONTENT_DIR.exists() && CONTENT_DIR.mkdir()) {
                LOGGER.info("Created ModularWarfare folder, it's recommended to install content packs.");
                LOGGER.info("As the mod itself doesn't come with any content.");
            }
            loadConfig();
            DEV_ENV = true;

            contentPacks = PROXY.getContentList();
        }

        NetworkRegistry.INSTANCE.newEventDrivenChannel("MWF_sync_customInventory_" + (ModConfig.INSTANCE.general.customInventory ? "enabled" : "disabled"));
        NetworkRegistry.INSTANCE.newEventDrivenChannel("MWF_sync_allowGunModifyGui_" + (ModConfig.INSTANCE.general.allowGunModifyGui ? "enabled" : "disabled"));

        registerRayCasting(new DefaultRayCasting());
        loaderManager.preInitAddons(event);

        // Loads Content Packs
        ContentTypes.registerTypes();
        loadContentPacks(false);

        // Client side loading
        //PROXY.forceReload();

        PROXY.registerEventHandlers();

        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerStateManager());
        MinecraftForge.EVENT_BUS.register(this);

    }
    
    public static void loadConfig() {
        new ModConfig(new File(CONTENT_DIR, "mod_config.json"));
        if(isLoadedModularMovements) {
            ModularMovements.loadConfig();
        }
    }

    // Register events, imc, and world stuff
    @Mod.EventHandler
    private void onInitialization(FMLInitializationEvent event) {
        PROXY.load();

        final String property = System.getProperty("mwf.banbukkit", "false");
        final boolean enableBukkit = !Boolean.parseBoolean(property);
        if (enableBukkit) {
            try {
                Class.forName("org.bukkit.Bukkit");
            }
            catch (ClassNotFoundException e) {
                LOGGER.info("Bukkit extension not found, skipping initialization");
            }
            MinecraftForge.EVENT_BUS.register(BukkitHelper.class);
        }

        NETWORK = new NetworkHandler();
        NETWORK.initialise();
        NetworkRegistry.INSTANCE.registerGuiHandler(ModularWarfare.INSTANCE, new GuiHandler());
        loaderManager.initAddons(event);
    }

    // Last loading things
    @Mod.EventHandler
    private void onPostInitialization(FMLPostInitializationEvent event) {
        NETWORK.postInitialise();
        PROXY.init();
        loaderManager.postInitAddons(event);
    }

    // Registers commands and server sided regions
    @Mod.EventHandler
    private void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandClear());
        event.registerServerCommand(new CommandNBT());
        event.registerServerCommand(new CommandDebug());
        event.registerServerCommand(new CommandKit());
        event.registerServerCommand(new CommandPlay());
    }

    // Registers protected content-pack before preInit, to allow making a custom ResourcePackLoader allowing protected .zip
    @Mod.EventHandler
    private void constructionEvent(FMLConstructionEvent event) {
        LOGGER = LogManager.getLogger(ModularWarfare.MOD_ID);
        /*
         * Create & Check Addon System
         */

        addonDir = new File(ModUtil.getGameFolder() + "/addons_mwf_shining");

        if (!addonDir.exists() && !addonDir.mkdirs()) {
            LOGGER.error("Failed to create Addon Directory");
        }
        loaderManager = new AddonLoaderManager();
        loaderManager.constructAddons(addonDir, event.getSide());

        /*
         * Load the addon from the gradle project compilation (.class folder) instead of final .jar
         * in order to allow HotSwap changes
         */
        if(FMLLaunchHandler.isDeobfuscatedEnvironment()) {
            File file = new File(ModUtil.getGameFolder()).getParentFile().getParentFile();
            String folder = file.toString().replace("\\", "/");
            loaderManager.constructDevAddons(new File(folder + "/melee-addon/build/classes/java/main"), "com.modularwarfare.melee.ModularWarfareMelee", event.getSide());
        }

        PROXY.construction(event);
    }

    @SubscribeEvent
    void registerItems(RegistryEvent.Register<Item> evt) {
        final IForgeRegistry<Item> registry = evt.getRegistry();
        contentPacks.forEach(file -> {
            final String fname = file.getName();
            final List<Item> tabOrder = new ArrayList<>();

            Stream.of(gunTypes, ammoTypes, bulletTypes, attachmentTypes, specialArmorTypes, sprayTypes, backpackTypes, grenadeTypes)
                .map(HashMap::values)
                .flatMap(Collection::stream)
                .filter(it -> it.baseType.contentPack.equals(fname))
                .forEachOrdered(it -> {
                    registry.register(it);
                    tabOrder.add(it);
                });

            armorTypes.values().stream()
                .filter(it -> it.type.contentPack.equals(fname))
                .forEachOrdered(it -> {
                    registry.register(it);
                    tabOrder.add(it);
                });

            ItemRegisterEvent itemRegisterEvent = new ItemRegisterEvent(registry, tabOrder);
            MinecraftForge.EVENT_BUS.post(itemRegisterEvent);

            itemRegisterEvent.tabOrder.forEach(item -> {
                if (item instanceof ItemGun) {
                    final ItemGun itemGun = (ItemGun) item;
                    final GunType type = itemGun.type;
                    for(SkinType skin: type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, type);
                    }
                    CommonProxy.preloadFlashTex.add(type.flashType);
                }
                else if (item instanceof ItemBullet) {
                    final ItemBullet itemBullet = (ItemBullet) item;
                    final BulletType type = itemBullet.type;
                    for(SkinType skin: type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, type);
                    }
                }
                else if (item instanceof ItemMWArmor) {
                    final ItemMWArmor itemArmor = (ItemMWArmor) item;
                    final ArmorType type = itemArmor.type;
                    for(SkinType skin: type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, type);
                    }
                }
            });

            MODS_TABS.get(fname).preInitialize(tabOrder);
        });

        registry.register(new ItemLight("light"));
    }

    @SubscribeEvent
    void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "bullethole"), EntityBulletHole.class, "bullethole", 3, this, 80, 10, false);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "shell"), EntityShell.class, "shell", 4, this, 64, 1, false);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "itemloot"), EntityItemLoot.class, "itemloot", 6, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "grenade"), EntityGrenade.class, "grenade", 7, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "smoke_grenade"), EntitySmokeGrenade.class, "smoke_grenade", 8, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "stun_grenade"), EntityStunGrenade.class, "stun_grenade", 9, this, 64, 1, true);

        //EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "bullet"), EntityBullet.class, "bullet", 15, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "explosive_projectile"), EntityExplosiveProjectile.class, "explosive_projectile", 15, this, 80, 1, true);
    }

    public static void registerRayCasting(RayCasting rayCasting) {
        INSTANCE.RAY_CASTING = rayCasting;
    }
}
