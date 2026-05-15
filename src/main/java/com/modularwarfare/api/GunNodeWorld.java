package com.modularwarfare.api;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderGunEnhanced;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * 枪械 GLTF 节点世界坐标（第一人称近似 / 第三人称基于 pre-arm MV 反算）。仅客户端有效。
 */
@SideOnly(Side.CLIENT)
public final class GunNodeWorld {

    private GunNodeWorld() {}

    /** 第一人称：手掌根 × 节点全局矩阵 → 近似世界坐标。 */
    public static Vec3d fp(Matrix4f handRoot, Matrix4f nodeGlobal, Entity view, float partialTicks) {
        return RenderGunEnhanced.fpNodeWorld(handRoot, nodeGlobal, view, partialTicks);
    }

    /**
     * 第三人称：view space 中的 rel（通常为当前 MV 变换下的原点）→ 世界坐标。
     * 须与本帧 {@code drawThirdGun} 写入的 pre-arm 矩阵一致（主手 {@link RenderType#PLAYER} / 副手 {@link RenderType#PLAYER_OFFHAND}）。
     */
    @Nullable
    public static Vec3d tpRel(Vector3f rel, RenderManager rm, EntityLivingBase gunHolder, RenderType thirdHandLayer) {
        return RenderGunEnhanced.tpNodeWorldFromRel(rel, rm, gunHolder, thirdHandLayer);
    }

    /** 第三人称：节点处完整 MODELVIEW → 世界坐标。 */
    @Nullable
    public static Vec3d tpMv(Matrix4f modelViewAtNode, RenderManager rm, EntityLivingBase gunHolder,
            RenderType thirdHandLayer) {
        return RenderGunEnhanced.tpNodeWorldFromMv(modelViewAtNode, rm, gunHolder, thirdHandLayer);
    }

    public static boolean hasTpPreArm(EntityLivingBase holder, RenderType handLayer) {
        return RenderGunEnhanced.hasTpPreArm(holder, handLayer);
    }

    @Nullable
    public static Matrix4f tpPreArmCopy(EntityLivingBase holder, RenderType handLayer) {
        return RenderGunEnhanced.tpPreArmMatrixCopy(holder, handLayer);
    }

    /**
     * 指定实体当前手持枪械某 GLTF 节点在世界中的近似位置（第三人称 layer）。
     * <p>
     * 调用时机：须在本帧已对该实体执行过对应主/副手 {@code drawThirdGun}（从而写入 pre-arm 缓存）之后，
     * 且在合适的 OpenGL 矩阵栈上下文中（例如玩家持枪 Layer 渲染流程内）；否则返回 {@code null}。
     * 动画会与 {@link RenderGunEnhanced#drawThirdGun} 开头同步一次，与完整渲染路径仍可能存在附件/剔除等差异。
     */
    @Nullable
    public static Vec3d playerHeldTp(EntityLivingBase holder, EnumHand hand, String gltfNodeName) {
        if (holder == null || gltfNodeName == null) {
            return null;
        }
        ItemStack stack = holder.getHeldItem(hand);
        if (!(stack.getItem() instanceof ItemGun)) {
            return null;
        }
        GunType gunType = ((ItemGun) stack.getItem()).type;
        if (gunType == null) {
            return null;
        }
        RenderGunEnhanced ren = ClientProxy.gunEnhancedRenderer;
        ModelEnhancedGun model = ren.modelGunThirdPerson(gunType, holder.getUniqueID());
        ren.applyThirdPersonGunAnimSync(model, holder);
        RenderType rt = hand == EnumHand.OFF_HAND ? RenderType.PLAYER_OFFHAND : RenderType.PLAYER;
        return RenderGunEnhanced.tpHeldNodeWorld(holder, rt, model, gltfNodeName,
                Minecraft.getMinecraft().getRenderManager());
    }

    /** 主手第三人称；见 {@link #playerHeldTp(EntityLivingBase, EnumHand, String)}。 */
    @Nullable
    public static Vec3d playerHeldMainTp(EntityLivingBase holder, String gltfNodeName) {
        return playerHeldTp(holder, EnumHand.MAIN_HAND, gltfNodeName);
    }
}
