package com.modularwarfare.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GenerateJsonModelsEvent;
import com.modularwarfare.api.WeaponAnimations;
import com.modularwarfare.client.commands.CommandMWClient;
import com.modularwarfare.client.export.ItemModelExport;
import com.modularwarfare.client.fpp.basic.animations.ReloadType;
import com.modularwarfare.client.fpp.basic.animations.anims.*;
import com.modularwarfare.client.fpp.basic.configs.GunRenderConfig;
import com.modularwarfare.client.fpp.basic.renderers.*;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.client.handler.*;
import com.modularwarfare.client.hud.AttachmentUI;
import com.modularwarfare.client.hud.FlashSystem;
import com.modularwarfare.client.hud.GunUI;
import com.modularwarfare.client.killchat.KillFeedManager;
import com.modularwarfare.client.killchat.KillFeedRender;
import com.modularwarfare.client.model.ModelGun;
import com.modularwarfare.client.model.layers.RenderLayerBackpack;
import com.modularwarfare.client.model.layers.RenderLayerBody;
import com.modularwarfare.client.model.layers.RenderLayerHeldGun;
import com.modularwarfare.client.model.layers.ResetHiddenModelLayer;
import com.modularwarfare.client.patch.customnpc.CustomNPCListener;
import com.modularwarfare.client.patch.galacticraft.GCCompatInterop;
import com.modularwarfare.client.patch.galacticraft.GCDummyInterop;
import com.modularwarfare.client.patch.obfuscate.ObfuscateCompatInterop;
import com.modularwarfare.client.renderers.RenderItemLoot;
import com.modularwarfare.client.renderers.RenderProjectile;
import com.modularwarfare.client.renderers.RenderShell;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.client.shader.Programs;
import com.modularwarfare.common.CommonProxy;
import com.modularwarfare.common.armor.ArmorType;
import com.modularwarfare.common.armor.ArmorType.ArmorInfo;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.armor.ItemSpecialArmor;
import com.modularwarfare.common.backpacks.ItemBackpack;
import com.modularwarfare.common.entity.EntityBulletClient;
import com.modularwarfare.common.entity.EntityExplosiveProjectile;
import com.modularwarfare.common.entity.decals.EntityBulletHole;
import com.modularwarfare.common.entity.decals.EntityShell;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.entity.grenades.EntitySmokeGrenade;
import com.modularwarfare.common.entity.grenades.EntityStunGrenade;
import com.modularwarfare.common.entity.item.EntityItemLoot;
import com.modularwarfare.common.extra.ItemLight;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.init.ModSounds;
import com.modularwarfare.common.particle.EntityBloodFX;
import com.modularwarfare.common.particle.ParticleExplosion;
import com.modularwarfare.common.particle.ParticleRocket;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.objects.SoundEntry;
import com.modularwarfare.raycast.obb.OBBPlayerManager;
import com.modularwarfare.utility.MWResourcePack;
import com.modularwarfare.utility.MWSound;
import com.modularwarfare.utility.ModUtil;
import mchhui.modularmovements.tactical.client.ClientLitener;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.MWFRenderHelper;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.MetadataCollection;
import net.minecraftforge.fml.common.discovery.ContainerType;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.IForgeRegistry;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.modularwarfare.ModularWarfare.contentPacks;

public class ClientProxy extends CommonProxy {

    public static String modelDir = "com.modularwarfare.client.model.";

    /**
     * Renders
     */
    public static RenderGunStatic gunStaticRenderer;
    public static RenderGunEnhanced gunEnhancedRenderer;

    public static RenderAmmo ammoRenderer;
    public static RenderAttachment attachmentRenderer;
    public static RenderGrenade grenadeRenderer;

    public static HashMap<String, SoundEvent> modSounds = new HashMap<String, SoundEvent>();

    public static ScopeUtils scopeUtils;
    public static FlashSystem flashImage;

    public static ItemLight itemLight = new ItemLight("light");

    public static ClientRenderHooks renderHooks;

    public static AttachmentUI attachmentUI;
    public static GunUI gunUI;

    public static KillFeedManager killFeedManager;
    /**
     * Patches
     **/
    public static GCCompatInterop galacticraftInterop;
    public static ObfuscateCompatInterop obfuscateInterop;

    private static int lastBobbingParm = 1;

    public KillFeedManager getKillChatManager() {
        return killFeedManager;
    }

    @Override
    public void construction(FMLConstructionEvent event) {
        super.construction(event);

        File[] contentPackFiles = modularWarfareDir.listFiles(file -> !file.getName().contains("cache")
                && !file.getName().contains("officialmw")
                && !file.getName().contains("highres"));

        if (contentPackFiles == null) {
            return;
        }

        for (File file : contentPackFiles) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("modid", ModularWarfare.MOD_ID);
            map.put("name", ModularWarfare.MOD_NAME + " : " + file.getName());
            map.put("version", "1");

            FMLModContainer container = null;
            if (zipJar.matcher(file.getName()).matches()) {
                ZipFile zipFile = new ZipFile(file);

                try {
                    if (zipFile.isEncrypted()) {
                        /* Check if the zipFile is encrypted by a password or not */
                        ModularWarfare.PROTECTOR.decryptAlternateFile(zipFile, file.getName());

                        container = new MWResourcePack.Container("com.modularwarfare.ModularWarfare", new ModCandidate(file, file, ContainerType.JAR), map, zipFile, ModularWarfare.MOD_NAME + " : " + file.getName());
                    } else {
                        container = new FMLModContainer("com.modularwarfare.ModularWarfare", new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), map);
                    }
                } catch (ZipException e) {
                    throw new RuntimeException(e);
                }

                container.bindMetadata(MetadataCollection.from(null, ""));
                FMLClientHandler.instance().addModAsResource(container);
                contentPacks.add(file);
            } else if (file.isDirectory()) {
                container = new FMLModContainer("com.modularwarfare.ModularWarfare", new ModCandidate(file, file, file.isDirectory() ? ContainerType.DIR : ContainerType.JAR), map);
                container.bindMetadata(MetadataCollection.from(null, ""));
                FMLClientHandler.instance().addModAsResource(container);

                contentPacks.add(file);
            }
        }
    }


    @Override
    public void preload() {
        //Smooth Swing Ticker Runnable
        SmoothSwingTicker smoothSwingTicker = new SmoothSwingTicker();
        Thread smoothTickThread = new Thread(smoothSwingTicker, "SmoothSwingThread");
        smoothTickThread.start();

        MinecraftForge.EVENT_BUS.register(this);
        startPatches();
        Minecraft.getMinecraft().gameSettings.useVbo = false;
    }

    public void startPatches() {
        if (Loader.isModLoaded("customnpcs")) {
            CustomNPCListener customNPCListener = new CustomNPCListener();
            MinecraftForge.EVENT_BUS.register(customNPCListener);
        }

        if (Loader.isModLoaded("galacticraftcore")) {
            try {
                ClientProxy.galacticraftInterop = Class.forName("com.modularwarfare.client.patch.galacticraft.GCInteropImpl").asSubclass(GCCompatInterop.class).newInstance();
                ModularWarfare.LOGGER.info("Galatic Craft has been detected! Will attempt to patch.");
                ClientProxy.galacticraftInterop.applyFix();
            } catch (Exception e) {
                e.printStackTrace();
                ClientProxy.galacticraftInterop = new GCDummyInterop();
            }
        } else {
            ClientProxy.galacticraftInterop = new GCDummyInterop();
        }
    }

    @Override
    public void load() {

        new KeyInputHandler();
        new ClientTickHandler();
        new ClientGunHandler();
        new RenderGuiHandler();

        renderHooks = new ClientRenderHooks();
        MinecraftForge.EVENT_BUS.register(renderHooks);

        scopeUtils = new ScopeUtils();
        MinecraftForge.EVENT_BUS.register(scopeUtils);

        flashImage = new FlashSystem();
        MinecraftForge.EVENT_BUS.register(flashImage);

        attachmentUI = new AttachmentUI();
        MinecraftForge.EVENT_BUS.register(attachmentUI);

        gunUI = new GunUI();
        MinecraftForge.EVENT_BUS.register(gunUI);

        killFeedManager = new KillFeedManager();
        MinecraftForge.EVENT_BUS.register(new KillFeedRender(killFeedManager));

        WeaponAnimations.registerAnimation("rifle", new AnimationRifle());
        WeaponAnimations.registerAnimation("rifle2", new AnimationRifle2());
        WeaponAnimations.registerAnimation("rifle3", new AnimationRifle3());
        WeaponAnimations.registerAnimation("rifle4", new AnimationRifle4());
        WeaponAnimations.registerAnimation("pistol", new AnimationPistol());
        WeaponAnimations.registerAnimation("revolver", new AnimationRevolver());
        WeaponAnimations.registerAnimation("shotgun", new AnimationShotgun());
        WeaponAnimations.registerAnimation("sniper", new AnimationSniperBottom());
        WeaponAnimations.registerAnimation("sniper_top", new AnimationSniperTop());
        WeaponAnimations.registerAnimation("sideclip", new AnimationSideClip());
        WeaponAnimations.registerAnimation("toprifle", new AnimationTopRifle());
        WeaponAnimations.registerAnimation("rocket_launcher", new AnimationRocketLauncher());

        final Map<String, RenderPlayer> skinMap = Minecraft.getMinecraft().getRenderManager().getSkinMap();
        for (final RenderPlayer renderer : skinMap.values()) {
            setupLayers(renderer);
        }
    }

    public void setupLayers(RenderPlayer renderer) {
        MWFRenderHelper helper = new MWFRenderHelper(renderer);
        helper.getLayerRenderers().add(0, new ResetHiddenModelLayer(renderer));
        renderer.addLayer(new RenderLayerBackpack(renderer, renderer.getMainModel().bipedBodyWear));
        renderer.addLayer(new RenderLayerBody(renderer, renderer.getMainModel().bipedBodyWear));
        // Disabled for animation third person test
        // renderer.addLayer(new RenderLayerHeldGun(renderer));
        renderer.addLayer(new RenderLayerHeldGun(renderer));
    }

    @Override
    public void init() {
        //Disable VAO on Mac computer (not compatibility)
        if (ModUtil.isMac()) {
            ModConfig.INSTANCE.model_optimization = false;
        }

        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener((ISelectiveResourceReloadListener) (resourceManager, resourcePredicate) -> loadTextures());
        loadTextures();

        ClientCommandHandler.instance.registerCommand(new CommandMWClient());

        Programs.init();
    }

    public void loadTextures() {
        ModularWarfare.LOGGER.info("Preloading textures");
        long time = System.currentTimeMillis();
        preloadSkinTypes.forEach((skin, type) -> {

            for (int i = 0; i < skin.textures.length; i++) {
                ResourceLocation resource = new ResourceLocation(ModularWarfare.MOD_ID,
                        String.format(skin.textures[i].format, type.getAssetDir(), skin.getSkin()));
                Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
                if (skin.sampling.equals(SkinType.Sampling.LINEAR)) {
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                    GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                }
            }
        });
        ModularWarfare.LOGGER.info("All textures are ready(" + (System.currentTimeMillis() - time) + "ms)");
    }

    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {

        for (ItemGun itemGun : ModularWarfare.gunTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemGun, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemGun.type.internalName));
        }

        for (ItemAmmo itemAmmo : ModularWarfare.ammoTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemAmmo, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemAmmo.type.internalName));
        }

        for (ItemAttachment itemAttachment : ModularWarfare.attachmentTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemAttachment, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemAttachment.type.internalName));
        }

        for (ItemBullet itemBullet : ModularWarfare.bulletTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemBullet, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemBullet.type.internalName));
        }

        for (ItemMWArmor itemArmor : ModularWarfare.armorTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemArmor, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemArmor.internalName));
        }

        for (ItemSpecialArmor itemArmor : ModularWarfare.specialArmorTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemArmor, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemArmor.type.internalName));
        }

        for (ItemSpray itemSpray : ModularWarfare.sprayTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemSpray, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemSpray.type.internalName));
        }

        for (ItemBackpack itemBackpack : ModularWarfare.backpackTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemBackpack, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemBackpack.type.internalName));
        }

        for (ItemGrenade itemGrenade : ModularWarfare.grenadeTypes.values()) {
            ModelLoader.setCustomModelResourceLocation(itemGrenade, 0, new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemGrenade.type.internalName));
        }

        ModelLoader.setCustomModelResourceLocation(itemLight, 0, new ModelResourceLocation(itemLight.getRegistryName(), "inventory"));

    }

    @Override
    public void forceReload() {
        FMLClientHandler.instance().refreshResources();
    }

    /**
     * Helper method that sorts out packages with staticModel name input
     * For example, the staticModel class "com.modularwarfare.client.staticModel.mw.ModelMP5"
     * is referenced in the type file by the string "mw.MP5"
     */
    private String getModelName(String in) {
        //Split about dots
        String[] split = in.split("\\.");
        //If there is no dot, our staticModel class is in the default staticModel package
        if (split.length == 1)
            return in;
            //Otherwise, we need to slightly rearrange the wording of the string for it to make sense
        else if (split.length > 1) {
            String out = split[split.length - 1];
            for (int i = split.length - 2; i >= 0; i--) {
                out = split[i] + "." + out;
            }
            return out;
        }
        return in;
    }

    /**
     * Generic staticModel loader method for getting staticModel classes and casting them to the required class type
     */
    @Override
    public <T> T loadModel(String s, String shortName, Class<T> typeClass) {
        if (s == null || shortName == null)
            return null;
        try {
            return typeClass.cast(Class.forName(modelDir + getModelName(s)).getConstructor().newInstance());
        } catch (Exception e) {
            ModularWarfare.LOGGER.error("Failed to load staticModel : " + shortName + " (" + s + ")");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void reloadModels(boolean reloadSkins) {
        for (BaseType baseType : ModularWarfare.baseTypes) {
            if (baseType.hasModel()) {
                baseType.reloadModel();
            }
        }
        if (reloadSkins)
            forceReload();
    }

    @Override
    public void generateJsonModels(ArrayList<BaseType> types) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        GenerateJsonModelsEvent event = new GenerateJsonModelsEvent();
        MinecraftForge.EVENT_BUS.post(event);

        for (BaseType type : types) {
            if (type.contentPack == null)
                continue;

            File contentPackDir = new File(ModularWarfare.MOD_DIR, type.contentPack);

            if (zipJar.matcher(contentPackDir.getName()).matches())
                continue;

            if (contentPackDir.exists() && contentPackDir.isDirectory()) {

                File itemModelsDir = new File(contentPackDir, "/assets/modularwarfare/models/item");
                if (!itemModelsDir.exists())
                    itemModelsDir.mkdirs();

                File typeModel = new File(itemModelsDir, type.internalName + ".json");
                if (!typeModel.exists()) {
                    if (type instanceof ArmorType) {
                        ArmorType armorType = (ArmorType) type;
                        for (ArmorInfo armorInfo : armorType.armorTypes.values()) {
                            String internalName = armorInfo.internalName != null ? armorInfo.internalName : armorType.internalName;
                            typeModel = new File(itemModelsDir, internalName + ".json");
                            try {
                                FileWriter fileWriter = new FileWriter(typeModel, false);
                                gson.toJson(createJson(type, internalName), fileWriter);
                                fileWriter.flush();
                                fileWriter.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        try {
                            FileWriter fileWriter = new FileWriter(typeModel, false);
                            gson.toJson(createJson(type), fileWriter);
                            fileWriter.flush();
                            fileWriter.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            /**
             * Create directories & files for .render.json if they don't exist
             */
            if (ModularWarfare.DEV_ENV) {
                final File dir = new File(contentPackDir, "/" + type.getAssetDir() + "/render");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                final File renderFile = new File(dir, type.internalName + ".render.json");
                if (!renderFile.exists()) {
                    try {
                        FileWriter fileWriter = new FileWriter(renderFile, true);
                        if (type instanceof GunType) {
                            if (((GunType) type).animationType.equals(WeaponAnimationType.ENHANCED)) {
                                GunEnhancedRenderConfig renderConfig = new GunEnhancedRenderConfig();
                                renderConfig.modelFileName = type.internalName.replaceAll(type.contentPack + ".", "");
                                renderConfig.modelFileName = renderConfig.modelFileName + ".glb";
                                gson.toJson(renderConfig, fileWriter);
                            } else {
                                GunRenderConfig renderConfig = new GunRenderConfig();
                                renderConfig.modelFileName = type.internalName.replaceAll(type.contentPack + ".", "");
                                renderConfig.modelFileName = renderConfig.modelFileName + ".obj";
                                gson.toJson(renderConfig, fileWriter);
                            }
                            fileWriter.flush();
                            fileWriter.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void generateJsonSounds(Collection<BaseType> types, boolean replace) {
        HashMap<String, ArrayList<String>> cpSounds = new HashMap<>();

        for (BaseType baseType : types) {
            if (baseType.contentPack == null)
                continue;

            String contentPack = baseType.contentPack;

            if (!cpSounds.containsKey(contentPack))
                cpSounds.put(contentPack, new ArrayList<String>());

            for (WeaponSoundType weaponSoundType : baseType.weaponSoundMap.keySet()) {
                ArrayList<SoundEntry> soundEntries = baseType.weaponSoundMap.get(weaponSoundType);
                for (SoundEntry soundEntry : soundEntries) {
                    if (soundEntry.soundName != null && !cpSounds.get(contentPack).contains(soundEntry.soundName))
                        cpSounds.get(contentPack).add(soundEntry.soundName);

                    if (soundEntry.soundNameDistant != null && !cpSounds.get(contentPack).contains(soundEntry.soundNameDistant))
                        cpSounds.get(contentPack).add(soundEntry.soundNameDistant);
                }
            }
        }

        for (String contentPack : cpSounds.keySet()) {
            try {
                File contentPackDir = new File(ModularWarfare.MOD_DIR, contentPack);
                if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                    ArrayList<String> soundEntries = cpSounds.get(contentPack);
                    if (soundEntries != null && !soundEntries.isEmpty()) {
                        Path assetsDir = Paths.get(ModularWarfare.MOD_DIR.getAbsolutePath() + "/" + contentPack + "/assets/modularwarfare/");
                        if (!Files.exists(assetsDir))
                            Files.createDirectories(assetsDir);
                        Path soundsFile = Paths.get(assetsDir + "/sounds.json");

                        boolean soundsExists = Files.exists(soundsFile);
                        boolean shouldCreate = !soundsExists || replace;
                        if (shouldCreate) {
                            if (!soundsExists)
                                Files.createFile(soundsFile);

                            ArrayList<String> jsonEntries = new ArrayList<String>();
                            String format = "\"%s\":{\"category\": \"player\",\"subtitle\": \"MW Sound\",\"sounds\": [\"modularwarfare:%s\"]}";
                            jsonEntries.add("{");
                            for (int i = 0; i < soundEntries.size(); i++) {
                                if (i + 1 < soundEntries.size()) {
                                    // add comma
                                    jsonEntries.add(format.replaceAll("%s", soundEntries.get(i)) + ",");
                                } else {
                                    // no comma
                                    jsonEntries.add(format.replaceAll("%s", soundEntries.get(i)));
                                }
                            }
                            jsonEntries.add("}");
                            Files.write(soundsFile, jsonEntries, StandardCharsets.UTF_8);
                        }
                    }
                }
            } catch (Exception exception) {
                if (ModularWarfare.DEV_ENV) {
                    exception.printStackTrace();
                } else {
                    ModularWarfare.LOGGER.error(String.format("Failed to create sounds.json for content pack '%s'", contentPack));
                }
            }
        }
    }

    @Override
    public void generateLangFiles(ArrayList<BaseType> types, boolean replace) {
        HashMap<String, ArrayList<BaseType>> langEntryMap = new HashMap<>();

        for (BaseType baseType : types) {
            if (baseType.contentPack == null)
                continue;

            String contentPack = baseType.contentPack;

            if (!langEntryMap.containsKey(contentPack))
                langEntryMap.put(contentPack, new ArrayList<>());

            if (baseType.displayName != null && !langEntryMap.get(contentPack).contains(baseType))
                langEntryMap.get(contentPack).add(baseType);

            if (baseType instanceof ArmorType)
                langEntryMap.get(contentPack).add(baseType);
        }

        for (String contentPack : langEntryMap.keySet()) {
            try {
                File contentPackDir = new File(ModularWarfare.MOD_DIR, contentPack);
                if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                    ArrayList<BaseType> langEntries = langEntryMap.get(contentPack);
                    if (langEntries != null && !langEntries.isEmpty()) {
                        Path langDir = Paths.get(ModularWarfare.MOD_DIR.getAbsolutePath() + "/" + contentPack + "/assets/modularwarfare/lang/");
                        if (!Files.exists(langDir))
                            Files.createDirectories(langDir);
                        Path langPath = Paths.get(langDir + "/en_US.lang");

                        boolean soundsExists = Files.exists(langPath);
                        boolean shouldCreate = !soundsExists || replace;
                        if (shouldCreate) {
                            if (!soundsExists)
                                Files.createFile(langPath);

                            ArrayList<String> jsonEntries = new ArrayList<>();
                            String format = "item.%s.name=%s";
                            for (BaseType type : langEntries) {
                                if (type instanceof ArmorType) {
                                    ArmorType armorType = (ArmorType) type;
                                    for (ArmorInfo armorInfo : armorType.armorTypes.values()) {
                                        String internalName = armorInfo.internalName != null ? armorInfo.internalName : armorType.internalName;
                                        jsonEntries.add(String.format(format, internalName, armorInfo.displayName));
                                    }
                                } else {
                                    jsonEntries.add(String.format(format, type.internalName, type.displayName));
                                }
                            }
                            Files.write(langPath, jsonEntries, StandardCharsets.UTF_8);
                        }
                    }
                }
            } catch (Exception exception) {
                if (ModularWarfare.DEV_ENV) {
                    exception.printStackTrace();
                } else {
                    ModularWarfare.LOGGER.error(String.format("Failed to create sounds.json for content pack '%s'", contentPack));
                }
            }
        }
    }

    private ItemModelExport createJson(BaseType type) {
        ItemModelExport exportedModel = new ItemModelExport();

        float scale = !(type instanceof GunType) && !(type instanceof GrenadeType) ? 0.4f : 0.0f;
        setThirdPersonScale(exportedModel, scale);

        exportedModel.setBaseLayer(type.getAssetDir() + "/" + (type.iconName != null ? type.iconName : type.internalName));
        return exportedModel;
    }

    private ItemModelExport createJson(BaseType type, String iconName) {
        ItemModelExport exportedModel = new ItemModelExport();

        setThirdPersonScale(exportedModel, 0.4f);

        exportedModel.setBaseLayer(type.getAssetDir() + "/" + iconName);
        return exportedModel;
    }

    private void setThirdPersonScale(ItemModelExport exportedModel, float scale) {
        exportedModel.display.thirdperson_lefthand.scale[0] = scale;
        exportedModel.display.thirdperson_lefthand.scale[1] = scale;
        exportedModel.display.thirdperson_lefthand.scale[2] = scale;

        exportedModel.display.thirdperson_righthand.scale[0] = scale;
        exportedModel.display.thirdperson_righthand.scale[1] = scale;
        exportedModel.display.thirdperson_righthand.scale[2] = scale;
    }

    @Override
    public void playSound(MWSound sound) {
        SoundEvent soundEvent = modSounds.get(sound.soundName);
        if (soundEvent == null) {
            ModularWarfare.LOGGER.error(String.format("The sound named '%s' does not exist. Skipping playSound", sound.soundName));
            return;
        }
        //System.out.println(sound.soundName);
        Minecraft.getMinecraft().world.playSound(Minecraft.getMinecraft().player, sound.blockPos, soundEvent, SoundCategory.PLAYERS, sound.volume, sound.pitch);
    }

    @Override
    public void registerSound(String soundName) {
        ResourceLocation resourceLocation = new ResourceLocation(ModularWarfare.MOD_ID, soundName);
        modSounds.put(soundName, new SoundEvent(resourceLocation).setRegistryName(resourceLocation));
    }

    @SubscribeEvent
    public void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        IForgeRegistry<SoundEvent> registry = event.getRegistry();
        for (WeaponSoundType weaponSoundType : WeaponSoundType.values()) {
            if (weaponSoundType.defaultSound != null) {
                registerSound(weaponSoundType.defaultSound);
                //registry.register(modSounds.get(weaponSoundType.defaultSound));
            }
        }

        for (SoundEvent soundEvent : modSounds.values()) {
            if (!registry.containsKey(soundEvent.getRegistryName())) {
                registry.register(soundEvent);
            }
        }
    }

    @SubscribeEvent
    public void registerEntities(RegistryEvent.Register<EntityEntry> event) {

        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {

            //BULLET HOLE
            RenderingRegistry.registerEntityRenderingHandler(EntityBulletHole.class, RenderDecal.FACTORY);

            //RENDER SHELL EJECTION
            RenderingRegistry.registerEntityRenderingHandler(EntityShell.class, RenderShell.FACTORY);

            //RENDER GRENADES
            RenderingRegistry.registerEntityRenderingHandler(EntityGrenade.class, RenderGrenadeEntity.FACTORY);
            RenderingRegistry.registerEntityRenderingHandler(EntitySmokeGrenade.class, RenderGrenadeEntity.FACTORY);
            RenderingRegistry.registerEntityRenderingHandler(EntityStunGrenade.class, RenderGrenadeEntity.FACTORY);

            RenderingRegistry.registerEntityRenderingHandler(EntityItemLoot.class, RenderItemLoot.FACTORY);

            RenderingRegistry.registerEntityRenderingHandler(EntityBulletClient.class, RenderBullet.FACTORY);

            //RENDER PROJECTILES
            RenderingRegistry.registerEntityRenderingHandler(EntityExplosiveProjectile.class, RenderProjectile.FACTORY);
        }

    }

    @Override
    public void onShootAnimation(EntityPlayer player, String wepType, int fireTickDelay, float recoilPitch,
                                 float recoilYaw) {
        GunType gunType = ModularWarfare.gunTypes.get(wepType).type;
        if (gunType != null) {
            if (gunType.animationType == WeaponAnimationType.BASIC) {
                ClientRenderHooks.getAnimMachine(player).triggerShoot((ModelGun) gunType.model, gunType, fireTickDelay);
            } else {
                float rand = (float) Math.random();
                ClientEventHandler.cemeraBobbing = lastBobbingParm * (0.3f + 0.4f * Math.abs(rand));
                lastBobbingParm = -lastBobbingParm;
                AnimationController controller = gunEnhancedRenderer.getController(player, (GunEnhancedRenderConfig) gunType.enhancedModel.config);
                ClientRenderHooks.getEnhancedAnimMachine(player).triggerShoot(controller, (ModelEnhancedGun) gunType.enhancedModel,
                        gunType, fireTickDelay);
            }

            RenderParameters.rate = Math.min(RenderParameters.rate + 0.07f, 1f);

            float recoilPitchGripFactor = 1.0f;
            float recoilYawGripFactor = 1.0f;

            float recoilPitchBarrelFactor = 1.0f;
            float recoilYawBarrelFactor = 1.0f;

            float recoilPitchStockFactor = 1.0f;
            float recoilYawStockFactor = 1.0f;

            if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Grip) != null) {
                ItemAttachment gripAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Grip).getItem();
                recoilPitchGripFactor = gripAttachment.type.grip.recoilPitchFactor;
                recoilYawGripFactor = gripAttachment.type.grip.recoilYawFactor;
            }

            if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel) != null) {
                ItemAttachment barrelAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Barrel).getItem();
                recoilPitchBarrelFactor = barrelAttachment.type.barrel.recoilPitchFactor;
                recoilYawBarrelFactor = barrelAttachment.type.barrel.recoilYawFactor;
            }

            if (GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock) != null) {
                ItemAttachment stockAttachment = (ItemAttachment) GunType.getAttachment(player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND), AttachmentPresetEnum.Stock).getItem();
                recoilPitchStockFactor = stockAttachment.type.stock.recoilPitchFactor;
                recoilYawStockFactor = stockAttachment.type.stock.recoilYawFactor;
            }

            boolean isCrawling = false;
            if (ModularWarfare.isLoadedModularMovements) {
                if (ClientLitener.clientPlayerState.isCrawling) {
                    isCrawling = true;
                }
            }
            float offsetYaw = 0;
            float offsetPitch = 0;
            if (!(ClientRenderHooks.isAiming || ClientRenderHooks.isAimingScope)) {
                offsetPitch = gunType.recoilPitch;
                offsetPitch += ((gunType.randomRecoilPitch * 2) - gunType.randomRecoilPitch);
                offsetPitch *= (recoilPitchGripFactor * recoilPitchBarrelFactor * recoilPitchStockFactor);


                offsetYaw = gunType.recoilYaw;
                offsetYaw *= new Random().nextFloat() * (gunType.randomRecoilYaw * 2) - gunType.randomRecoilYaw;
                offsetYaw *= recoilYawGripFactor * recoilYawBarrelFactor * recoilYawStockFactor;
                offsetYaw *= RenderParameters.rate * (isCrawling ? 0.2f : 1.0f);
                offsetYaw *= RenderParameters.phase ? 1 : -1;
            } else {
                //offsetYaw *= RenderParameters.phase ? 1 : -1;
                offsetPitch = gunType.recoilPitch;
                offsetPitch += ((gunType.randomRecoilPitch * 2) - gunType.randomRecoilPitch);
                offsetPitch *= (recoilPitchGripFactor * recoilPitchBarrelFactor * recoilPitchStockFactor);
                offsetPitch *= gunType.recoilAimReducer;

                offsetYaw = gunType.recoilYaw;
                offsetYaw *= new Random().nextFloat() * (gunType.randomRecoilYaw * 2) - gunType.randomRecoilYaw;
                offsetYaw *= recoilYawGripFactor * recoilYawBarrelFactor * recoilYawStockFactor;
                offsetYaw *= RenderParameters.rate * (isCrawling ? 0.2f : 1.0f);
                offsetYaw *= gunType.recoilAimReducer;
                offsetYaw *= RenderParameters.phase ? 1 : -1;
            }
            if (ModularWarfare.isLoadedModularMovements) {
                if (ClientLitener.clientPlayerState.isCrawling) {
                    offsetPitch *= gunType.recoilCrawlPitchFactor;
                    offsetYaw *= gunType.recoilCrawlYawFactor;
                }
            }
            RenderParameters.playerRecoilPitch += offsetPitch;
            if (Math.random() > 0.5f) {
                RenderParameters.playerRecoilYaw += offsetYaw;
            } else {
                RenderParameters.playerRecoilYaw -= offsetYaw;
            }
            RenderParameters.phase = !RenderParameters.phase;
        }
    }

    @Override
    public void onReloadAnimation(EntityPlayer player, String wepType, int reloadTime, int reloadCount,
                                  int reloadType) {
        ClientTickHandler.playerReloadCooldown.put(player.getUniqueID(), reloadTime);
        ItemGun gunType = ModularWarfare.gunTypes.get(wepType);
        if (gunType != null) {
            if (gunType.type.animationType == WeaponAnimationType.BASIC) {
                ClientRenderHooks.getAnimMachine(player).triggerReload(reloadTime, reloadCount, (ModelGun) gunType.type.model, ReloadType.getTypeFromInt(reloadType), player.isSprinting());
            } else {
                AnimationController controller = gunEnhancedRenderer.getController(player, (GunEnhancedRenderConfig) gunType.type.enhancedModel.config);
                ClientRenderHooks.getEnhancedAnimMachine(player).triggerReload(controller, player, reloadTime, reloadCount, (ModelEnhancedGun) gunType.type.enhancedModel, ReloadType.getTypeFromInt(reloadType));
            }
        }
    }

    @Override
    public void onShootFailedAnimation(EntityPlayer player, String wepType) {
        ItemGun gunType = ModularWarfare.gunTypes.get(wepType);
        if (gunType != null) {
            if (gunType.type.animationType == WeaponAnimationType.ENHANCED) {
                AnimationController controller = gunEnhancedRenderer.getController(player, (GunEnhancedRenderConfig) gunType.type.enhancedModel.config);
                ClientRenderHooks.getEnhancedAnimMachine(player).triggerShoot(controller, (ModelEnhancedGun) gunType.type.enhancedModel, gunType.type, 0, true);
            }
        }
    }

    @Override
    public void onModeChangeAnimation(EntityPlayer player, String wepType) {
        ItemGun gunType = ModularWarfare.gunTypes.get(wepType);
        if (gunType != null) {
            if (gunType.type.animationType == WeaponAnimationType.ENHANCED) {
                if (gunEnhancedRenderer.controller != null) {
                    gunEnhancedRenderer.controller.MODE_CHANGE = 0;
                }
            }
        }
    }

    @Override
    public World getClientWorld() {
        return FMLClientHandler.instance().getClient().world;
    }

    @Override
    public void registerEventHandlers() {
        super.registerEventHandlers();
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        MinecraftForge.EVENT_BUS.register(new OBBPlayerManager());
    }


    @Override
    public void addBlood(final EntityLivingBase living, final int amount, final boolean onhit) {
        if (onhit) {
            this.addBlood(living, amount);
        }
    }

    @Override
    public void playHitmarker(boolean headshot) {
        if (ModConfig.INSTANCE.hud.hitmarkers) {
            Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.getRecord(ClientProxy.modSounds.get("hitmarker"), 1f, 4f));
            GunUI.addHitMarker(headshot);
        }
    }

    @Override
    public void addBlood(final EntityLivingBase living, final int amount) {
        for (int k = 0; k < amount; ++k) {
            float attenuator = 0.3f;
            double mX = -MathHelper.sin(living.rotationYaw / 180.0f * 3.1415927f) * MathHelper.cos(living.rotationPitch / 180.0f * 3.1415927f) * attenuator;
            double mZ = MathHelper.cos(living.rotationYaw / 180.0f * 3.1415927f) * MathHelper.cos(living.rotationPitch / 180.0f * 3.1415927f) * attenuator;
            double mY = -MathHelper.sin(living.rotationPitch / 180.0f * 3.1415927f) * attenuator + 0.1f;
            attenuator = 0.02f;
            final float var5 = living.getRNG().nextFloat() * 3.1415927f * 2.0f;
            attenuator *= living.getRNG().nextFloat();
            mX += Math.cos(var5) * attenuator;
            mY += (living.getRNG().nextFloat() - living.getRNG().nextFloat()) * 0.1f;
            mZ += Math.sin(var5) * attenuator;
            final Particle blood = new EntityBloodFX(living.getEntityWorld(), living.posX, living.posY + 0.5 + living.getRNG().nextDouble() * 0.7, living.posZ, living.motionX * 2.0 + mX, living.motionY + mY, living.motionZ * 2.0 + mZ, 0.0);
            Minecraft.getMinecraft().effectRenderer.addEffect(blood);
        }
    }

    @Override
    public void resetSens() {
        ClientRenderHooks.isAimingScope = false;
        ClientRenderHooks.isAiming = false;
    }

    @Override
    public void spawnExplosionParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            super.spawnExplosionParticle(world, x, y, z);
            return;
        }
        final Particle explosionParticle = new ParticleExplosion(world, x, y, z);
        Minecraft.getMinecraft().effectRenderer.addEffect(explosionParticle);
    }

    public void spawnRocketParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            super.spawnRocketParticle(world, x, y, z);
            return;
        }
        final Particle rocketParticle = new ParticleRocket(world, x, y, z);
        Minecraft.getMinecraft().effectRenderer.addEffect(rocketParticle);
    }

    @Override
    public void playFlashSound(EntityPlayer entityPlayer) {
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(ModSounds.FLASHED, SoundCategory.PLAYERS, (float) FlashSystem.flashValue / 1000, 1, (float) entityPlayer.posX, (float) entityPlayer.posY, (float) entityPlayer.posZ));
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(ModSounds.FLASHED, SoundCategory.PLAYERS, 5.0f, 0.2f, (float) entityPlayer.posX, (float) entityPlayer.posY, (float) entityPlayer.posZ));
        Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(ModSounds.FLASHED, SoundCategory.PLAYERS, 5.0f, 0.1f, (float) entityPlayer.posX, (float) entityPlayer.posY, (float) entityPlayer.posZ));
    }
}
