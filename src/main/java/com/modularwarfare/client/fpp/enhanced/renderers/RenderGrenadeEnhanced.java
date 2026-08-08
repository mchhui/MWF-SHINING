package com.modularwarfare.client.fpp.enhanced.renderers;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.GUN_BALANCING_Y;
import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.SMOOTH_SWING;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.ModConfig;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ThirdPerson.RenderElement;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.handler.GrenadeEnhancedHandler;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.maths.Interpolation;

import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class RenderGrenadeEnhanced extends CustomItemRendererEnhanced {
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    public static final List<String> defaultHideList = Arrays.asList("leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel");
    static {
        for (String str : defaultHideList) {
            DEFAULT_EXCEPT.add(str);
        }
    }

    @Override
    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        EntityPlayerSP player = (EntityPlayerSP)Minecraft.getMinecraft().player;
        if (!(item.getItem() instanceof ItemGrenade))
            return;

        if (AtomicShaderCompat.shouldSkipLegacyColorDraw()) {
            return;
        }

        if (type == CustomItemRenderType.EQUIPPED_FIRST_PERSON) {
            AtomicShaderCompat.onFirstPersonItemBegin();
        }

        GrenadeType grenadeType = ((ItemGrenade)item.getItem()).type;
        if (grenadeType == null)
            return;
        ModelEnhancedGrenade model = getOrCreateModel(grenadeType, true, player.getUniqueID());
        if (!(Minecraft.getMinecraft().getRenderViewEntity() instanceof AbstractClientPlayer)) {
            return;
        }
        if (model == null)
            return;
        if (!model.isAnimReady() || model.model == null || model.model.geoModel == null
                || model.model.geoModel.nodes == null) {
            return;
        }
        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(Minecraft.getMinecraft().player);
        RenderPlayer renderplayer = (RenderPlayer)render;
        ModelPlayer modelPlayer = renderplayer.getMainModel();
        ClientProxy.renderHooks.hidePlayerModel((AbstractClientPlayer)Minecraft.getMinecraft().getRenderViewEntity(), renderplayer);

        GrenadeEnhancedRenderConfig config = (GrenadeEnhancedRenderConfig)model.config;
        if (AnimationController.getClientController() == null || AnimationController.getClientController().getConfig() != config || AnimationController.getClientController().player != Minecraft.getMinecraft().player) {
            AnimationController.setClientController(new AnimationController(Minecraft.getMinecraft().player, config));
        }
        AnimationController controller = AnimationController.getClientController();
        float partialTicks = getTimer().renderPartialTicks;

        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);

        Matrix4f mat = new Matrix4f();

        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;
        boolean glow = ObjModelRenderer.glowTxtureMode;
        /**
         * INITIAL BLENDER POSITION
         * nonono this is minecrfat hand transform
         */
        //mat.rotate(toRadians(45.0F), new Vector3f(0,1,0));
        //mat.translate(new Vector3f(-1.8f,1.3f,-1.399f));

        /**
         * DEFAULT TRANSFORM
         * */
        //mat.translate(new Vector3f(0,1.3f,-1.8f));
        mat.rotate(toRadians(90.0F), new Vector3f(0, 1, 0));

        /**
         * 诡异的缩放2023.6.7
         * */
        mat.scale(new Vector3f(1 / sizeFactor, 1 / sizeFactor, 1 / sizeFactor));
        //Do hand rotations
        float f5 = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
        float f6 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
        mat.rotate(toRadians((player.rotationPitch - f5) * 0.1F), new Vector3f(1, 0, 0));
        mat.rotate(toRadians((player.rotationYaw - f6) * 0.1F), new Vector3f(0, 1, 0));
        /**
         *  global (x-正左负右 y-正上负下 z-正前负后)
         * */
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
        mat.translate(new Vector3f(0, adsModifier * Interpolation.SINE_IN.interpolate(0F, (-0.2f * (1F - (float)controller.ADS)), GUN_BALANCING_Y), 0));
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
        if (!AtomicShaderCompat.isGBufferFillActive()) {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        } else {
            GlStateManager.disableBlend();
        }
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        model.updateAnimation(controller.getTime(), true);
        /**
         * LEFT HAND GROUP
         * */
        blendTransform(model,item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float) controller.SPRINT, "sprint_lefthand", false, false, () -> {
            /**
             * player left hand
             * */
            bindPlayerSkin();
            ObjModelRenderer.glowTxtureMode=false;
            renderHandAndArmor(EnumHandSide.LEFT, player, config, modelPlayer, model);
            ObjModelRenderer.glowTxtureMode=true;
        });
        /**
         * RIGHT HAND GROUP
         * */
        blendTransform(model,item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float) controller.SPRINT, "sprint_righthand", false, false, () -> {
            /**
             * player left hand
             * */
            bindPlayerSkin();
            ObjModelRenderer.glowTxtureMode=false;
            renderHandAndArmor(EnumHandSide.RIGHT, player, config, modelPlayer, model);
            ObjModelRenderer.glowTxtureMode=true;
            int skinId = 0;
            if (item.hasTagCompound()) {
                if (item.getTagCompound().hasKey("skinId")) {
                    skinId = item.getTagCompound().getInteger("skinId");
                }
            }
            String grenadePath = skinId > 0 ? grenadeType.modelSkins[skinId].getSkin() : grenadeType.modelSkins[0].getSkin();
            bindTexture("grenades", grenadePath);
            AtomicShaderCompat.beginOpaqueFillCapture();
            model.renderPartExcept(DEFAULT_EXCEPT);
            AtomicShaderCompat.afterOpaqueMesh();
        });
        
        GlStateManager.popMatrix();

        ObjModelRenderer.glowTxtureMode = glow;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        GlStateManager.disableBlend();
    }

    public void blendTransform(ModelEnhancedGrenade model, ItemStack grenadeStack, boolean basicSprint, float time, float sprintTime, float alpha, String hand, boolean applySprint, boolean skin, Runnable runnable) {
        if (runnable == null) {
            return;
        }
        
        AnimationType controllerState = AnimationController.getClientController().getPlayingAnimation();
        boolean isDefaultState = controllerState == AnimationType.DEFAULT || controllerState == AnimationType.SPRINT || controllerState == AnimationType.DRAW;
        
        if (isDefaultState && !basicSprint) {
            model.setAnimationCalBlender(new NodeAnimationBlender("FirstPersonBlender") {
                @Override
                public void handle(DataNode node, org.joml.Matrix4f mat) {
                    if (alpha != 0) {
                        sprint: {
                            org.joml.Matrix4f begin_transform = mat;
                            mchhui.hegltf.DataAnimation.Transform end_transform = model.findLocalTransform(node.name, sprintTime);
                            if (end_transform == null) {
                                break sprint;
                            }
                            if (!ModConfig.INSTANCE.sprintBlendNodes.isBlendableNode(node.name)) {
                                break sprint;
                            }
                            Quaternionf quat = new Quaternionf();
                            quat.setFromUnnormalized(begin_transform);
                            quat.normalize().slerp(end_transform.rot.normalize(), alpha);
                            org.joml.Vector3f pos = new org.joml.Vector3f();
                            begin_transform.getTranslation(pos);
                            pos.set(pos.x + (end_transform.pos.x - pos.x) * alpha, 
                                  pos.y + (end_transform.pos.y - pos.y) * alpha, 
                                  pos.z + (end_transform.pos.z - pos.z) * alpha);
                            org.joml.Vector3f size = new org.joml.Vector3f();
                            begin_transform.getScale(size);
                            size.set(size.x + (end_transform.size.x - size.x) * alpha,
                                   size.y + (end_transform.size.y - size.y) * alpha,
                                   size.z + (end_transform.size.z - size.z) * alpha);
                            mat.identity();
                            mat.translate(pos);
                            mat.scale(size);
                            mat.rotate(quat);
                        }
                    }
                }
            });
            model.updateAnimationBlended(time, skin, basicSprint, sprintTime, alpha, 0f, 0f, 0f);
            runnable.run();
            model.setAnimationCalBlender(null);
            return;
        }
        
        model.updateAnimationBlended(time, skin, basicSprint, sprintTime, alpha, 0f, 0f, 0f);
        runnable.run();
    }

    

    public void renderThirdPersonGrenade(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player, ItemStack stack) {
        renderThirdPersonGrenade(renderPlayer, renderType, player, stack, false);
    }

    public void renderThirdPersonGrenade(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player, ItemStack demoStack, boolean sneakFlag) {
        if (!(demoStack.getItem() instanceof ItemGrenade)) return;
        
        if (AtomicShaderCompat.shouldSkipLegacyColorDraw()) {
            return;
        }

        GrenadeType grenadeType = ((ItemGrenade) demoStack.getItem()).type;
        if (grenadeType == null || grenadeType.animationType != WeaponAnimationType.ENHANCED) return;

        ModelEnhancedGrenade model;
        if (renderType == RenderType.ITEMFRAME || renderType == RenderType.ITEMLOOT) {
            String key = renderType.serializedName;
            if(!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != grenadeType) {
                ModelEnhancedGrenade newModel = new ModelEnhancedGrenade((GrenadeEnhancedRenderConfig)grenadeType.enhancedModel.config, grenadeType);
                newModel.model = grenadeType.enhancedModel.model;
                thirdPersonModels.put(key, newModel);
            }
            model = (ModelEnhancedGrenade)thirdPersonModels.get(key);
        } else {
            model = getOrCreateModel(grenadeType, false, player != null ? player.getUniqueID() : null);
        }

        if (model == null) return;
        if (!model.isAnimReady() || model.model == null || model.model.geoModel == null) {
            return;
        }

        GrenadeEnhancedRenderConfig config = (GrenadeEnhancedRenderConfig) model.config;
        
        if (renderType == RenderType.GRENADE) {
            float animTime;
            if (config.animations.containsKey(AnimationType.THROWED)) {
                float throwedDuration = (float) (config.animations.get(AnimationType.THROWED).getEndTime(config.FPS) - 
                                               config.animations.get(AnimationType.THROWED).getStartTime(config.FPS));
                float startTime = (float) config.animations.get(AnimationType.THROWED).getStartTime(config.FPS);
                animTime = startTime + (Minecraft.getMinecraft().world.getTotalWorldTime() % (int)(throwedDuration * 20)) / 20.0f;
            } else {
                float defaultDuration = (float) (config.animations.get(AnimationType.DEFAULT).getEndTime(config.FPS) - 
                                              config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS));
                float startTime = (float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS);
                animTime = startTime + (Minecraft.getMinecraft().world.getTotalWorldTime() % (int)(defaultDuration * 20)) / 20.0f;
            }
            model.updateAnimation(animTime, true);
        } else {
            AnimationController controller;
            EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
            if (player != null) {
                controller = AnimationController.getController(player, config);
                if (controller.getPlayingAnimation() == AnimationType.DEFAULT) {
                    model.updateAnimation(controller.getTime(), true);
                } else {
                    model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS),
                            true);
                }
            } else {
                model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS), true);
            }
        }

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;

        HashSet<String> exceptParts = new HashSet<String>();
        exceptParts.addAll(RenderGunEnhanced.DEFAULT_EXCEPT);

        float worldScale = 1;

        HashSet<String> exceptPartsRendering = exceptParts;

        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        ClientProxy.grenadeEnhancedRenderer.color(1, 1, 1, 1f);

        if (player != null && sneakFlag) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        if (renderPlayer != null && renderPlayer.getMainModel() instanceof ModelBiped) {
            if (renderType == RenderType.PLAYER_OFFHAND) {
                ((ModelBiped) renderPlayer.getMainModel()).bipedLeftArm.postRender(0.0625F);
            } else {
                ((ModelBiped) renderPlayer.getMainModel()).bipedRightArm.postRender(0.0625F);
            }
        }

        RenderElement renderConfigElement = config.thirdPerson.renderElements.get(renderType.serializedName);
        GlStateManager.translate(renderConfigElement.pos.x, renderConfigElement.pos.y, renderConfigElement.pos.z);
        GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
        GlStateManager.scale(renderConfigElement.size.x, renderConfigElement.size.y, renderConfigElement.size.z);
        GlStateManager.rotate(renderConfigElement.rot.y, 0, -1, 0);
        GlStateManager.rotate(renderConfigElement.rot.x, -1, 0, 0);
        GlStateManager.rotate(renderConfigElement.rot.z, 0, 0, -1);

        int skinId = 0;
        if (demoStack.hasTagCompound()) {
            if (demoStack.getTagCompound().hasKey("skinId")) {
                skinId = demoStack.getTagCompound().getInteger("skinId");
            }
        }
        
        String grenadePath = skinId > 0 ? grenadeType.modelSkins[skinId].getSkin() : grenadeType.modelSkins[0].getSkin();
        
        ClientProxy.grenadeEnhancedRenderer.bindTexture("grenades", grenadePath);
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
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
        GlStateManager.color(r, g, b, a);
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
