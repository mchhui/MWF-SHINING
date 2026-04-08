package safx.client.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

public class SARenderHelper {

	protected static float lastBrightnessX=0;
	protected static float lastBrightnessY=0;

	/** ALPHA_SHADED 批次开始前保存，结束后恢复，避免沿用上一阶段的 240/240 导致仍显全亮 */
	private static float shadedPassSavedBrightnessX;
	private static float shadedPassSavedBrightnessY;
	
	protected static int lastBlendFuncSrc=0;
	protected static int lastBlendFuncDest=0;
	
	/*private static int lastBlendSrc=0;
	private static int lastBlendDst=0;
	private static void snapshotBlendFunc() {
		lastBlendSrc=GL11.glGetInteger(GL11.GL_BLEND_SRC);
		lastBlendDst=GL11.glGetInteger(GL11.GL_BLEND_DST);
	}
	private static void restoreBlendFunc() {
		GL11.glBlendFunc(lastBlendSrc, lastBlendDst);
	}*/

	public static void enableFXLighting()
    {
    	lastBrightnessX= OpenGlHelper.lastBrightnessX;
		lastBrightnessY= OpenGlHelper.lastBrightnessY;

		GlStateManager.disableLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
    }
	
    public static void disableFXLighting()
    {
    	GlStateManager.enableLighting();
    	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
    }
    
    public static void enableFluidGlow(int luminosity) {
    	lastBrightnessX= OpenGlHelper.lastBrightnessX;
		lastBrightnessY= OpenGlHelper.lastBrightnessY;
		
		float newLightX = Math.min((luminosity/15.0f)*240.0f + lastBrightnessX, 240.0f);
		float newLightY = Math.min((luminosity/15.0f)*240.0f + lastBrightnessY, 240.0f);
		
    	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, newLightX, newLightY);
    }
    
    public static void disableFluidGlow() {
    	OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
    }

    /**
     * ALPHA：仅自身颜色（无环境变暗），标准 Alpha 混合，无全局高亮。
     * ADDITIVE：自身颜色 + 加法混合 + 全局高亮。
     * SOLID：不透明感、无混合，无全局高亮。
     * ALPHA_SHADED：自身颜色 + 受方块/天空光照（顶点 lightmap），无全局高亮。
     * NO_Z_TEST：同 ALPHA + 关闭深度测试（穿透），无全局高亮。
     * NO_Z_TEST_ADDITIVE：同 ADDITIVE + 关闭深度测试 + 全局高亮。
     */
    public enum RenderType {
    	ALPHA, ADDITIVE, SOLID, ALPHA_SHADED, NO_Z_TEST, NO_Z_TEST_ADDITIVE;
    }
	
    private static boolean usesHighlight(RenderType type) {
    	return type == RenderType.ADDITIVE || type == RenderType.NO_Z_TEST_ADDITIVE;
    }
	
    public static void enableBlendMode(RenderType type) {
    	if (type != RenderType.SOLID) {
    		GlStateManager.enableBlend();
    	}
        if (type == RenderType.ALPHA || type == RenderType.ALPHA_SHADED || type == RenderType.NO_Z_TEST) {
        	lastBlendFuncSrc = GlStateManager.glGetInteger(GL11.GL_BLEND_SRC);
			lastBlendFuncDest = GlStateManager.glGetInteger(GL11.GL_BLEND_DST);
        	GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        } else if (type == RenderType.ADDITIVE || type == RenderType.NO_Z_TEST_ADDITIVE) {
        	lastBlendFuncSrc = GlStateManager.glGetInteger(GL11.GL_BLEND_SRC);
			lastBlendFuncDest = GlStateManager.glGetInteger(GL11.GL_BLEND_DST);
        	GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
        if (type == RenderType.NO_Z_TEST || type == RenderType.NO_Z_TEST_ADDITIVE) {
        	GlStateManager.depthMask(false);
        	GlStateManager.disableDepth();
        }

        if (type == RenderType.ALPHA_SHADED) {
        	shadedPassSavedBrightnessX = OpenGlHelper.lastBrightnessX;
        	shadedPassSavedBrightnessY = OpenGlHelper.lastBrightnessY;
        	Minecraft mc = Minecraft.getMinecraft();
        	Entity view = mc.getRenderViewEntity();
        	if (view != null && mc.world != null) {
        		BlockPos eyePos = new BlockPos(view.posX, view.posY + (double) view.getEyeHeight(), view.posZ);
        		int combined = mc.world.getCombinedLight(eyePos, 0);
        		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
        				(float) (combined & 65535), (float) (combined >> 16));
        	}
        }

        if (usesHighlight(type)) {
        	SARenderHelper.enableFXLighting();
        }
	}
	
	public static void disableBlendMode(RenderType type) {
		if (type == RenderType.ALPHA_SHADED) {
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
					shadedPassSavedBrightnessX, shadedPassSavedBrightnessY);
		}
		if (usesHighlight(type)) {
			SARenderHelper.disableFXLighting();
		}
		if (type != RenderType.SOLID) {
    		GlStateManager.disableBlend();
    	}
		if (type == RenderType.ALPHA || type == RenderType.ALPHA_SHADED || type == RenderType.ADDITIVE
				|| type == RenderType.NO_Z_TEST || type == RenderType.NO_Z_TEST_ADDITIVE) {
			GlStateManager.blendFunc(lastBlendFuncSrc, lastBlendFuncDest);
        }
        if (type == RenderType.NO_Z_TEST || type == RenderType.NO_Z_TEST_ADDITIVE) {
        	GlStateManager.enableDepth();
        }

	}
	
}
