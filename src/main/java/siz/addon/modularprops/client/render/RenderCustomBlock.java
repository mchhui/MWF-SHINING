package siz.addon.modularprops.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.client.compat.AtomicShaderCompat;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomBlock;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.TileEntityCustomBlock;

@SideOnly(Side.CLIENT)
public class RenderCustomBlock extends TileEntitySpecialRenderer<TileEntityCustomBlock> {
    
    // 为每个TileEntity位置缓存独立的模型实例（避免模型共享导致动画互相影响）
    private final HashMap<String, ModelEnhancedCustomBlock> tileEntityModels = new HashMap<>();
    
    // 排除玩家手臂的部件（与第三人称渲染一致）
    private static final HashSet<String> DEFAULT_EXCEPT = new HashSet<>(Arrays.asList(
        "leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel",
        "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel"
    ));

    @Override
    public void render(@Nonnull TileEntityCustomBlock te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if (te == null || te.getWorld() == null) {
            return;
        }

        if (AtomicShaderCompat.shouldSkipLegacyColorDraw()) {
            return;
        }

        // 获取方块类型
        CustomBlockType blockType = null;
        if (te.getWorld().getBlockState(te.getPos()).getBlock() instanceof siz.addon.modularprops.common.custom.BlockCustom) {
            siz.addon.modularprops.common.custom.BlockCustom block = (siz.addon.modularprops.common.custom.BlockCustom) te.getWorld().getBlockState(te.getPos()).getBlock();
            blockType = block.type;
        }

        if (blockType == null || blockType.enhancedModel == null) {
            return;
        }

        // 为每个TileEntity位置创建独立的模型实例（避免与手持/第三人称共享模型导致动画互相影响）
        String teKey = te.getPos().toString();
        ModelEnhancedCustomBlock model = tileEntityModels.get(teKey);
        
        // 如果模型不存在或类型改变，创建新的独立模型实例
        if (model == null || model.baseType != blockType) {
            // 获取配置并创建新的模型实例
            CustomBlockEnhancedRenderConfig config = com.modularwarfare.ModularWarfare.getRenderConfig(
                blockType, 
                CustomBlockEnhancedRenderConfig.class
            );
            
            if (config != null) {
                model = new ModelEnhancedCustomBlock(config, blockType);
                tileEntityModels.put(teKey, model);
                // System.out.println("[RenderCustomBlock] Created new model instance for TE at " + teKey + 
                //     " | Model instance: " + System.identityHashCode(model) + 
                //     " | GltfRenderModel instance: " + System.identityHashCode(model.model));
            } else {
                // 配置加载失败，不应该使用共享模型！
                System.err.println("[RenderCustomBlock] Config is null for block: " + blockType.internalName + " at " + teKey);
                return;
            }
        }
        
        if (model == null || !model.isAnimReady() || model.model == null || model.model.geoModel == null) {
            return;
        }

        CustomBlockEnhancedRenderConfig config = (CustomBlockEnhancedRenderConfig) model.config;
        if (config == null) {
            return;
        }

        GlStateManager.pushMatrix();
        
        // 启用平滑法线渲染
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        
        // 不透明渲染（禁用混合，使用深度测试）
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        
        GlStateManager.disableCull();

        // 移动到方块位置
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        
        // 获取方块朝向并应用旋转（使用blockFacing配置）
        if (config.blockFacing.autoRotateByFacing) {
            EnumFacing facing = getFacing(te);
            float facingRotation = getFacingRotation(facing) + config.blockFacing.facingRotationOffset;
            GlStateManager.rotate(facingRotation, 0, 1, 0);
        }
        
        // 获取block渲染元素配置
        com.modularwarfare.client.fpp.enhanced.configs.RenderType blockRenderType = 
            com.modularwarfare.client.fpp.enhanced.configs.RenderType.BLOCK;
        
        // 获取渲染配置（如果没有block配置，使用默认值）
        com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ThirdPerson.RenderElement renderElement = 
            config.thirdPerson.renderElements.get(blockRenderType.serializedName);
        
        if (renderElement != null) {
            // 应用渲染配置（参考第三人称的渲染顺序：translate -> scale -> rotate）
            GlStateManager.translate(renderElement.pos.x, renderElement.pos.y, renderElement.pos.z);
            
            // 应用缩放（参考第三人称，先缩小10倍再应用配置缩放）
            GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
            GlStateManager.scale(renderElement.size.x, renderElement.size.y, renderElement.size.z);
            
            // 应用旋转（参考第三人称的旋转顺序：Y -> X -> Z）
            GlStateManager.rotate(renderElement.rot.y, 0, -1, 0);
            GlStateManager.rotate(renderElement.rot.x, -1, 0, 0);
            GlStateManager.rotate(renderElement.rot.z, 0, 0, -1);
        } else {
            // 默认缩放
            GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
        }

        // 绑定贴图
        if (blockType.modelSkins != null && blockType.modelSkins.length > 0) {
            String skinPath = blockType.modelSkins[0].getSkin();
            net.minecraft.util.ResourceLocation textureLocation = new net.minecraft.util.ResourceLocation(
                com.modularwarfare.ModularWarfare.MOD_ID,
                "skins/" + blockType.getAssetDir() + "/" + skinPath + ".png"
            );
            Minecraft.getMinecraft().getTextureManager().bindTexture(textureLocation);
            AtomicShaderCompat.bindFillAlbedo(textureLocation);
        }
        
        // 计算动画时间（循环播放，优先级：thirdPlace > thirdDefault > default）
        float animationTime;
        com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.Animation anim = null;
        
        // 如果方块正在被破坏，优先播放break动画（TODO：需要支持break动画）
        // if (te.isBreaking() && config.customAnimations.containsKey(...BREAK)) { ... }
        
        // 循环播放优先级：thirdPlace > thirdDefault > default
        if (config.customAnimations != null) {
            if (config.customAnimations.containsKey(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.THIRD_PLACE)) {
                anim = config.customAnimations.get(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.THIRD_PLACE);
            } else if (config.customAnimations.containsKey(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.THIRD_DEFAULT)) {
                anim = config.customAnimations.get(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.THIRD_DEFAULT);
            } else if (config.customAnimations.containsKey(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.DEFAULT)) {
                anim = config.customAnimations.get(siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType.DEFAULT);
            }
        }
        
        if (anim != null) {
            float startTime = (float) anim.getStartTime(config.FPS);
            float endTime = (float) anim.getEndTime(config.FPS);
            float duration = endTime - startTime;
            
            // 使用世界时间循环播放动画
            float worldTime = (te.getWorld().getTotalWorldTime() + partialTicks) / 20.0f;
            animationTime = startTime + (worldTime % duration);
        } else {
            // 如果没有任何动画配置，使用世界时间
            animationTime = (te.getWorld().getTotalWorldTime() + partialTicks) / 20.0F;
        }
        
        model.updateAnimation(animationTime, false);

        // 渲染模型（排除玩家手臂部件，使用不透明渲染）
        if (model.model != null && model.model.geoModel != null && model.model.geoModel.loaded) {
            AtomicShaderCompat.beginOpaqueFillCapture();
            model.renderPartExcept(DEFAULT_EXCEPT);
            AtomicShaderCompat.afterOpaqueMesh();
        }

        // 恢复渲染状态
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.enableCull();
        GlStateManager.popMatrix();
    }

    /**
     * 获取方块的朝向（从TileEntity获取）
     */
    private EnumFacing getFacing(TileEntityCustomBlock te) {
        if (te == null) {
            return EnumFacing.NORTH;
        }
        return te.getFacing();
    }

    /**
     * 根据朝向返回对应的旋转角度（参考玩家视角）
     */
    private float getFacingRotation(EnumFacing facing) {
        switch (facing) {
            case SOUTH:
                return 180;  // 南 (Z+)
            case WEST:
                return 90;   // 西 (X-)
            case NORTH:
                return 0;    // 北 (Z-)
            case EAST:
                return 270;  // 东 (X+)
            default:
                return 0;
        }
    }
    
    /**
     * 清理指定位置的模型缓存（当方块被破坏时调用）
     */
    public void clearModelCache(net.minecraft.util.math.BlockPos pos) {
        if (pos != null) {
            tileEntityModels.remove(pos.toString());
        }
    }
    
    /**
     * 清理所有模型缓存
     */
    public void clearAllModelCache() {
        tileEntityModels.clear();
    }
}

