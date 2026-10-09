package siz.addon.modularprops.client.fpp.enhanced.renderers;

import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.utility.maths.Interpolation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomBlock;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomItem;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomBlockAnimationController;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomItemAnimationController;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType;
import siz.addon.modularprops.client.fpp.enhanced.AnimationCustomBlockType;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.custom.BlockCustom;
import siz.addon.modularprops.common.handler.CustomItemEventHandler;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.hegltf.DataNode;
import org.joml.Quaternionf;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.GUN_BALANCING_Y;
import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.SMOOTH_SWING;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class RenderCustomBlockEnhanced extends CustomItemRenderer {
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    public static final List<String> defaultHideList = Arrays.asList("leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel");
    static {
        for (String str : defaultHideList) {
            DEFAULT_EXCEPT.add(str);
        }
    }
    protected HashMap<String, EnhancedModel> thirdPersonModels = new HashMap<>();
    protected EnhancedModel firstPersonModel;
    protected static float sizeFactor = 10000f;
    protected static final float PI = 3.14159265f;
    private Timer timer;
    protected FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);

    protected static Timer getTimer() {
        if (Minecraft.getMinecraft() != null) {
            try {
                return ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "timer", "field_71428_T");
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    protected static float toRadians(float angdeg) {
        return angdeg / 180.0f * PI;
    }

    protected ModelEnhancedCustomBlock getOrCreateModel(CustomBlockType itemType, boolean isFirstPerson, UUID playerId) {
        // 检查 enhancedModel 是否存在
        if (itemType.enhancedModel == null) {
            System.err.println("[RenderCustomBlockEnhanced] enhancedModel is null for block: " + itemType.internalName);
            return null;
        }

        ModelEnhancedCustomBlock result;
        if (isFirstPerson && itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
            if (firstPersonModel == null || firstPersonModel.baseType != itemType) {
                try {
                    firstPersonModel = new ModelEnhancedCustomBlock((CustomBlockEnhancedRenderConfig)itemType.enhancedModel.config, itemType);
                } catch (Exception e) {
                    System.err.println("[RenderCustomBlockEnhanced] Failed to create first person model for block: " + itemType.internalName);
                    e.printStackTrace();
                }
            }
            result = (ModelEnhancedCustomBlock)firstPersonModel;
        } else {
            String key = playerId != null ? playerId.toString() : "default";
            if (itemType.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                if (!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != itemType) {
                    ModelEnhancedCustomBlock newModel = new ModelEnhancedCustomBlock((CustomBlockEnhancedRenderConfig)itemType.enhancedModel.config, itemType);
                    thirdPersonModels.put(key, newModel);
                }
            }
            result = (ModelEnhancedCustomBlock)thirdPersonModels.get(key);
        }
        if (result != null) {
            result.ensureRequested(mchhui.hegltf.GltfLoadPriority.HIGH);
            mchhui.hegltf.GltfModelManager.get().softPin(result.getModelLocation(), 8000);
        }
        return result;
    }

    /**
     * 重置所有缓存的模型（用于F9重载）
     */
    public void resetModels() {
        // 清空所有缓存的模型
        this.firstPersonModel = null;
        this.thirdPersonModels.clear();
    }

    @Override
    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemBlockCustom)) {
            return;
        }

        if (AtomicShaderCompat.shouldSkipLegacyColorDraw()) {
            return;
        }

        ItemBlockCustom customItem = (ItemBlockCustom)item.getItem();
        CustomBlockType itemType = customItem.type;

        if (itemType == null || itemType.animationType != com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
            return;
        }

        if (type == CustomItemRenderType.EQUIPPED_FIRST_PERSON) {
            World world = data.length > 0 ? (World)data[0] : null;
            EntityPlayer player = data.length > 1 ? (EntityPlayer)data[1] : null;

            if (world == null || player == null) {
                return;
            }

            renderFirstPerson(itemType, item, player, hand);
        }
    }

    private void renderFirstPerson(CustomBlockType itemType, ItemStack stack, EntityPlayer playerParm, EnumHand hand) {
        ModelEnhancedCustomBlock model = getOrCreateModel(itemType, true, playerParm.getUniqueID());
        if (model == null || !model.isAnimReady() || model.model == null || model.model.geoModel == null) {
            return;
        }

        CustomBlockEnhancedRenderConfig config = (CustomBlockEnhancedRenderConfig)model.config;
        if (config == null) {
            return;
        }

        if (this.timer == null) {
            this.timer = getTimer();
        }
        float partialTicks = this.timer != null ? this.timer.renderPartialTicks : 1.0f;

        // 获取玩家模型
        net.minecraft.client.renderer.entity.Render<?> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(playerParm);
        if (!(render instanceof net.minecraft.client.renderer.entity.RenderPlayer)) {
            return;
        }
        net.minecraft.client.renderer.entity.RenderPlayer renderPlayer = (net.minecraft.client.renderer.entity.RenderPlayer)render;
        ModelPlayer modelPlayer = renderPlayer.getMainModel();

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        // 从事件处理器获取动画控制器（动画在事件处理器中更新）
        CustomBlockAnimationController controller = siz.addon.modularprops.common.handler.CustomItemEventHandler.getBlockAnimationController(playerParm.getUniqueID(), itemType);

        if (controller == null) {
            return;
        }
        EntityPlayerSP player = (EntityPlayerSP)playerParm; // 暂时未使用
        Matrix4f mat = new Matrix4f();

        GlStateManager.pushMatrix();

        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;
        boolean glow = ObjModelRenderer.glowTxtureMode;
        /**
         * INITIAL BLENDER POSITION nonono this is minecrfat hand transform
         */
        // mat.rotate(toRadians(45.0F), new Vector3f(0,1,0));
        // mat.translate(new Vector3f(-1.8f,1.3f,-1.399f));

        /**
         * DEFAULT TRANSFORM
         */
        // mat.translate(new Vector3f(0,1.3f,-1.8f));
        mat.rotate(toRadians(90.0F), new Vector3f(0, 1, 0));

        /**
         * 诡异的缩放2023.6.7
         */
        mat.scale(new Vector3f(1 / sizeFactor, 1 / sizeFactor, 1 / sizeFactor));
        // Do hand rotations
        float f5 = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
        float f6 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
        mat.rotate(toRadians((player.rotationPitch - f5) * 0.1F), new Vector3f(1, 0, 0));
        mat.rotate(toRadians((player.rotationYaw - f6) * 0.1F), new Vector3f(0, 1, 0));
        /**
         * global (x-正左负右 y-正上负下 z-正前负后)
         */
        mat.rotate(toRadians(90), new Vector3f(0, 1, 0));
        mat.translate(new Vector3f(config.global.globalTranslate.x, config.global.globalTranslate.y, config.global.globalTranslate.z));
        mat.scale(new Vector3f(config.global.globalScale.x, config.global.globalScale.y, config.global.globalScale.z));
        mat.rotate(toRadians(-90), new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.y / 180 * 3.14f, new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.x / 180 * 3.14f, new Vector3f(1, 0, 0));
        mat.rotate(config.global.globalRotate.z / 180 * 3.14f, new Vector3f(0, 0, 1));

        /**
         * ACTION FORWARD
         */
        float adsModifier = 1;
        float f1 = (player.distanceWalkedModified - player.prevDistanceWalkedModified);
        float f2 = -(player.distanceWalkedModified + f1 * partialTicks);
        float f3 = (player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks);
        float f4 = (player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * partialTicks);
        mat.translate(new Vector3f(0, adsModifier * Interpolation.SINE_IN.interpolate(0F, -0.2f, GUN_BALANCING_Y), 0));
        mat.translate(new Vector3f(0, adsModifier * ((float)(0.05f * (Math.sin(SMOOTH_SWING / 10) * GUN_BALANCING_Y))), 0));

        mat.rotate(toRadians(adsModifier * 0.1f * Interpolation.SINE_OUT.interpolate(-GUN_BALANCING_Y, GUN_BALANCING_Y, adsModifier * MathHelper.sin(f2 * (float)Math.PI))), new Vector3f(0f, 1f, 0f));

        mat.translate(new Vector3f(adsModifier * MathHelper.sin(f2 * (float)Math.PI) * f3 * 0.5F, adsModifier * -Math.abs(MathHelper.cos(f2 * (float)Math.PI) * f3), 0.0F));
        mat.rotate(toRadians(adsModifier * MathHelper.sin(f2 * (float)Math.PI) * f3 * 3.0F), new Vector3f(0.0F, 0.0F, 1.0F));
        mat.rotate(toRadians(adsModifier * Math.abs(MathHelper.cos(f2 * (float)Math.PI - 0.2F) * f3) * 5.0F), new Vector3f(1.0F, 0.0F, 0.0F));
        mat.rotate(toRadians(adsModifier * f4), new Vector3f(1.0F, 0.0F, 0.0F));

        floatBuffer.clear();
        mat.get(floatBuffer);
        floatBuffer.rewind();
        GlStateManager.pushMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        GlStateManager.multMatrix(floatBuffer);
        // 禁用混合以取消透明效果
        GlStateManager.disableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        model.updateAnimation(controller.getTime(), true);
        /**
         * LEFT HAND GROUP
         */
        blendTransform(model, stack, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float)controller.SPRINT, "sprint_lefthand", false, false, () -> {
            /**
             * player left hand
             */
            bindPlayerSkin();
            ObjModelRenderer.glowTxtureMode = false;
            renderHandAndArmor(EnumHandSide.LEFT, player, config, modelPlayer, model);
            ObjModelRenderer.glowTxtureMode = true;
        });
        /**
         * RIGHT HAND GROUP
         */
        blendTransform(model, stack, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float)controller.SPRINT, "sprint_righthand", false, false, () -> {
            /**
             * player left hand
             */
            bindPlayerSkin();
            ObjModelRenderer.glowTxtureMode = false;
            renderHandAndArmor(EnumHandSide.RIGHT, player, config, modelPlayer, model);
            ObjModelRenderer.glowTxtureMode = true;
            // 绑定纹理并渲染模型
            int skinId = 0;
            if (stack.hasTagCompound() && stack.getTagCompound() != null && stack.getTagCompound().hasKey("skinId")) {
                skinId = stack.getTagCompound().getInteger("skinId");
            }
            String itemPath = skinId > 0 && itemType.modelSkins.length > skinId ? 
                itemType.modelSkins[skinId].getSkin() : itemType.modelSkins[0].getSkin();
            bindTexture("customblocks", itemPath);
            AtomicShaderCompat.beginOpaqueFillCapture();
            model.renderPartExcept(DEFAULT_EXCEPT);
            AtomicShaderCompat.afterOpaqueMesh();
        });

        GlStateManager.popMatrix();

        ObjModelRenderer.glowTxtureMode = glow;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        // 确保混合已禁用
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
    
    /**
     * 第三人称渲染方法（参考 RenderGrenadeEnhanced.renderThirdPersonGrenade）
     */
    public void renderThirdPersonCustomBlock(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player, ItemStack demoStack, boolean sneakFlag) {
        if (!(demoStack.getItem() instanceof ItemBlockCustom)) return;

        if (AtomicShaderCompat.shouldSkipLegacyColorDraw()) {
            return;
        }
        
        ItemBlockCustom customItem = (ItemBlockCustom) demoStack.getItem();
        CustomBlockType itemType = customItem.type;
        if (itemType == null || itemType.animationType != com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) return;

        ModelEnhancedCustomBlock model;
        if (renderType == RenderType.ITEMFRAME || renderType == RenderType.ITEMLOOT) {
            String key = renderType.serializedName;
            if(!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != itemType) {
                ModelEnhancedCustomBlock newModel = new ModelEnhancedCustomBlock((CustomBlockEnhancedRenderConfig)itemType.enhancedModel.config, itemType);
                newModel.model = itemType.enhancedModel.model;
                thirdPersonModels.put(key, newModel);
            }
            model = (ModelEnhancedCustomBlock)thirdPersonModels.get(key);
        } else {
            model = getOrCreateModel(itemType, false, player != null ? player.getUniqueID() : null);
        }

        if (model == null || !model.isAnimReady() || model.model == null || model.model.geoModel == null) {
            return;
        }

        CustomBlockEnhancedRenderConfig config = (CustomBlockEnhancedRenderConfig) model.config;
        
        // 对于实体渲染，使用默认动画
        if (renderType == RenderType.PLAYER){
            // 对于玩家手持第三人称，优先使用 THIRD_DEFAULT 动画（与近战一致）
            if (config.customAnimations.containsKey(AnimationCustomBlockType.THIRD_DEFAULT)) {
                model.updateAnimation((float) config.customAnimations.get(AnimationCustomBlockType.THIRD_DEFAULT).getStartTime(config.FPS), true);
            } else if (config.customAnimations.containsKey(AnimationCustomBlockType.DEFAULT)) {
                    model.updateAnimation((float) config.customAnimations.get(AnimationCustomBlockType.DEFAULT).getStartTime(config.FPS), true);
            }
        }else {
            float animTime;
            if (config.customAnimations.containsKey(AnimationCustomBlockType.DEFAULT)) {
                float defaultDuration = (float) (config.customAnimations.get(AnimationCustomBlockType.DEFAULT).getEndTime(config.FPS) - 
                                              config.customAnimations.get(AnimationCustomBlockType.DEFAULT).getStartTime(config.FPS));
                float startTime = (float) config.customAnimations.get(AnimationCustomBlockType.DEFAULT).getStartTime(config.FPS);
                animTime = startTime + (Minecraft.getMinecraft().world.getTotalWorldTime() % (int)(defaultDuration * 20)) / 20.0f;
            } else {
                animTime = 0.0f;
            }
            model.updateAnimation(animTime, true);
        }

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;

        HashSet<String> exceptParts = new HashSet<String>();
        exceptParts.addAll(DEFAULT_EXCEPT);

        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        color(1, 1, 1, 1f);

        if (player != null && sneakFlag) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        if (renderPlayer != null && renderPlayer.getMainModel() instanceof net.minecraft.client.model.ModelBiped) {
            if (renderType == RenderType.PLAYER_OFFHAND) {
                ((net.minecraft.client.model.ModelBiped) renderPlayer.getMainModel()).bipedLeftArm.postRender(0.0625F);
            } else {
                ((net.minecraft.client.model.ModelBiped) renderPlayer.getMainModel()).bipedRightArm.postRender(0.0625F);
            }
        }

        EnhancedRenderConfig.ThirdPerson.RenderElement renderConfigElement = config.thirdPerson.renderElements.get(renderType.serializedName);
        if (renderConfigElement != null) {
            GlStateManager.translate(renderConfigElement.pos.x, renderConfigElement.pos.y, renderConfigElement.pos.z);
            GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
            GlStateManager.scale(renderConfigElement.size.x, renderConfigElement.size.y, renderConfigElement.size.z);
            GlStateManager.rotate(renderConfigElement.rot.y, 0, -1, 0);
            GlStateManager.rotate(renderConfigElement.rot.x, -1, 0, 0);
            GlStateManager.rotate(renderConfigElement.rot.z, 0, 0, -1);
        }

        int skinId = 0;
        if (demoStack.hasTagCompound() && demoStack.getTagCompound() != null && demoStack.getTagCompound().hasKey("skinId")) {
            skinId = demoStack.getTagCompound().getInteger("skinId");
        }
        
        String itemPath = skinId > 0 && itemType.modelSkins.length > skinId ? 
            itemType.modelSkins[skinId].getSkin() : itemType.modelSkins[0].getSkin();
        
        bindTexture("customblocks", itemPath);
        exceptParts.addAll(Arrays.asList(
                "leftArmModel", "leftArmLayerModel",
                "rightArmModel", "rightArmLayerModel",
                "leftArmSlimModel", "leftArmLayerSlimModel",
                "rightArmSlimModel", "rightArmLayerSlimModel"
            ));
        AtomicShaderCompat.beginOpaqueFillCapture();
        model.renderPartExcept(exceptParts);
        AtomicShaderCompat.afterOpaqueMesh();

        ObjModelRenderer.glowTxtureMode = glowTxtureMode;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }
    
    public void color(float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
    }

    public void renderHandAndArmor(EnumHandSide side, AbstractClientPlayer player, EnhancedRenderConfig config, ModelPlayer modelPlayer, EnhancedModel model) {
        // 实现手臂渲染逻辑，参考CustomItemRendererEnhanced
        if (side == EnumHandSide.LEFT) {
            if (config.showHandArmorType != EnhancedRenderConfig.ShowHandArmorType.NONE) {
                com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer leftFirst = new com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer(this, EnumHandSide.LEFT);
                com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer leftSecond = new com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer(this, EnumHandSide.LEFT);
                MinecraftForge.EVENT_BUS.post(leftFirst);
                MinecraftForge.EVENT_BUS.post(leftSecond);

                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    if (!leftFirst.isCanceled() && modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                        model.renderPart("leftArmModel");
                    }
                    if (!leftSecond.isCanceled() && modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                        model.renderPart("leftArmLayerModel");
                    }
                } else {
                    if (!leftFirst.isCanceled() && modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                        model.renderPart("leftArmSlimModel");
                    }
                    if (!leftSecond.isCanceled() && modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                        model.renderPart("leftArmLayerSlimModel");
                    }
                }
            } else {
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    model.renderPart(LEFT_HAND_PART);
                } else {
                    model.renderPart(LEFT_SLIM_HAND_PART);
                }
            }
        } else {
            if (config.showHandArmorType != EnhancedRenderConfig.ShowHandArmorType.NONE) {
                com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer rightFirst = new com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer(this, EnumHandSide.RIGHT);
                com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer rightSecond = new com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer(this, EnumHandSide.RIGHT);
                MinecraftForge.EVENT_BUS.post(rightFirst);
                MinecraftForge.EVENT_BUS.post(rightSecond);

                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    if (!rightFirst.isCanceled() && modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                        model.renderPart("rightArmModel");
                    }
                    if (!rightSecond.isCanceled() && modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                        model.renderPart("rightArmLayerModel");
                    }
                } else {
                    if (!rightFirst.isCanceled() && modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                        model.renderPart("rightArmSlimModel");
                    }
                    if (!rightSecond.isCanceled() && modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                        model.renderPart("rightArmLayerSlimModel");
                    }
                }
            } else {
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    model.renderPart(RIGHT_HAND_PART);
                } else {
                    model.renderPart(RIGHT_SLIM_HAND_PART);
                }
            }
        }
    }

    /**
     * Sprint 动画混合方法（参考 RenderMelee）
     */
    private void blendTransform(ModelEnhancedCustomBlock model, ItemStack stack, boolean basicSprint, float time, float sprintTime, float alpha, String hand, boolean applySprint, boolean skin, Runnable runnable) {
        if (runnable == null || model == null || model.model == null) {
            return;
        }

        model.setAnimationCalBlender(new NodeAnimationBlender("FirstPersonBlender") {
            @Override
            public void handle(DataNode node, org.joml.Matrix4f mat) {
                if (!basicSprint && alpha > 0) {
                    sprint:
                    {
                        org.joml.Matrix4f begin_transform = mat;
                        mchhui.hegltf.DataAnimation.Transform end_transform = model.findLocalTransform(node.name, sprintTime);
                        if (end_transform == null) {
                            break sprint;
                        }
                        // 检查节点是否可混合（简化处理，实际应该从配置读取）
                        // 可以检查节点名是否包含特定后缀或使用配置
                        Quaternionf quat = new Quaternionf();
                        quat.setFromUnnormalized(begin_transform);
                        quat.normalize().slerp(end_transform.rot.normalize(), alpha);
                        org.joml.Vector3f pos = new org.joml.Vector3f();
                        begin_transform.getTranslation(pos);
                        pos.set(pos.x + (end_transform.pos.x - pos.x) * alpha, pos.y + (end_transform.pos.y - pos.y) * alpha, pos.z + (end_transform.pos.z - pos.z) * alpha);
                        org.joml.Vector3f size = new org.joml.Vector3f();
                        begin_transform.getScale(size);
                        size.set(size.x + (end_transform.size.x - size.x) * alpha, size.y + (end_transform.size.y - size.y) * alpha, size.z + (end_transform.size.z - size.z) * alpha);
                        mat.identity();
                        mat.translate(pos);
                        mat.scale(size);
                        mat.rotate(quat);
                    }
                }
            }
        });

        model.updateAnimation(time, skin);
        runnable.run();
        model.setAnimationCalBlender(null);
    }

    @Override
    public void bindTexture(String type, String fileName) {
        super.bindTexture(type, fileName);
        // String pathFormat = "skins/%s/%s.png";
        // bindTexture(new ResourceLocation(ModularWarfare.MOD_ID,
        // String.format(pathFormat, type, fileName)));
    }

    public void bindTexture(ResourceLocation location) {
        bindingTexture = location;
        Minecraft.getMinecraft().renderEngine.bindTexture(bindingTexture);
        AtomicShaderCompat.ensurePbrMapsForBoundAlbedo(bindingTexture);
    }

    public void bindPlayerSkin() {
        bindingTexture = AtomicShaderCompat.resolveReadyPlayerSkin(Minecraft.getMinecraft().player);
        AtomicShaderCompat.bindFillAlbedo(bindingTexture);
    }

    public void bindCustomHands(TextureType handTextureType) {
        if (handTextureType.resourceLocations != null) {
            bindingTexture = handTextureType.resourceLocations.get(0);
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(bindingTexture);
        AtomicShaderCompat.ensurePbrMapsForBoundAlbedo(bindingTexture);
    }
}

