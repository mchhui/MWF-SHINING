package com.modularwarfare.common;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.SkinType;
import com.modularwarfare.common.network.PacketParticle;
import com.modularwarfare.common.network.PacketParticle.ParticleType;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.MWSound;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.FMLInjectionData;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class CommonProxy {

    public static final Pattern zipJar = Pattern.compile("(.+).(zip|jar)$");

    public static HashMap<SkinType,BaseType> preloadSkinTypes = new HashMap<>();
    public static HashSet<TextureType> preloadFlashTex = new HashSet<>();

    public void construction(FMLConstructionEvent event) {
        //Production-environment
        // Creates directory if it doesn't exist
        ModularWarfare.CONTENT_DIR = new File(getGameFolder(),"ModularWarfare");;
        if (!ModularWarfare.CONTENT_DIR.exists() && !ModularWarfare.CONTENT_DIR.mkdir()) {
            ModularWarfare.LOGGER.error("Failed to create content directory");
        }
        new ModConfig(new File(ModularWarfare.CONTENT_DIR, "mod_config.json"));

        ModularWarfare.DEV_ENV = ModConfig.INSTANCE.dev_mode;

//        /**
//         * Prototype pack extraction
//         */
//        boolean needPrototypeExtract = ModConfig.INSTANCE.general.prototype_pack_extraction;
//        for (File file : modularWarfareDir.listFiles()) {
//            if (file.getName().matches("prototype-" + MOD_VERSION + "-contentpack.zip")) {
//                needPrototypeExtract = false;
//            } else if (file.getName().contains("prototype") && !file.getName().contains(MOD_VERSION) && file.getName().contains(".zip") && !file.getName().endsWith(".bak")) {
//                file.renameTo(new File(file.getAbsolutePath() + ".bak"));
//            }
//        }
//        if (needPrototypeExtract) {
//            try {
//                ZipFile zipFile = new ZipFile(modFile);
//                zipFile.extractFile("prototype-" + MOD_VERSION + "-contentpack.zip", modularWarfareDir.getAbsolutePath());
//            } catch (ZipException e) {
//                e.printStackTrace();
//            }
//        }
//
//        /**
//         * Animated pack extraction
//         */
//        boolean needAnimatedExtract = ModConfig.INSTANCE.general.animated_pack_extraction;
//        for (File file : modularWarfareDir.listFiles()) {
//            if (file.getName().matches("animated-" + MOD_VERSION + "-contentpack.zip")) {
//                needAnimatedExtract = false;
//            } else if (file.getName().contains("animated") && !file.getName().contains(MOD_VERSION) && file.getName().contains(".zip") && !file.getName().endsWith(".bak")) {
//                file.renameTo(new File(file.getAbsolutePath() + ".bak"));
//            }
//        }
//        if (needAnimatedExtract) {
//            try {
//                ZipFile zipFile = new ZipFile(modFile);
//                zipFile.extractFile("animated-" + MOD_VERSION + "-contentpack.zip", modularWarfareDir.getAbsolutePath());
//            } catch (ZipException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void preload() {

    }

    public void load() {
        try {
            final Class<?> bridge = Class.forName("mchhui.hebridge.HEBridge");
            bridge.getDeclaredMethod("init").invoke(null);
        }
        catch (ClassNotFoundException e) {
            ModularWarfare.LOGGER.info("HEBridge not found, skipping initialization");
        }
        catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            ModularWarfare.LOGGER.error("Failed to initialize HEBridge", e);
        }
    }

    public void init() {
    }

    public void forceReload() {
    }

    @Nonnull
    public static String getGameFolder() {
        return ((File) (FMLInjectionData.data()[6])).getAbsolutePath();
    }

    public List<File> getContentList() {
        final File[] files = ModularWarfare.CONTENT_DIR.listFiles();
        final List<File> contentPacks = (
            Arrays.stream(Objects.requireNonNull(files))
            .filter(file -> {
                final String fname = file.getName();
                return (
                    !fname.contains("cache")
                    && !fname.contains("officialmw")
                    && !fname.contains("highres")
                );
            })
            .map(file -> {
                if (file.isDirectory()) {
                    return Optional.of(file);
                } else if (zipJar.matcher(file.getName()).matches()) {
                    try {
                        ZipFile zipFile = new ZipFile(file);
                        if (!zipFile.isEncrypted()) {
                            return Optional.of(file);
                        } else {
                            ModularWarfare.LOGGER.info("[WARNING] ModularWarfare can't load encrypted content-packs in server-side (" + file.getName() + ") !");
                        }
                    } catch (ZipException e) {
                        e.printStackTrace();
                    }
                }
                return Optional.<File>empty();
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList())
        );
        ModularWarfare.LOGGER.info("Loaded content pack list server side.");
        return contentPacks;
    }

    public <T> T loadModel(String s, String shortName, Class<T> typeClass) {
        return null;
    }

    public void spawnExplosionParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(new PacketParticle(ParticleType.EXPLOSION,x,y,z),new TargetPoint(world.provider.getDimension(), x, y, z, 64));
        }
    }
    
    public void spawnRocketParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(new PacketParticle(ParticleType.ROCKET,x,y,z),new TargetPoint(world.provider.getDimension(), x, y, z, 64));
        }
    }


    public void reloadModels(boolean reloadSkins) {
    }

    public void generateJsonModels(ArrayList<BaseType> types) {
    }

    public void generateJsonSounds(Collection<BaseType> types, boolean replace) {
    }

    public void generateLangFiles(ArrayList<BaseType> types, boolean replace) {
    }

    public void playSound(MWSound sound) {
    }

    public void playHitmarker(boolean headshot) {
    }

    public void registerSound(String soundName) {
    }

    public void onShootAnimation(EntityPlayer player, String wepType, int fireTickDelay, float recoilPitch, float recoilYaw) {
    }

    public void onReloadAnimation(EntityPlayer player, String wepType, int reloadTime, int reloadCount, int reloadType) {
    }

    public void onShootFailedAnimation(EntityPlayer player, String wepType) {
    }
    
    public void onModeChangeAnimation(EntityPlayer player, String wepType) {
    }

    public World getClientWorld() {
        return null;
    }

    public void addBlood(final EntityLivingBase living, final int amount) {
    }

    public void addBlood(final EntityLivingBase living, final int amount, final boolean onhit) {
    }

    public void registerEventHandlers() {
    }

    public void resetSens() {
    }

    public void playFlashSound(EntityPlayer player) {
    }
}
