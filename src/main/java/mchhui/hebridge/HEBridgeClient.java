package mchhui.hebridge;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import org.lwjgl.BufferUtils;

import com.modularwarfare.api.AnimationUtils;
import com.modularwarfare.api.RenderHeldItemLayerEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.loader.api.model.ObjModelRenderer;

import mchhui.he.api.event.entitydisplay.EasyLivingModelEvent.AnimationController;
import mchhui.he.api.event.entitydisplay.EasyLivingModelEvent.CoreHeadWeight;
import mchhui.he.api.event.entitydisplay.EasyLivingModelEvent.NodeBlender;
import mchhui.he.api.event.entitydisplay.EasyLivingModelEvent.RenderOverlay;
import mchhui.he.datapack.data.keepname.DataEasyAnimation.AnimationPiece;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.modularmovements.tactical.client.ClientLitener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HEBridgeClient {
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static HashMap<UUID, PlayerState> states = new HashMap<UUID, HEBridgeClient.PlayerState>();
    public static HashMap<UUID, Boolean> fireTask = new HashMap<UUID, Boolean>();
    public static HashMap<UUID, Boolean> reloadTask = new HashMap<UUID, Boolean>();

    public static class PlayerState {
        public float p_crouch_idle;
        public float p_crouch_walk;
        public float p_slide;
        public float p_prone_idle;
        public float p_prone_walk;
        public float p_lean_left;
        public float p_lean_right;
        public float p_gun_idle;
        public float p_gun_aim;

        public float w_crouch_idle;
        public float w_crouch_walk;
        public float w_slide;
        public float w_prone_idle;
        public float w_prone_walk;
        public float w_lean_left;
        public float w_lean_right;
        public float w_gun_idle;
        public float w_gun_aim;
    }

    public static PlayerState getPlayerState(UUID uid) {
        if (!states.containsKey(uid)) {
            states.put(uid, new PlayerState());
        }
        return states.get(uid);
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderOverlay event) {
        HashSet<String> except = new HashSet<String>();
        except.add("fp_root");

        ItemStack itemstack = event.entity.getHeldItemMainhand();
        if (!(itemstack.getItem() instanceof ItemGun)) {
            return;
        }
        BaseType type = ((BaseItem)itemstack.getItem()).baseType;
        if (!type.hasModel()) {
            return;
        }

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;
        GlStateManager.pushMatrix();
        if (event.model != null && event.model.existNodeState("righthand_mwf_item")) {
            event.model.applyNodeStateTransform("righthand_mwf_item", () -> {
                if (((GunType)type).animationType == WeaponAnimationType.ENHANCED) {

                    GunType gunType = (GunType)type;
                    EnhancedModel model = type.enhancedModel;

                    GunEnhancedRenderConfig config = (GunEnhancedRenderConfig)gunType.enhancedModel.config;

                    if (event.entity instanceof EntityPlayer) {
                        ClientProxy.gunEnhancedRenderer.drawThirdGun(null, RenderType.PLAYER,
                            (EntityPlayer)event.entity, itemstack, false);
                    } else {
                        ClientProxy.gunEnhancedRenderer.drawThirdGun(null, RenderType.PLAYER, null, itemstack);
                    }

                }
            });
        }
        GlStateManager.popMatrix();
        ObjModelRenderer.glowTxtureMode = glowTxtureMode;
    }

    @SubscribeEvent
    public void onCoreHeadWeight(CoreHeadWeight event) {
        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer)event.entity;
        PlayerState state = getPlayerState(player.getUniqueID());
        float ptick = event.ptick;
        boolean aim = AnimationUtils.isAiming.containsKey(player.getName());
        if (!aim) {
            return;
        }
        if (event.node.equals("mwf_core_head")) {
            event.weight = 1 - state.w_gun_aim;
        }
    }

//    @SubscribeEvent
//    @Deprecated
//    public void onNodeBlender(NodeBlender event) {
//        if (!(event.entity instanceof EntityPlayer)) {
//            return;
//        }
//        EntityPlayer player = (EntityPlayer)event.entity;
//        PlayerState state = getPlayerState(player.getUniqueID());
//        float ptick = event.ptick;
//        if (ptick > 1) {
//            ptick = 1;
//        }
//        boolean aim = AnimationUtils.isAiming.containsKey(player.getName());
//        if (!aim) {
//            return;
//        }
//        if (event.node.equals("body_mwf_blender")) {
//            event.rot.rotateAxis(
//                (float)Math.toRadians(
//                    (player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * ptick) * 0.5f),
//                1, 0, 0);
//            float head =
//                interpolateRotation(interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, ptick),
//                    interpolateRotation(player.prevRotationYawHead, player.rotationYawHead, ptick), 0.5f);
//            head -= interpolateRotation(player.prevRenderYawOffset, player.renderYawOffset, ptick);
//            event.rot.rotateAxis((float)-Math.toRadians(head), 0, 1, 0);
//        }
//    }

    public static float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks) {
        float f;
        for (f = yawOffset - prevYawOffset; f < -180.0F; f += 360.0F);
        while (f >= 180.0F) {
            f -= 360.0F;
        }
        return prevYawOffset + partialTicks * f;
    }

    @SubscribeEvent
    public void onAnimationController(AnimationController event) {
        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer)event.entity;
        PlayerState state = getPlayerState(player.getUniqueID());
        float step = event.step;

        float moveSpeed = (float)Math.sqrt((player.posX - player.lastTickPosX) * (player.posX - player.lastTickPosX)
            + (player.posZ - player.lastTickPosZ) * (player.posZ - player.lastTickPosZ));
        boolean movedFlag = moveSpeed > 0.01f;
        if (player == Minecraft.getMinecraft().player) {
            movedFlag = player.moveForward != 0 || player.moveStrafing != 0;
        }
        boolean moved = movedFlag;

        boolean aim = AnimationUtils.isAiming.containsKey(player.getName());

        AnimationPiece animationPiece = event.elm.animation.animations.get("crouch_idle");
        // crouch_idle
        if (animationPiece != null) {
            if (ClientLitener.isSitting(player.getEntityId())
                || (event.entity == Minecraft.getMinecraft().player && ClientLitener.clientPlayerState.isSitting)) {
                if (state.p_crouch_idle < 0 || state.p_crouch_idle >= 1) {
                    state.p_crouch_idle = 0;
                }
                if (!moved) {
                    state.p_crouch_idle +=
                        step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                }
                if (state.p_crouch_idle > 1) {
                    state.p_crouch_idle = 1;
                }
                state.w_crouch_idle += step / event.elm.animation.animations.get("crouch_idle").fadeTime;
            } else {
                state.w_crouch_idle -= step / event.elm.animation.animations.get("crouch_idle").fadeTime;
            }
        }
        if (state.w_crouch_idle < 0) {
            state.w_crouch_idle = 0;
        } else if (state.w_crouch_idle > 1) {
            state.w_crouch_idle = 1;
        }

        // crouch
        animationPiece = event.elm.animation.animations.get("crouch");
        if (animationPiece != null) {
            if ((ClientLitener.isSitting(player.getEntityId()) && moved)
                || (event.entity == Minecraft.getMinecraft().player && ClientLitener.clientPlayerState.isSitting
                    && moved)) {
                if (state.p_crouch_walk < 0 || state.p_crouch_walk >= 1) {
                    state.p_crouch_walk = 0;
                }
                state.p_crouch_walk +=
                    step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                if (state.p_crouch_walk > 1) {
                    state.p_crouch_walk = 1;
                }
                state.w_crouch_walk += step / event.elm.animation.animations.get("crouch").fadeTime;
            } else {
                state.w_crouch_walk -= step / event.elm.animation.animations.get("crouch").fadeTime;
            }
        }
        if (state.w_crouch_walk < 0) {
            state.w_crouch_walk = 0;
        } else if (state.w_crouch_walk > 1) {
            state.w_crouch_walk = 1;
        }

        // slide
        animationPiece = event.elm.animation.animations.get("slide");
        if (animationPiece != null) {
            if ((ClientLitener.isSitting(player.getEntityId()))
                || (event.entity == Minecraft.getMinecraft().player && ClientLitener.clientPlayerState.isSliding)) {
                if (state.p_slide < 0) {
                    state.p_slide = 0;
                }
                state.p_slide += step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                if (state.p_slide > 1) {
                    state.p_slide = 1;
                }
                state.w_slide += step / event.elm.animation.animations.get("slide").fadeTime;
            } else {
                state.w_slide -= step / event.elm.animation.animations.get("slide").fadeTime;
            }
        }
        if (state.w_slide < 0) {
            state.p_slide = 0;
            state.w_slide = 0;
        } else if (state.w_slide > 1) {
            state.w_slide = 1;
        }

        // prone_idle
        animationPiece = event.elm.animation.animations.get("prone_idle");
        if (animationPiece != null) {
            if ((ClientLitener.isCrawling(player.getEntityId()))
                || (event.entity == Minecraft.getMinecraft().player && ClientLitener.clientPlayerState.isCrawling)) {
                if (state.p_prone_idle < 0 || state.p_prone_idle >= 1) {
                    state.p_prone_idle = 0;
                }
                if (!moved) {
                    state.p_prone_idle +=
                        step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                }
                if (state.p_prone_idle > 1) {
                    state.p_prone_idle = 1;
                }
                state.w_prone_idle += step / event.elm.animation.animations.get("prone_idle").fadeTime;
            } else {
                state.w_prone_idle -= step / event.elm.animation.animations.get("prone_idle").fadeTime;
            }
        }
        if (state.w_prone_idle < 0) {
            state.w_prone_idle = 0;
        } else if (state.w_prone_idle > 1) {
            state.w_prone_idle = 1;
        }

        // prone
        animationPiece = event.elm.animation.animations.get("prone");
        if (animationPiece != null) {
            if ((ClientLitener.isCrawling(player.getEntityId()) && moved)
                || (event.entity == Minecraft.getMinecraft().player && ClientLitener.clientPlayerState.isCrawling
                    && moved)) {
                if (state.p_prone_walk < 0 || state.p_prone_walk >= 1) {
                    state.p_prone_walk = 0;
                }
                state.p_prone_walk += step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                if (state.p_prone_walk > 1) {
                    state.p_prone_walk = 1;
                }
                state.w_prone_walk += step / event.elm.animation.animations.get("prone").fadeTime;
            } else {
                state.w_prone_walk -= step / event.elm.animation.animations.get("prone").fadeTime;
            }
        }
        if (state.w_prone_walk < 0) {
            state.w_prone_walk = 0;
        } else if (state.w_prone_walk > 1) {
            state.w_prone_walk = 1;
        }

        // lean
        float lean = 0;
        if (event.entity != Minecraft.getMinecraft().player) {
            if (ClientLitener.ohterPlayerStateMap.containsKey(player.getEntityId())) {
                lean = ClientLitener.ohterPlayerStateMap.get(player.getEntityId()).probeOffset;
            }
        } else {
            lean = ClientLitener.cameraProbeOffset;
        }
        if (lean > 0) {
            state.p_lean_right = lean;
            state.p_lean_left = 0;
        } else {
            state.p_lean_right = 0;
            state.p_lean_left = -lean;
        }
        state.w_lean_right = state.p_lean_right;
        state.w_lean_left = state.p_lean_left;

        // gun_idle
        animationPiece = event.elm.animation.animations.get("gun_idle");
        if (animationPiece != null) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                if (state.p_gun_idle < 0 || state.p_gun_idle >= 1) {
                    state.p_gun_idle = 0;
                }
                if (!moved) {
                    state.p_gun_idle +=
                        step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                }
                if (state.p_gun_idle > 1) {
                    state.p_gun_idle = 1;
                }
                state.w_gun_idle += step / event.elm.animation.animations.get("gun_idle").fadeTime;
            } else {
                state.w_gun_idle -= step / event.elm.animation.animations.get("gun_idle").fadeTime;
            }
        }
        if (state.w_gun_idle < 0) {
            state.w_gun_idle = 0;
        } else if (state.w_gun_idle > 1) {
            state.w_gun_idle = 1;
        }

        // gun_aim
        animationPiece = event.elm.animation.animations.get("gun_aim");
        if (animationPiece != null) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun
                && AnimationUtils.isAiming.containsKey(player.getName())) {
                if (state.p_gun_aim < 0) {
                    state.p_gun_aim = 0;
                }
                state.p_gun_aim += step / (animationPiece.endTime - animationPiece.startTime) * animationPiece.speed;
                if (state.p_gun_aim > 1) {
                    state.p_gun_aim = 1;
                }
                state.w_gun_aim += step / event.elm.animation.animations.get("gun_aim").fadeTime;
            } else {
                state.w_gun_aim -= step / event.elm.animation.animations.get("gun_aim").fadeTime;
            }
        }
        if (state.w_gun_aim < 0) {
            state.p_gun_aim = 0;
            state.w_gun_aim = 0;
        } else if (state.w_gun_aim > 1) {
            state.w_gun_aim = 1;
        }

        event.processMap.put("crouch", state.p_crouch_walk);
        event.processMap.put("crouch_idle", state.p_crouch_idle);
        event.processMap.put("slide", state.p_slide);
        event.processMap.put("prone_idle", state.p_prone_idle);
        event.processMap.put("prone", state.p_prone_walk);
        event.processMap.put("lean_left", state.p_lean_left);
        event.processMap.put("lean_right", state.p_lean_right);
        event.processMap.put("gun_idle", state.p_gun_idle);
        event.processMap.put("gun_aim", state.p_gun_aim);

        event.weightMap.put("crouch", state.w_crouch_walk);
        event.weightMap.put("crouch_idle", state.w_crouch_idle);
        event.weightMap.put("slide", state.w_slide);
        event.weightMap.put("prone_idle", state.w_prone_idle);
        event.weightMap.put("prone", state.w_prone_walk);
        event.weightMap.put("lean_left", state.w_lean_left);
        event.weightMap.put("lean_right", state.w_lean_right);
        event.weightMap.put("gun_idle", state.w_gun_idle);
        event.weightMap.put("gun_aim", state.w_gun_aim);
    }
}
