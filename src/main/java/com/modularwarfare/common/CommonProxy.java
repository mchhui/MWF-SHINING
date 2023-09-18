package com.modularwarfare.common;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.SkinType;
import com.modularwarfare.common.network.PacketParticle;
import com.modularwarfare.common.network.PacketParticle.ParticleType;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.MWSound;
import com.modularwarfare.utility.event.ForgeEvent;
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
import java.util.*;
import java.util.regex.Pattern;


public class CommonProxy extends ForgeEvent {

    public static Pattern zipJar = Pattern.compile("(.+).(zip|jar)$");

    public static File modularWarfareDir;

    public static HashMap<SkinType, BaseType> preloadSkinTypes = new HashMap<SkinType, BaseType>();
    public static HashSet<TextureType> preloadFlashTex = new HashSet<TextureType>();

    @Nonnull
    public static String getGameFolder() {
        return ((File) (FMLInjectionData.data()[6])).getAbsolutePath();
    }

    public void construction(FMLConstructionEvent event) {
        //Production-environment
        this.modularWarfareDir = new File(getGameFolder(), "ModularWarfare");
        File modFile = null;

        // Creates directory if doesn't exist
        ModularWarfare.MOD_DIR = modularWarfareDir;
        if (!ModularWarfare.MOD_DIR.exists()) {
            ModularWarfare.MOD_DIR.mkdir();
        }
        new ModConfig(new File(ModularWarfare.MOD_DIR, "mod_config.json"));

        ModularWarfare.DEV_ENV = ModConfig.INSTANCE.dev_mode;

        for (File source : new File(modularWarfareDir.getParentFile(), "mods").listFiles()) {
            if (source.getName().contains("modularwarfare")) {
                modFile = source;
            }
        }

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

    }

    public void init() {
    }

    public void forceReload() {
    }

    public List<File> getContentList() {
        List<File> contentPacks = new ArrayList<File>();
        for (File file : ModularWarfare.MOD_DIR.listFiles()) {
            if (!file.getName().contains("cache") && !file.getName().contains("officialmw") && !file.getName().contains("highres")) {
                if (file.isDirectory()) {
                    contentPacks.add(file);
                } else if (zipJar.matcher(file.getName()).matches()) {
                    try {
                        ZipFile zipFile = new ZipFile(file);
                        if (!zipFile.isEncrypted()) {
                            contentPacks.add(file);
                        } else {
                            ModularWarfare.LOGGER.info("[WARNING] ModularWarfare can't load encrypted content-packs in server-side (" + file.getName() + ") !");
                        }
                    } catch (ZipException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        ModularWarfare.LOGGER.info("Loaded content pack list server side.");
        return contentPacks;
    }

    public <T> T loadModel(String s, String shortName, Class<T> typeClass) {
        return null;
    }

    public void spawnExplosionParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(new PacketParticle(ParticleType.EXPLOSION, x, y, z), new TargetPoint(world.provider.getDimension(), x, y, z, 64));
        }
    }

    public void spawnRocketParticle(World world, double x, double y, double z) {
        if (!world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(new PacketParticle(ParticleType.ROCKET, x, y, z), new TargetPoint(world.provider.getDimension(), x, y, z, 64));
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
