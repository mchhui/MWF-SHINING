package safx.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Shared camera frustum for SAFX particles and BeanMod gibs.
 * Same contract as HE {@code ClientFrustumCache}: vanilla planes from current
 * PROJECTION*MODELVIEW, {@code setPosition} on interpolated view-entity world pos,
 * tests world-space AABBs. Not for shadow passes.
 */
@SideOnly(Side.CLIENT)
public final class SAFrustumCache {

	private static final Frustum FRUSTUM = new Frustum(ClippingHelperImpl.getInstance());
	private static float lastPartialTicks = -1.0f;
	private static int lastRenderPass = Integer.MIN_VALUE;

	private SAFrustumCache() {
	}

	public static void ensureUpdated(float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		Entity view = mc.getRenderViewEntity();
		if (view == null) {
			return;
		}
		int pass = forgeRenderPass();
		if (partialTicks == lastPartialTicks && pass == lastRenderPass) {
			return;
		}
		boolean invertView = mc.gameSettings != null && mc.gameSettings.thirdPersonView == 2;
		ActiveRenderInfo.updateRenderInfo(view, invertView);
		ClippingHelperImpl.getInstance();
		double pt = partialTicks;
		double px = view.lastTickPosX + (view.posX - view.lastTickPosX) * pt;
		double py = view.lastTickPosY + (view.posY - view.lastTickPosY) * pt;
		double pz = view.lastTickPosZ + (view.posZ - view.lastTickPosZ) * pt;
		FRUSTUM.setPosition(px, py, pz);
		lastPartialTicks = partialTicks;
		lastRenderPass = pass;
	}

	public static boolean isBoxInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return FRUSTUM.isBoxInFrustum(minX, minY, minZ, maxX, maxY, maxZ);
	}

	public static boolean isBoundingBoxInFrustum(AxisAlignedBB box) {
		if (box == null) {
			return true;
		}
		return FRUSTUM.isBoundingBoxInFrustum(box);
	}

	private static int forgeRenderPass() {
		try {
			return net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
		} catch (Throwable ignored) {
			return 0;
		}
	}
}
