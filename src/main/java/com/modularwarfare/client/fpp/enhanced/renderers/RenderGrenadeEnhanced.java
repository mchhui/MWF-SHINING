package com.modularwarfare.client.fpp.enhanced.renderers;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.GUN_BALANCING_Y;
import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.SMOOTH_SWING;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.maths.Interpolation;

import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
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

        GrenadeType grenadeType = ((ItemGrenade)item.getItem()).type;
        if (grenadeType == null)
            return;
        ModelEnhancedGrenade model = getOrCreateModel(grenadeType, true, player.getUniqueID());
        if (!(Minecraft.getMinecraft().getRenderViewEntity() instanceof AbstractClientPlayer)) {
            return;
        }
        if (model == null)
            return;
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
         *  global
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
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
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
            model.renderPartExcept(DEFAULT_EXCEPT);
        });
        
        GlStateManager.popMatrix();

        ObjModelRenderer.glowTxtureMode = glow;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        GlStateManager.disableBlend();
    }

    public void blendTransform(ModelEnhancedGrenade model, ItemStack gunStack, boolean basicSprint, float time, float sprintTime, float alpha, String hand, boolean applySprint, boolean skin, Runnable runnable) {

        model.setAnimationCalBlender(new NodeAnimationBlender("FirstPersonBlender") {

            @Override
            public void handle(DataNode node, org.joml.Matrix4f mat) {
                if (!basicSprint) {
                    if (alpha != 0) {
                        sprint:
                        {
                            org.joml.Matrix4f begin_transform = mat;
                            mchhui.hegltf.DataAnimation.Transform end_transform = model.findLocalTransform(node.name, sprintTime);
                            if (end_transform == null) {
                                break sprint;
                            }
                            if (!node.name.equals("root") && !node.name.equals("sprint_lefthand") && !node.name.equals("sprint_righthand") && !node.name.equals("root_bone") && !node.name.equals("sprint_lefthand_bone") && !node.name.equals("sprint_righthand_bone") && !node.name.endsWith("_sprint")) {
                                break sprint;
                            }
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
    }

    public void bindPlayerSkin() {
        bindingTexture = Minecraft.getMinecraft().player.getLocationSkin();
        Minecraft.getMinecraft().renderEngine.bindTexture(bindingTexture);
    }

    public void bindCustomHands(TextureType handTextureType) {
        if (handTextureType.resourceLocations != null) {
            bindingTexture = handTextureType.resourceLocations.get(0);
        }
        Minecraft.getMinecraft().renderEngine.bindTexture(bindingTexture);
    }
}
