package mchhui.modularmovements.tactical.server;

import com.modularwarfare.common.type.BaseItem;
import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.network.TacticalHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ServerListener {
    public static Method setSize;
    public static Map<Integer, PlayerState> playerStateMap = new HashMap<Integer, PlayerState>();
    public static Map<Integer, Long> playerNotStepMap = new HashMap<Integer, Long>();

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
                    .rotateYaw((float) (-player.rotationYaw * Math.PI / 180f)));
        }
        return vec3d;
    }

    public static void setRotationAngles(com.modularwarfare.raycast.obb.ModelPlayer model, float limbSwing, float limbSwingAmount, float ageInTicks,
                                         float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        if (entityIn instanceof EntityPlayer && entityIn.isEntityAlive()) {
            PlayerState state = null;
            float offest = 0;
            state = playerStateMap.get(entityIn.getEntityId());
            if (state == null) {
                return;
            }
            offest = state.probeOffset;

            if (state.isSitting) {
                model.bipedRightLeg.rotateAngleX = -1.4137167F;
                model.bipedRightLeg.rotateAngleY = ((float) Math.PI / 10F);
                model.bipedRightLeg.rotateAngleZ = 0.07853982F;
                model.bipedLeftLeg.rotateAngleX = -1.4137167F;
                model.bipedLeftLeg.rotateAngleY = -((float) Math.PI / 10F);
                model.bipedLeftLeg.rotateAngleZ = -0.07853982F;
            }

            if (state.isCrawling) {
                model.bipedHead.rotateAngleX -= 70 * 3.14 / 180;
                model.bipedRightArm.rotateAngleX *= 0.2;
                model.bipedLeftArm.rotateAngleX *= 0.2;
                model.bipedRightArm.rotateAngleX += 180 * 3.14 / 180;
                model.bipedLeftArm.rotateAngleX += 180 * 3.14 / 180;
                if (entityIn instanceof EntityPlayer) {
                    ItemStack itemstack = ((EntityPlayer) entityIn).getHeldItemMainhand();
                    if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
                        if (ModularMovements.mwfEnable) {
                            if (itemstack.getItem() instanceof BaseItem) {
                                model.bipedLeftArm.rotateAngleY = 0;
                                model.bipedRightArm.rotateAngleY = 0;
                                model.bipedLeftArm.rotateAngleX = (float) (180 * 3.14 / 180);
                                model.bipedRightArm.rotateAngleX = (float) (180 * 3.14 / 180);
                            }
                        }
                    }
                }
                model.bipedRightLeg.rotateAngleX *= 0.2;
                model.bipedLeftLeg.rotateAngleX *= 0.2;
            }
            if (offest >= 0) {
                model.bipedRightLeg.rotateAngleZ += offest * 20 * 3.14 / 180;
            } else {
                model.bipedLeftLeg.rotateAngleZ += offest * 20 * 3.14 / 180;
            }
        }
    }

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

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
        if (event.side != Side.SERVER) {
            return;
        }
        if (event.phase == Phase.END) {
            updateOffset(event.player.getEntityId());

            if (isSitting(event.player.getEntityId())) {
                if (event.player.eyeHeight != 1.1f) {
                    event.player.eyeHeight = 1.1f;
                }
            } else if (isCrawling(event.player.getEntityId())) {
                if (event.player.eyeHeight != 0.7f) {
                    event.player.eyeHeight = 0.7f;
                }
            } else if (event.player.eyeHeight == 0.7f) {
                event.player.eyeHeight = event.player.getDefaultEyeHeight();
            } else if (event.player.eyeHeight == 1.1f) {
                event.player.eyeHeight = event.player.getDefaultEyeHeight();
            }

            float f = event.player.width;
            float f1 = event.player.height;
            if (isSitting(event.player.getEntityId())) {
                f1 = 1.2f;
            } else if (isCrawling(event.player.getEntityId())) {
                f1 = 0.8f;
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
            PlayerState state = playerStateMap.get(event.player.getEntityId());
            if (state != null) {
                Vec3d vec3d = new Vec3d(-0.6, 0, 0).rotateYaw((float) (-(event.player.rotationYaw - 180) * Math.PI / 180f));
                state.lastAABB = event.player.getEntityBoundingBox();
                state.lastModAABB = state.lastAABB.offset(vec3d.scale(-getCameraProbeOffset(event.player.getEntityId())));
                event.player.setEntityBoundingBox(state.lastModAABB);
            }
        } else {
            PlayerState state = playerStateMap.get(event.player.getEntityId());
            if (state != null) {
                if (state.lastAABB != null && event.player.getEntityBoundingBox() == state.lastModAABB) {
                    event.player.setEntityBoundingBox(state.lastAABB);
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
