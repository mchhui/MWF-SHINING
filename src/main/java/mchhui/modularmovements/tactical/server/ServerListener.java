package mchhui.modularmovements.tactical.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.modularwarfare.api.PlayerSnapshotCreateEvent;

import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.network.TacticalHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

public class ServerListener {
    public static Method setSize;
    public static Map<Integer, PlayerState> playerStateMap = new HashMap<Integer, PlayerState>();
    public static Map<Integer, Long> playerNotStepMap = new HashMap<Integer, Long>();

    public void onFMLInit(FMLInitializationEvent event) {
        setSize = ReflectionHelper.findMethod(Entity.class, "setSize", "func_70105_a", Float.TYPE, Float.TYPE);
    }

    @SubscribeEvent
    public void onLogin(PlayerLoggedInEvent event) {
        playerStateMap.put(event.player.getEntityId(), new PlayerState());
        TacticalHandler.sendClientConfig((EntityPlayerMP) event.player);
    }

    @SubscribeEvent
    public void onLogout(PlayerLoggedOutEvent event) {
        playerStateMap.remove(event.player.getEntityId());
        playerNotStepMap.remove(event.player.getEntityId());
    }

    public static double getCameraProbeOffset(Integer id) {
        if (!playerStateMap.containsKey(id)) {
            return 0;
        }
        return playerStateMap.get(id).probeOffset;
    }

    public static boolean isSitting(Integer id) {
        if (!playerStateMap.containsKey(id)) {
            return false;
        }
        return playerStateMap.get(id).isSitting;
    }

    public static boolean isCrawling(Integer id) {
        if (!playerStateMap.containsKey(id)) {
            return false;
        }
        return playerStateMap.get(id).isCrawling;
    }

    public static void updateOffset(Integer id) {
        if (!playerStateMap.containsKey(id)) {
            return;
        }
        playerStateMap.get(id).updateOffset();
    }

    public static Vec3d onGetPositionEyes(EntityPlayer player, float partialTicks, Vec3d vec3d) {
        if (getCameraProbeOffset(player.getEntityId()) != 0) {
            return vec3d.add(new Vec3d(getCameraProbeOffset(player.getEntityId()) * -0.6, 0, 0)
                    .rotateYaw((float) (-Minecraft.getMinecraft().player.rotationYaw * Math.PI / 180f)));
        }
        return vec3d;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.side == Side.SERVER && event.phase == Phase.END) {
            updateOffset(event.player.getEntityId());

            if (isSitting(event.player.getEntityId())) {
                if (event.player.eyeHeight != 1.1f) {
                    event.player.eyeHeight = 1.1f;
                }
            } else if (isCrawling(event.player.getEntityId())) {
                if (event.player.eyeHeight != 0.4f) {
                    event.player.eyeHeight = 0.4f;
                }
            } else if (event.player.eyeHeight == 0.4f) {
                event.player.eyeHeight = event.player.getDefaultEyeHeight();
            } else if (event.player.eyeHeight == 1.1f) {
                event.player.eyeHeight = event.player.getDefaultEyeHeight();
            }

            float f = event.player.width;
            float f1 = event.player.height;
            if (isSitting(event.player.getEntityId())) {
                f1 = 1.2f;
            } else if (isCrawling(event.player.getEntityId())) {
                f1 = 0.5f;
            }

            if (f != event.player.width || f1 != event.player.height) {
                AxisAlignedBB axisalignedbb = event.player.getEntityBoundingBox();
                axisalignedbb = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
                        axisalignedbb.minX + (double) f, axisalignedbb.minY + (double) f1,
                        axisalignedbb.minZ + (double) f);

                if (!event.player.world.collidesWithAnyBlock(axisalignedbb)) {
                    try {
                        setSize.invoke(event.player, f, f1);
                    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event) {
        if (event.getEntity() instanceof EntityPlayer) {
            if (playerNotStepMap.containsKey(event.getEntity().getEntityId())) {
                if (playerNotStepMap.get(event.getEntity().getEntityId()) > System.currentTimeMillis()) {
                    if (SoundEvent.REGISTRY.getNameForObject(event.getSound()).toString().contains("step")) {
                        event.setVolume(0);
                    }
                }
            }
        }
    }

}
