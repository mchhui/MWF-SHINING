package com.modularwarfare;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.modularwarfare.addon.AddonLoaderManager;
import com.modularwarfare.addon.LibClassLoader;
import com.modularwarfare.api.ItemRegisterEvent;
import com.modularwarfare.client.fpp.enhanced.AnimationType.AnimationTypeJsonAdapter.AnimationTypeException;
import com.modularwarfare.common.CommonProxy;
import com.modularwarfare.common.MWTab;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.commands.CommandClear;
import com.modularwarfare.common.commands.CommandDebug;
import com.modularwarfare.common.commands.CommandNBT;
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
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.hitbox.playerdata.PlayerDataHandler;
import com.modularwarfare.common.network.NetworkHandler;
import com.modularwarfare.common.protector.ModularProtector;
import com.modularwarfare.common.protector.ModularProtectorOfficial;
import com.modularwarfare.common.protector.ModularProtectorTemplate;
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
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static com.modularwarfare.common.CommonProxy.zipJar;

@Mod(modid = ModularWarfare.MOD_ID, name = ModularWarfare.MOD_NAME, version = ModularWarfare.MOD_VERSION, dependencies = "required-client:mcgltf@[1.12.2-Forge-2.0.3.0,);")
public class ModularWarfare {

    /**
     * Mod Info
     */
    public static final String MOD_ID = "modularwarfare";
    public static final String MOD_NAME = "ModularWarfare";
    public static final String MOD_VERSION = "2023.2.4.2f";
    public static final String MOD_PREFIX = TextFormatting.GRAY + "[" + TextFormatting.RED + "ModularWarfare" + TextFormatting.GRAY + "]" + TextFormatting.GRAY;
    public static final LibClassLoader LOADER = new LibClassLoader(ModularWarfare.class.getClassLoader());
    /**
     * Main Instance
     */
    @Instance(ModularWarfare.MOD_ID)
    public static ModularWarfare INSTANCE;
    /**
     * Proxy
     */
    @SidedProxy(clientSide = "com.modularwarfare.client.ClientProxy", serverSide = "com.modularwarfare.common.CommonProxy")
    public static CommonProxy PROXY;
    /**
     * Dev Environment
     */
    public static boolean DEV_ENV = true;
    /**
     * Logger
     */
    public static Logger LOGGER;
    /**
     * Network Handler
     */
    public static NetworkHandler NETWORK;
    public static ModularProtector PROTECTOR;
    public static PlayerDataHandler PLAYER_HANDLER = new PlayerDataHandler();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static HashMap<String, ZipContentPack> zipContentsPack = new HashMap<>();
    /**
     * The ModularWarfare directory
     */
    public static File MOD_DIR;
    public static List<File> contentPacks = new ArrayList<>();
    /**
     * Arrays for the varied types
     */
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
    public static ArrayList<String> contentPackHashList = new ArrayList<>();
    public static boolean usingDirectoryContentPack = false;
    public static HashMap<String, MWTab> MODS_TABS = new HashMap<>();
    /**
     * ModularWarfare Addon System
     */
    public static File addonDir;
    public static AddonLoaderManager loaderManager;
    public static boolean isLoadedModularMovements = false;
    /**
     * Custom RayCasting
     */
    public RayCasting RAY_CASTING;

    public static void loadContent() {
        usingDirectoryContentPack = false;
        for (File file : contentPacks) {
            if (!file.isDirectory()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer, 0, 1024)) != -1) {
                        md.update(buffer, 0, length);
                    }
                    StringBuilder md5 = new StringBuilder();
                    for (byte b : md.digest()) {
                        md5.append(b);
                    }
                    contentPackHashList.add(md5.toString());
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            } else {
                usingDirectoryContentPack = true;
            }
        }
        for (File file : contentPacks) {
            if (!MODS_TABS.containsKey(file.getName())) {
                MODS_TABS.put(file.getName(), new MWTab(file.getName()));
            }
            if (!zipJar.matcher(file.getName()).matches()) {
                continue;
            }

            if (zipContentsPack.containsKey(file.getName())) {
                continue;
            }

            try {
                ZipFile zipFile = new ZipFile(file);
                System.out.println(file.getName());

                /* Set password */
                if (zipFile.isEncrypted()) {
                    if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                        ModularWarfare.PROTECTOR.decryptAlternateFile(zipFile, file.getName());
                    } else {
                        ModularWarfare.LOGGER.info("Can't use password protected content-pack on server-side.");
                    }
                }

                ZipContentPack zipContentPack = new ZipContentPack(file.getName(), zipFile.getFileHeaders(), zipFile);
                zipContentsPack.put(file.getName(), zipContentPack);
                ModularWarfare.LOGGER.info("Registered content pack");
            } catch (ZipException e) {
                e.printStackTrace();
            }
        }
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

        for (TextureType type : textureTypes.values()) {
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
    }

    /**
     * Gets all the render config json for each gun
     */
    public static <T> T getRenderConfig(BaseType baseType, Class<T> typeClass) {
        if (baseType.isInDirectory) {
            try {
                File contentPackDir = new File(ModularWarfare.MOD_DIR, baseType.contentPack);
                if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                    File renderConfig = new File(contentPackDir, "/" + baseType.getAssetDir() + "/render");
                    File typeRender = new File(renderConfig, baseType.internalName + ".render.json");
                    JsonReader jsonReader = new JsonReader(new FileReader(typeRender));
                    return GSONUtils.fromJson(gson, jsonReader, typeClass, baseType.internalName + ".render.json");
                }
            } catch (JsonParseException | FileNotFoundException e) {
                e.printStackTrace();
            } catch (AnimationTypeException err) {
                ModularWarfare.LOGGER.info(baseType.internalName + " was loaded. But something was wrong.");
                err.printStackTrace();
            }
        } else {
            if (zipContentsPack.containsKey(baseType.contentPack)) {
                String typeName = baseType.getAssetDir();

                FileHeader foundFile = zipContentsPack.get(baseType.contentPack).fileHeaders.stream().filter(fileHeader -> fileHeader.getFileName().startsWith(typeName + "/" + "render/") && fileHeader.getFileName().replace(typeName + "/render/", "").equalsIgnoreCase(baseType.internalName + ".render.json")).findFirst().orElse(null);
                if (foundFile != null) {
                    try {
                        ZipInputStream stream = zipContentsPack.get(baseType.contentPack).getZipFile().getInputStream(foundFile);
                        JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));
                        return GSONUtils.fromJson(gson, jsonReader, typeClass, baseType.internalName + ".render.json");
                    } catch (JsonParseException | IOException e) {
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
     *
     * @param contentPacks - The content packs to get the type files from
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
                        File[] subFiles = subFolder.listFiles();
                        if (subFiles != null) {
                            for (File typeFile : subFiles) {
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
                }
                /*
                  LOAD SCRIPT START
                  */
                File scriptFolder = new File(file, "/script/");
                if (!scriptFolder.exists()) {
                    continue;
                }

                File[] scripts = scriptFolder.listFiles();
                if (scripts == null) {
                    continue;
                }

                Arrays.stream(scripts)
                        .filter(script -> script.getName().endsWith(".js"))
                        .forEach(script -> {
                            StringBuilder text = new StringBuilder();
                            try {
                                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Files.newInputStream(script.toPath()), StandardCharsets.UTF_8));
                                String temp;
                                while ((temp = bufferedReader.readLine()) != null) {
                                    text.append(temp);
                                }
                                bufferedReader.close();
                                ScriptHost.INSTANCE.initScript(new ResourceLocation(ModularWarfare.MOD_ID, "script/" + script.getName()), text.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                /*
                  LOAD SCRIPT END
                  */
            } else {
                if (!zipContentsPack.containsKey(file.getName())) {
                    continue;
                }

                for (FileHeader fileHeader : zipContentsPack.get(file.getName()).fileHeaders) {
                    for (TypeEntry type : ContentTypes.values) {
                        final String zipName = fileHeader.getFileName();
                        final String typeName = type.toString();
                        if (zipName.startsWith(typeName + "/") && zipName.split(typeName + "/").length > 1 && zipName.split(typeName + "/")[1].length() > 0 && !zipName.contains("render")) {
                            ZipInputStream stream;
                            try {
                                stream = zipContentsPack.get(file.getName()).getZipFile().getInputStream(fileHeader);
                                JsonReader jsonReader = new JsonReader(new InputStreamReader(stream));

                                try {
                                    BaseType parsedType = GSONUtils.fromJson(gson, jsonReader, type.typeClass, fileHeader.getFileName());
                                    parsedType.id = type.id;
                                    parsedType.contentPack = file.getName();
                                    parsedType.isInDirectory = false;
                                    baseTypes.add(parsedType);

                                    if (parsedType instanceof TextureType) {
                                        textureTypes.put(parsedType.internalName, (TextureType) parsedType);
                                    }
                                } catch (com.google.gson.JsonParseException ignored) {
                                }
                            } catch (ZipException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    /*
                      LOAD SCRIPT START
                      */
                    String zipName = fileHeader.getFileName();
                    if (zipName.startsWith("script/") && zipName.endsWith(".js")) {
                        String typeFile = zipName.replaceFirst("script/", "").replace(".js", "");
                        StringBuilder text = new StringBuilder();
                        try {
                            ZipInputStream inputStream = zipContentsPack.get(file.getName()).getZipFile().getInputStream(fileHeader);
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                            String temp;
                            while ((temp = bufferedReader.readLine()) != null) {
                                text.append(temp);
                            }
                            bufferedReader.close();
                            ScriptHost.INSTANCE.initScript(new ResourceLocation(ModularWarfare.MOD_ID, "script/" + typeFile + ".js"), text.toString());
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    /*
                      LOAD SCRIPT END
                      */
                }
            }
        }
    }

    public static void loadConfig() {
        new ModConfig(new File(MOD_DIR, "mod_config.json"));
    }

    public static void registerRayCasting(RayCasting rayCasting) {
        INSTANCE.RAY_CASTING = rayCasting;
    }

    /**
     * Registers items, blocks, renders, etc
     *
     * @param event - The event
     */
    @EventHandler
    public void onPreInitialization(FMLPreInitializationEvent event) {
        PROXY.preload();

        if (FMLCommonHandler.instance().getSide().isServer()) {
            // Creates directory if doesn't exist
            MOD_DIR = new File(event.getModConfigurationDirectory().getParentFile(), "ModularWarfare");
            if (!MOD_DIR.exists()) {
                MOD_DIR.mkdir();
                LOGGER.info("Created ModularWarfare folder, it's recommended to install content packs.");
                LOGGER.info("As the mod itself doesn't come with any content.");
            }
            loadConfig();
            DEV_ENV = true;

            contentPacks = PROXY.getContentList();
        }

        registerRayCasting(new DefaultRayCasting());
        loaderManager.preInitAddons(event);

        // Loads Content Packs
        ContentTypes.registerTypes();
        loadContentPacks(false);

        // Client side loading
        //PROXY.forceReload();

        PROXY.registerEventHandlers();

        MinecraftForge.EVENT_BUS.register(new CommonEventHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * Register events, imc, and world stuff
     *
     * @param event - The event
     */
    @EventHandler
    public void onInitialization(FMLInitializationEvent event) {
        new ServerTickHandler();

        PROXY.load();

        NETWORK = new NetworkHandler();
        NETWORK.initialise();
        NetworkRegistry.INSTANCE.registerGuiHandler(ModularWarfare.INSTANCE, new GuiHandler());
        loaderManager.initAddons(event);
    }

    /**
     * Last loading things
     *
     * @param event - The event
     */
    @EventHandler
    public void onPostInitialization(FMLPostInitializationEvent event) {
        NETWORK.postInitialise();
        PROXY.init();
        loaderManager.postInitAddons(event);
    }

    /**
     * Registers commands and server sided regions
     *
     * @param event - The event
     */
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandClear());
        event.registerServerCommand(new CommandNBT());
        event.registerServerCommand(new CommandDebug());
        event.registerServerCommand(new CommandKit());
    }

    /**
     * Registers protected content-pack before preInit, to allow making a custom ResourcePackLoader allowing protected .zip
     *
     * @param event - The event
     */
    @Mod.EventHandler
    public void constructionEvent(FMLConstructionEvent event) {
        LOGGER = LogManager.getLogger(ModularWarfare.MOD_ID);

        /**
         * Loading ModularProtector
         * Because of security reasons ModularProtectorOfficial class is not shown in the GitHub repository
         * however ModularProtectorOfficial is in the official releases on Curseforge and obfuscated
         * <p>
         * In order to still be able to build the repository from source, we give you a template called ModularProtectorTemplate
         * that allow you to implement your own content-pack protector.
         * Note: You will not be able to use existing official encrypted content-pack using the GitHub builds.
         */
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            Class<?> protectorClass = null;
            try {
                try {
                    protectorClass = Class.forName("com.modularwarfare.common.protector.ModularProtectorOfficial");
                    PROTECTOR = (ModularProtector) protectorClass.newInstance();
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } catch (RuntimeException exception) {
                exception.printStackTrace();
            }
            if (protectorClass == null) {
                PROTECTOR = new ModularProtectorOfficial();
            }
            LOGGER.info("Registered ModularProtector :" + PROTECTOR.getClass().toString());
        }
        /**
         * Create & Check Addon System
         */

        addonDir = new File(ModUtil.getGameFolder() + "/addons_mwf_shining");

        if (!addonDir.exists())
            addonDir.mkdirs();
        loaderManager = new AddonLoaderManager();
        loaderManager.constructAddons(addonDir, event.getSide());

        /**
         * Load the addon from the gradle project compilation (.class folder) instead of final .jar
         * in order to allow HotSwap changes
         */
        if (ModUtil.isIDE()) {
            File file = new File(ModUtil.getGameFolder()).getParentFile().getParentFile();
            String folder = file.toString().replace("\\", "/");
            loaderManager.constructDevAddons(new File(folder + "/melee-addon/build/classes/java/main"), "com.modularwarfare.melee.ModularWarfareMelee", event.getSide());
        }

        PROXY.construction(event);
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        for (File file : contentPacks) {
            List<Item> tabOrder = new ArrayList<>();
            gunTypes.values().stream()
                    .filter(itemGun -> itemGun.type.contentPack.equals(file.getName()))
                    .forEach(itemGun -> {
                        event.getRegistry().register(itemGun);
                        tabOrder.add(itemGun);
                    });

            ammoTypes.values().stream()
                    .filter(itemAmmo -> itemAmmo.type.contentPack.equals(file.getName()))
                    .forEach(itemAmmo -> {
                        event.getRegistry().register(itemAmmo);
                        tabOrder.add(itemAmmo);
                    });

            bulletTypes.values().stream()
                    .filter(itemBullet -> itemBullet.type.contentPack.equals(file.getName()))
                    .forEach(itemBullet -> {
                        event.getRegistry().register(itemBullet);
                        tabOrder.add(itemBullet);
                    });

            armorTypes.values().stream()
                    .filter(itemArmor -> itemArmor.type.contentPack.equals(file.getName()))
                    .forEach(itemArmor -> {
                        event.getRegistry().register(itemArmor);
                        tabOrder.add(itemArmor);
                    });

            attachmentTypes.values().stream()
                    .filter(itemAttachment -> itemAttachment.type.contentPack.equals(file.getName()))
                    .forEach(itemAttachment -> {
                        event.getRegistry().register(itemAttachment);
                        tabOrder.add(itemAttachment);
                    });

            specialArmorTypes.values().stream()
                    .filter(itemSpecialArmor -> itemSpecialArmor.type.contentPack.equals(file.getName()))
                    .forEach(itemSpecialArmor -> {
                        event.getRegistry().register(itemSpecialArmor);
                        tabOrder.add(itemSpecialArmor);
                    });

            sprayTypes.values().stream()
                    .filter(itemSpray -> itemSpray.type.contentPack.equals(file.getName()))
                    .forEach(itemSpray -> {
                        event.getRegistry().register(itemSpray);
                        tabOrder.add(itemSpray);
                    });

            backpackTypes.values().stream()
                    .filter(itemBackpack -> itemBackpack.type.contentPack.equals(file.getName()))
                    .forEach(itemBackpack -> {
                        event.getRegistry().register(itemBackpack);
                        tabOrder.add(itemBackpack);
                    });

            grenadeTypes.values().stream()
                    .filter(itemGrenade -> itemGrenade.type.contentPack.equals(file.getName()))
                    .forEach(itemGrenade -> {
                        event.getRegistry().register(itemGrenade);
                        tabOrder.add(itemGrenade);
                    });

            ItemRegisterEvent itemRegisterEvent = new ItemRegisterEvent(event.getRegistry(), tabOrder);
            MinecraftForge.EVENT_BUS.post(itemRegisterEvent);

            itemRegisterEvent.tabOrder.forEach((item) -> {
                if (item instanceof ItemGun) {
                    for (SkinType skin : ((ItemGun) item).type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, ((ItemGun) item).type);
                    }
                }

                if (item instanceof ItemBullet) {
                    for (SkinType skin : ((ItemBullet) item).type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, ((ItemBullet) item).type);
                    }
                }

                if (item instanceof ItemMWArmor) {
                    for (SkinType skin : ((ItemMWArmor) item).type.modelSkins) {
                        CommonProxy.preloadSkinTypes.put(skin, ((ItemMWArmor) item).type);
                    }
                }

            });

            MODS_TABS.get(file.getName()).preInitialize(tabOrder);
        }

        event.getRegistry().register(new ItemLight("light"));
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "bullethole"), EntityBulletHole.class, "bullethole", 3, this, 80, 10, false);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "shell"), EntityShell.class, "shell", 4, this, 64, 1, false);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "itemloot"), EntityItemLoot.class, "itemloot", 6, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "grenade"), EntityGrenade.class, "grenade", 7, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "smoke_grenade"), EntitySmokeGrenade.class, "smoke_grenade", 8, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "stun_grenade"), EntityStunGrenade.class, "stun_grenade", 9, this, 64, 1, true);

        //EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "bullet"), EntityBullet.class, "bullet", 15, this, 64, 1, true);
        EntityRegistry.registerModEntity(new ResourceLocation(ModularWarfare.MOD_ID, "explosive_projectile"), EntityExplosiveProjectile.class, "explosive_projectile", 15, this, 80, 1, true);
    }

}

