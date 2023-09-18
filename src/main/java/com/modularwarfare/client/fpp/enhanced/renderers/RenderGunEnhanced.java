package com.modularwarfare.client.fpp.enhanced.renderers;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer;
import com.modularwarfare.api.RenderHandSleeveEnhancedEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ShowHandArmorType;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Attachment;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.ObjectControl;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.ThirdPerson.RenderElement;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Transform;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.client.shader.Programs;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.data.VarBoolean;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
import com.modularwarfare.utility.OptifineHelper;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.maths.Interpolation;
import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.modularmovements.tactical.client.ClientLitener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.optifine.shaders.Shaders;
import org.joml.Quaternionf;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class RenderGunEnhanced extends CustomItemRenderer {
    public static final float PI = 3.14159265f;
    public static final int BULLET_MAX_RENDER = 256;
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    private static final String[] LEFT_HAND_PART = new String[]{
            "leftArmModel", "leftArmLayerModel"
    };
    private static final String[] LEFT_SLIM_HAND_PART = new String[]{
            "leftArmSlimModel", "leftArmLayerSlimModel"
    };
    private static final String[] RIGHT_HAND_PART = new String[]{
            "rightArmModel", "rightArmLayerModel"
    };
    private static final String[] RIGHT_SLIM_HAND_PART = new String[]{
            "rightArmSlimModel", "rightArmLayerSlimModel"
    };
    public static float sizeFactor = 10000f;
    public static boolean debug = false;
    public static boolean debug1 = false;
    private static float theata90 = (float) Math.toRadians(90);

    static {
        for (String str : ModConfig.INSTANCE.guns.anim_guns_show_default_objects) {
            DEFAULT_EXCEPT.add(str);
        }
        for (int i = 0; i < BULLET_MAX_RENDER; i++) {
            DEFAULT_EXCEPT.add("bulletModel_" + i);
        }
    }

    public AnimationController controller = new AnimationController(null, null);
    public HashMap<String, AnimationController> otherControllers = new HashMap<String, AnimationController>();
    public FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
    private ShortBuffer pixelBuffer = null;
    private int lastWidth;
    private int lastHeight;
    private Timer timer;
    private boolean renderingMagazine = true;

    public static float toRadians(float angdeg) {
        return angdeg / 180.0f * PI;
    }

    public AnimationController getController(EntityPlayer player, GunEnhancedRenderConfig config) {
        if (player == Minecraft.getMinecraft().player) {
            if (controller.player != player || controller.getConfig() != config) {
                controller = new AnimationController(player, config);
            }
            return controller;
        }
        String name = player.getName();
        if (config == null && !otherControllers.containsKey(name)) {
            return null;
        }
        if (!otherControllers.containsKey(name)) {
            otherControllers.put(name, new AnimationController(player, config));
        }
        if (config != null && otherControllers.get(name).getConfig() != config) {
            otherControllers.put(name, new AnimationController(player, config));
        }
        return otherControllers.get(name);
    }

    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemGun))
            return;

        GunType gunType = ((ItemGun) item.getItem()).type;
        if (gunType == null)
            return;

        ModelEnhancedGun model = (ModelEnhancedGun) gunType.enhancedModel;
        if (!(Minecraft.getMinecraft().getRenderViewEntity() instanceof AbstractClientPlayer)) {
            return;
        }

        if (model == null)
            return;

        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(Minecraft.getMinecraft().player);
        RenderPlayer renderplayer = (RenderPlayer) render;
        ModelPlayer modelPlayer = renderplayer.getMainModel();
        ClientProxy.renderHooks.hidePlayerModel((AbstractClientPlayer) Minecraft.getMinecraft().getRenderViewEntity(), renderplayer);

        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) model.config;
        if (this.controller == null || this.controller.getConfig() != config || this.controller.player != Minecraft.getMinecraft().player) {
            this.controller = new AnimationController(Minecraft.getMinecraft().player, config);
        }


        if (this.timer == null) {
            this.timer = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "timer", "field_71428_T");
        }

        if (!item.hasTagCompound())
            return;

        float partialTicks = this.timer.renderPartialTicks;

        EntityPlayerSP player = (EntityPlayerSP) Minecraft.getMinecraft().player;

        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);

        Matrix4f mat = new Matrix4f();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();

        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;
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

        float rotateX = 0;
        float adsModifier = (float) (0.95f - controller.ADS);

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
         * ACTION GUN MOTION
         */
        float gunRotX = RenderParameters.GUN_ROT_X_LAST
                + (RenderParameters.GUN_ROT_X - RenderParameters.GUN_ROT_X_LAST) * ClientProxy.renderHooks.partialTicks;
        float gunRotY = RenderParameters.GUN_ROT_Y_LAST
                + (RenderParameters.GUN_ROT_Y - RenderParameters.GUN_ROT_Y_LAST) * ClientProxy.renderHooks.partialTicks;
        mat.rotate(toRadians(gunRotX), new Vector3f(0, -1, 0));
        mat.rotate(toRadians(gunRotY), new Vector3f(0, 0, -1));

        /**
         * ACTION FORWARD
         */
        float f1 = (player.distanceWalkedModified - player.prevDistanceWalkedModified);
        float f2 = -(player.distanceWalkedModified + f1 * partialTicks);
        float f3 = (player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks);
        float f4 = (player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * partialTicks);

        mat.translate(new Vector3f(0, adsModifier * Interpolation.SINE_IN.interpolate(0F, (-0.2f * (1F - (float) controller.ADS)), GUN_BALANCING_Y), 0));
        mat.translate(new Vector3f(0, adsModifier * ((float) (0.05f * (Math.sin(SMOOTH_SWING / 10) * GUN_BALANCING_Y))), 0));

        mat.rotate(toRadians(adsModifier * 0.1f * Interpolation.SINE_OUT.interpolate(-GUN_BALANCING_Y, GUN_BALANCING_Y, adsModifier * MathHelper.sin(f2 * (float) Math.PI))), new Vector3f(0f, 1f, 0f));

        mat.translate(new Vector3f(adsModifier * MathHelper.sin(f2 * (float) Math.PI) * f3 * 0.5F, adsModifier * -Math.abs(MathHelper.cos(f2 * (float) Math.PI) * f3), 0.0F));
        mat.rotate(toRadians(adsModifier * MathHelper.sin(f2 * (float) Math.PI) * f3 * 3.0F), new Vector3f(0.0F, 0.0F, 1.0F));
        mat.rotate(toRadians(adsModifier * Math.abs(MathHelper.cos(f2 * (float) Math.PI - 0.2F) * f3) * 5.0F), new Vector3f(1.0F, 0.0F, 0.0F));
        mat.rotate(toRadians(adsModifier * f4), new Vector3f(1.0F, 0.0F, 0.0F));

        /**
         * ACTION GUN COLLIDE
         */
        float collideFrontDistanceAlpha = RenderParameters.collideFrontDistance;
        float rotateZ = (10F * collideFrontDistanceAlpha);
        float translateX = -(15F * collideFrontDistanceAlpha);
        float translateY = -(2F * collideFrontDistanceAlpha);
        mat.translate(new Vector3f(0, translateY, 0));
        mat.rotate(toRadians(rotateZ), new Vector3f(0, 0, 1));
        mat.translate(new Vector3f(translateX, 0, 0));

        /**
         * ACTION GUN SWAY
         */
        RenderParameters.VAL = (float) (Math.sin(RenderParameters.SMOOTH_SWING / 100) * 8);
        RenderParameters.VAL2 = (float) (Math.sin(RenderParameters.SMOOTH_SWING / 80) * 8);
        RenderParameters.VALROT = (float) (Math.sin(RenderParameters.SMOOTH_SWING / 90) * 1.2f);
        mat.translate(new Vector3f(0f, ((VAL / 500) * (0.95f - (float) controller.ADS)), ((VAL2 / 500 * (0.95f - (float) controller.ADS)))));
        mat.rotate(toRadians(adsModifier * VALROT), new Vector3f(1F, 0F, 0F));

        /**
         * ACTION GUN BALANCING X / Y
         */
        mat.translate(new Vector3f((float) (0.1f * GUN_BALANCING_X * Math.cos(Math.PI * RenderParameters.SMOOTH_SWING / 50)) * (1F - (float) controller.ADS), 0, 0));
        rotateX -= (GUN_BALANCING_X * 4F) + (float) (GUN_BALANCING_X * Math.sin(Math.PI * RenderParameters.SMOOTH_SWING / 35));
        rotateX -= (float) Math.sin(Math.PI * GUN_BALANCING_X);
        rotateX -= (GUN_BALANCING_X) * 0.4F;
        /**
         * ACTION PROBE
         */
        if (ModularWarfare.isLoadedModularMovements) {
            rotateX += 15F * ClientLitener.cameraProbeOffset;
        }
        mat.rotate(toRadians(rotateX), new Vector3f(1f, 0f, 0f));

        /**
         * ACTION SPRINT
         */
        RenderParameters.VALSPRINT = (float) (Math.cos(controller.SPRINT_RANDOM * 2 * Math.PI)) * gunType.moveSpeedModifier;
        RenderParameters.VALSPRINT2 = (float) (Math.sin(controller.SPRINT_RANDOM * 2 * Math.PI)) * gunType.moveSpeedModifier;

        Vector3f customSprintRotation = new Vector3f();
        Vector3f customSprintTranslate = new Vector3f();
        float springModifier = (float) (0.8f - controller.ADS);
        mat.rotate(toRadians(0.2f * VALSPRINT * springModifier), new Vector3f(1, 0, 0));
        mat.rotate(toRadians(VALSPRINT2 * springModifier), new Vector3f(0, 0, 1));
        mat.translate(new Vector3f(VALSPRINT * 0.2f * springModifier, 0, VALSPRINT2 * 0.2f * springModifier));

        customSprintRotation = new Vector3f((config.sprint.sprintRotate.x * (float) controller.SPRINT), (config.sprint.sprintRotate.y * (float) controller.SPRINT), (config.sprint.sprintRotate.z * (float) controller.SPRINT));
        customSprintTranslate = new Vector3f((config.sprint.sprintTranslate.x * (float) controller.SPRINT), (config.sprint.sprintTranslate.y * (float) controller.SPRINT), (config.sprint.sprintTranslate.z * (float) controller.SPRINT));

        customSprintRotation.scale((1F - (float) controller.ADS));
        customSprintTranslate.scale((1F - (float) controller.ADS));
        /**
         * CUSTOM HIP POSITION
         */

        Vector3f customHipRotation = new Vector3f(config.aim.rotateHipPosition.x, config.aim.rotateHipPosition.y, config.aim.rotateHipPosition.z);
        Vector3f customHipTranslate = new Vector3f(config.aim.translateHipPosition.x, (config.aim.translateHipPosition.y), (config.aim.translateHipPosition.z));

        Vector3f customAimRotation = new Vector3f((config.aim.rotateAimPosition.x * (float) controller.ADS), (config.aim.rotateAimPosition.y * (float) controller.ADS), (config.aim.rotateAimPosition.z * (float) controller.ADS));
        Vector3f customAimTranslate = new Vector3f((config.aim.translateAimPosition.x * (float) controller.ADS), (config.aim.translateAimPosition.y * (float) controller.ADS), (config.aim.translateAimPosition.z * (float) controller.ADS));

        mat.rotate(toRadians(customHipRotation.x + customSprintRotation.x + customAimRotation.x), new Vector3f(1f, 0f, 0f));
        mat.rotate(toRadians(customHipRotation.y + customSprintRotation.y + customAimRotation.y), new Vector3f(0f, 1f, 0f));
        mat.rotate(toRadians(customHipRotation.z + customSprintRotation.z + customAimRotation.z), new Vector3f(0f, 0f, 1f));
        mat.translate(new Vector3f(customHipTranslate.x + customSprintTranslate.x + customAimTranslate.x, customHipTranslate.y + customSprintTranslate.y + customAimTranslate.y, customHipTranslate.z + customSprintTranslate.z + customAimTranslate.z));

        float renderInsideGunOffset = 5;

        /**
         * ATTACHMENT AIM
         * */
        ItemAttachment sight = null;
        if (GunType.getAttachment(item, AttachmentPresetEnum.Sight) != null) {
            sight = (ItemAttachment) GunType.getAttachment(item, AttachmentPresetEnum.Sight).getItem();
            Attachment sightConfig = config.attachment.get(sight.type.internalName);
            if (sightConfig != null) {
                //System.out.println("test");
                float ads = (float) controller.ADS;
                mat.translate((Vector3f) new Vector3f(sightConfig.sightAimPosOffset).scale(ads));
                mat.rotate(ads * sightConfig.sightAimRotOffset.y * 3.14f / 180, new Vector3f(0, 1, 0));
                mat.rotate(ads * sightConfig.sightAimRotOffset.x * 3.14f / 180, new Vector3f(1, 0, 0));
                mat.rotate(ads * sightConfig.sightAimRotOffset.z * 3.14f / 180, new Vector3f(0, 0, 1));
                renderInsideGunOffset = sightConfig.renderInsideGunOffset;
            }
        }

        /**
         * RECOIL
         */
        /** Random Shake */
        float min = -1.5f;
        float max = 1.5f;
        float randomNum = new Random().nextFloat();
        float randomShake = min + (randomNum * (max - min));

        float alpha = anim.lastGunRecoil + (anim.gunRecoil - anim.lastGunRecoil) * partialTicks;
        float bounce = Interpolation.BOUNCE_INOUT.interpolate(0F, 1F, alpha);
        float elastic = Interpolation.ELASTIC_OUT.interpolate(0F, 1F, alpha);

        float sin = MathHelper.sin((float) (2 * Math.PI * alpha));

        float sin10 = MathHelper.sin((float) (2 * Math.PI * alpha)) * 0.05f;

        //枪托抖动影响参数
        float modelBackwardsFactor = 1.0f;
        float modelUpwardsFactor = 1.0f;
        float modelShakeFactor = 1.0f;

        if (player.getHeldItemMainhand() != null) {
            if (player.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemStack itemStack = GunType.getAttachment(player.getHeldItemMainhand(), AttachmentPresetEnum.Stock);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                    modelBackwardsFactor = ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilBackwardsFactor;
                    modelUpwardsFactor = ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilUpwardsFactor;
                    modelShakeFactor = ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilShakeFactor;
                }
            }
        }

        mat.translate(new Vector3f(-(bounce) * config.extra.modelRecoilBackwards * (float) (1 - controller.ADS) * modelBackwardsFactor, 0F, 0F));
        mat.translate(new Vector3f(0F, (-(elastic) * config.extra.modelRecoilBackwards * modelBackwardsFactor) * 0.05F, 0F));

        mat.translate(new Vector3f(0F, 0F, sin10 * anim.recoilSide * config.extra.modelRecoilUpwards * modelUpwardsFactor));

        mat.rotate(toRadians(sin * anim.recoilSide * config.extra.modelRecoilUpwards * modelUpwardsFactor), new Vector3f(0F, 0F, 1F));
        mat.rotate(toRadians(5F * sin10 * anim.recoilSide * config.extra.modelRecoilUpwards * modelUpwardsFactor), new Vector3f(0F, 0F, 1F));

        mat.rotate(toRadians((bounce) * config.extra.modelRecoilUpwards), new Vector3f(0F, 0F, 1F));

        mat.rotate(toRadians(((-alpha) * randomShake * config.extra.modelRecoilShake * modelShakeFactor)), new Vector3f(0.0f, 1.0f, 0.0f));
        mat.rotate(toRadians(((-alpha) * randomShake * config.extra.modelRecoilShake * modelShakeFactor)), new Vector3f(1.0f, 0.0f, 0.0f));

        if (ScopeUtils.isIndsideGunRendering) {
            mat.translate(new Vector3f(-renderInsideGunOffset, 0, 0));
        }

        floatBuffer.clear();
        mat.store(floatBuffer);
        floatBuffer.rewind();

        GL11.glMultMatrix(floatBuffer);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        if (ScopeUtils.isIndsideGunRendering) {
            GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
        }
        float worldScale = 1;
        float rotateXRendering = rotateX;
        CROSS_ROTATE = rotateXRendering;
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        color(1, 1, 1, 1f);

        boolean applySprint = controller.SPRINT > 0.1 && controller.INSPECT >= 1;
        boolean isRenderHand0 = ScopeUtils.isRenderHand0 || !OptifineHelper.isShadersEnabled();
        HashSet<String> exceptParts = new HashSet<String>();
        if (isRenderHand0) {
            exceptParts.addAll(config.defaultHidePart);
            //exceptParts.addAll(DEFAULT_EXCEPT);

            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(item, attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                    String binding = "gunModel";
                    if (config.attachmentGroup.containsKey(attachment.typeName)) {
                        if (config.attachmentGroup.get(attachment.typeName).hidePart != null) {
                            exceptParts.addAll(config.attachmentGroup.get(attachment.typeName).hidePart);
                        }
                    }
                    if (config.attachment.containsKey(attachmentType.internalName)) {
                        if (config.attachment.get(attachmentType.internalName).hidePart != null) {
                            exceptParts.addAll(config.attachment.get(attachmentType.internalName).hidePart);
                        }
                    }
                }
            }

            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(item, attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                    String binding = "gunModel";
                    if (config.attachmentGroup.containsKey(attachment.typeName)) {
                        if (config.attachmentGroup.get(attachment.typeName).showPart != null) {
                            exceptParts.removeAll(config.attachmentGroup.get(attachment.typeName).showPart);
                        }
                    }
                    if (config.attachment.containsKey(attachmentType.internalName)) {
                        if (config.attachment.get(attachmentType.internalName).showPart != null) {
                            exceptParts.removeAll(config.attachment.get(attachmentType.internalName).showPart);
                        }
                    }
                }
            }

            exceptParts.addAll(DEFAULT_EXCEPT);
        }

        HashSet<String> exceptPartsRendering = exceptParts;


        //model.updateAnimation(controller.getTime(),false);

        /**
         * RIGHT HAND GROUP
         * */

        final ItemAttachment sightRendering = sight;

        boolean glowMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;
        blendTransform(model, item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float) controller.SPRINT, "sprint_righthand", applySprint, true, () -> {
            if (isRenderHand0) {
                if (sightRendering != null) {
                    String binding = "gunModel";
                    if (config.attachment.containsKey(sightRendering.type.internalName)) {
                        binding = config.attachment.get(sightRendering.type.internalName).binding;
                    }
                    model.applyGlobalTransformToOther(binding, () -> {
                        renderAttachment(config, AttachmentPresetEnum.Sight.typeName, sightRendering.type.internalName, () -> {
                            writeScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model, controller.ADS > 0, worldScale, sightRendering.type.sight.modeType.isPIP);
                        });
                    });
                }

                /**
                 * player right hand
                 * */

                if (gunType.handsTextureType != null) {
                    bindCustomHands(gunType.handsTextureType);
                } else {
                    bindPlayerSkin();
                }
                ObjModelRenderer.glowTxtureMode = false;
                renderHandAndArmor(EnumHandSide.RIGHT, player, config, modelPlayer, model);
                ObjModelRenderer.glowTxtureMode = true;
                /**
                 * gun
                 * */
                int skinId = 0;
                if (item.hasTagCompound()) {
                    if (item.getTagCompound().hasKey("skinId")) {
                        skinId = item.getTagCompound().getInteger("skinId");
                    }
                }
                String gunPath = skinId > 0 ? gunType.modelSkins[skinId].getSkin() : gunType.modelSkins[0].getSkin();
                bindTexture("guns", gunPath);
                model.renderPartExcept(exceptPartsRendering);

                /**
                 * selecotr
                 * */
                WeaponFireMode fireMode = GunType.getFireMode(item);
                if (fireMode == WeaponFireMode.SEMI) {
                    model.renderPart("selector_semi");
                } else if (fireMode == WeaponFireMode.FULL) {
                    model.renderPart("selector_full");
                } else if (fireMode == WeaponFireMode.BURST) {
                    model.renderPart("selector_burst");
                }


                /**
                 * ammo and bullet
                 * */
                boolean flagDynamicAmmoRendered = false;
                ItemStack stackAmmo = new ItemStack(item.getTagCompound().getCompoundTag("ammo"));
                ItemStack orignalAmmo = stackAmmo;
                stackAmmo = controller.getRenderAmmo(stackAmmo);
                ItemStack renderAmmo = stackAmmo;
                ItemStack prognosisAmmo = ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;

                ItemStack bulletStack = ItemStack.EMPTY;
                int currentAmmoCount = 0;

                VarBoolean defaultBulletFlag = new VarBoolean();
                defaultBulletFlag.b = true;
                boolean defaultAmmoFlag = true;

                if (gunType.acceptedBullets != null) {
                    currentAmmoCount = item.getTagCompound().getInteger("ammocount");
                    if (anim.reloading) {
                        currentAmmoCount += anim.getAmmoCountOffset(true);
                    }
                    bulletStack = new ItemStack(item.getTagCompound().getCompoundTag("bullet"));
                    if (anim.reloading) {
                        bulletStack = ClientProxy.gunEnhancedRenderer.controller.getRenderAmmo(bulletStack);
                    }
                } else {
                    Integer currentMagcount = null;
                    if (stackAmmo != null && !stackAmmo.isEmpty() && stackAmmo.hasTagCompound()) {
                        if (stackAmmo.getTagCompound().hasKey("magcount")) {
                            currentMagcount = stackAmmo.getTagCompound().getInteger("magcount");
                        }
                        currentAmmoCount = ReloadHelper.getBulletOnMag(stackAmmo, currentMagcount);
                        bulletStack = new ItemStack(stackAmmo.getTagCompound().getCompoundTag("bullet"));
                    }
                }
                int currentAmmoCountRendering = currentAmmoCount;

                if (bulletStack != null) {
                    if (bulletStack.getItem() instanceof ItemBullet) {
                        BulletType bulletType = ((ItemBullet) bulletStack.getItem()).type;
                        if (bulletType.isDynamicBullet && bulletType.model != null) {
                            int skinIdBullet = 0;
                            if (bulletStack.hasTagCompound()) {
                                if (bulletStack.getTagCompound().hasKey("skinId")) {
                                    skinIdBullet = bulletStack.getTagCompound().getInteger("skinId");
                                }
                            }
                            if (bulletType.sameTextureAsGun) {
                                bindTexture("guns", gunPath);
                            } else {
                                String pathAmmo = skinIdBullet > 0 ? bulletType.modelSkins[skinIdBullet].getSkin()
                                        : bulletType.modelSkins[0].getSkin();
                                bindTexture("bullets", pathAmmo);
                            }
                            for (int bullet = 0; bullet < currentAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                                int renderBullet = bullet;
                                model.applyGlobalTransformToOther("bulletModel_" + bullet, () -> {
                                    renderAttachment(config, "bullet", bulletType.internalName, () -> {
                                        bulletType.model.renderPart("bulletModel", worldScale);
                                    });
                                });
                            }
                            model.applyGlobalTransformToOther("bulletModel", () -> {
                                renderAttachment(config, "bullet", bulletType.internalName, () -> {
                                    bulletType.model.renderPart("bulletModel", worldScale);
                                });
                            });
                            defaultBulletFlag.b = false;
                        }
                    }
                }

                ItemStack[] ammoList = new ItemStack[]{stackAmmo, orignalAmmo, prognosisAmmo};
                String[] binddings = new String[]{"ammoModel", "ammoModelPre", "ammoModelPost"};
                for (int x = 0; x < 3; x++) {
                    ItemStack stackAmmoX = ammoList[x];
                    if (stackAmmoX == null || stackAmmoX.isEmpty()) {
                        continue;
                    }
                    if (!model.existPart(binddings[x])) {
                        continue;
                    }
                    if (stackAmmoX.getItem() instanceof ItemAmmo) {
                        ItemAmmo itemAmmo = (ItemAmmo) stackAmmoX.getItem();
                        AmmoType ammoType = itemAmmo.type;
                        if (ammoType.isDynamicAmmo && ammoType.model != null) {
                            int skinIdAmmo = 0;
                            int baseAmmoCount = 0;

                            if (stackAmmoX.hasTagCompound()) {
                                if (stackAmmoX.getTagCompound().hasKey("skinId")) {
                                    skinIdAmmo = stackAmmoX.getTagCompound().getInteger("skinId");
                                }
                                if (stackAmmoX.getTagCompound().hasKey("magcount")) {
                                    baseAmmoCount = (stackAmmoX.getTagCompound().getInteger("magcount") - 1) * ammoType.ammoCapacity;
                                }
                            }
                            int baseAmmoCountRendering = baseAmmoCount;

                            if (ammoType.sameTextureAsGun) {
                                bindTexture("guns", gunPath);
                            } else {
                                String pathAmmo = skinIdAmmo > 0 ? ammoType.modelSkins[skinIdAmmo].getSkin() : ammoType.modelSkins[0].getSkin();
                                bindTexture("ammo", pathAmmo);
                            }

                            if (controller.shouldRenderAmmo()) {
                                model.applyGlobalTransformToOther("ammoModel", () -> {
                                    GlStateManager.pushMatrix();
                                    if (renderAmmo.getTagCompound().hasKey("magcount")) {
                                        if (config.attachment.containsKey(itemAmmo.type.internalName)) {
                                            if (config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform != null) {
                                                if (renderAmmo.getTagCompound().getInteger("magcount") <= config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.size()) {
                                                    //be careful, don't mod the config
                                                    Transform ammoTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(renderAmmo.getTagCompound().getInteger("magcount") - 1);
                                                    Transform renderTransform = ammoTransform;
                                                    if (anim.reloading && (anim
                                                            .getReloadAnimationType() == AnimationType.RELOAD_FIRST_QUICKLY)) {
                                                        float magAlpha = (float) controller.RELOAD;
                                                        renderTransform = new Transform();
                                                        ammoTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(prognosisAmmo.getTagCompound().getInteger("magcount") - 1);
                                                        Transform beginTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(orignalAmmo.getTagCompound().getInteger("magcount") - 1);

                                                        renderTransform.translate.x = beginTransform.translate.x
                                                                + (ammoTransform.translate.x - beginTransform.translate.x)
                                                                * magAlpha;
                                                        renderTransform.translate.y = beginTransform.translate.y
                                                                + (ammoTransform.translate.y - beginTransform.translate.y)
                                                                * magAlpha;
                                                        renderTransform.translate.z = beginTransform.translate.z
                                                                + (ammoTransform.translate.z - beginTransform.translate.z)
                                                                * magAlpha;

                                                        renderTransform.rotate.x = beginTransform.rotate.x
                                                                + (ammoTransform.rotate.x - beginTransform.rotate.x)
                                                                * magAlpha;
                                                        renderTransform.rotate.y = beginTransform.rotate.y
                                                                + (ammoTransform.rotate.y - beginTransform.rotate.y)
                                                                * magAlpha;
                                                        renderTransform.rotate.z = beginTransform.rotate.z
                                                                + (ammoTransform.rotate.z - beginTransform.rotate.z)
                                                                * magAlpha;

                                                        renderTransform.scale.x = beginTransform.scale.x
                                                                + (ammoTransform.scale.x - beginTransform.scale.x)
                                                                * magAlpha;
                                                        renderTransform.scale.y = beginTransform.scale.y
                                                                + (ammoTransform.scale.y - beginTransform.scale.y)
                                                                * magAlpha;
                                                        renderTransform.scale.z = beginTransform.scale.z
                                                                + (ammoTransform.scale.z - beginTransform.scale.z)
                                                                * magAlpha;
                                                    }
                                                    GlStateManager.translate(renderTransform.translate.x,
                                                            renderTransform.translate.y, renderTransform.translate.z);
                                                    GlStateManager.scale(renderTransform.scale.x, renderTransform.scale.y,
                                                            renderTransform.scale.z);
                                                    GlStateManager.rotate(renderTransform.rotate.y, 0, 1, 0);
                                                    GlStateManager.rotate(renderTransform.rotate.x, 1, 0, 0);
                                                    GlStateManager.rotate(renderTransform.rotate.z, 0, 0, 1);
                                                }
                                            }
                                        }
                                    }
                                    renderAttachment(config, "ammo", ammoType.internalName, () -> {
                                        ammoType.model.renderPart("ammoModel", worldScale);
                                        if (defaultBulletFlag.b) {
                                            if (renderAmmo.getTagCompound().hasKey("magcount")) {
                                                for (int i = 1; i <= ammoType.magazineCount; i++) {
                                                    int count = ReloadHelper.getBulletOnMag(renderAmmo, i);
                                                    for (int bullet = 0; bullet < count && bullet < BULLET_MAX_RENDER; bullet++) {
                                                        //System.out.println((ammoType.ammoCapacity*(i-1))+bullet);
                                                        ammoType.model.renderPart("bulletModel_" + ((ammoType.ammoCapacity * (i - 1)) + bullet), worldScale);
                                                    }
                                                }
                                            } else {
                                                for (int bullet = 0; bullet < currentAmmoCountRendering && bullet < BULLET_MAX_RENDER; bullet++) {
                                                    ammoType.model.renderPart("bulletModel_" + (baseAmmoCountRendering + bullet), worldScale);
                                                }
                                            }

                                            defaultBulletFlag.b = false;
                                        }
                                    });
                                    GlStateManager.popMatrix();
                                });
                                model.applyGlobalTransformToOther("bulletModel", () -> {
                                    renderAttachment(config, "bullet", ammoType.internalName, () -> {
                                        ammoType.model.renderPart("bulletModel", worldScale);
                                    });
                                });
                                flagDynamicAmmoRendered = true;
                                defaultAmmoFlag = false;
                            }
                        }
                    }
                }

                /**
                 * default bullet and ammo
                 * */

                bindTexture("guns", gunPath);

                if (defaultBulletFlag.b) {
                    for (int bullet = 0; bullet < currentAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                        model.renderPart("bulletModel_" + bullet);
                    }
                    model.renderPart("bulletModel");
                }

                if (controller.shouldRenderAmmo() && defaultAmmoFlag) {
                    model.renderPart("ammoModel");
                }


                /**
                 * attachment
                 * */

                for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                    ItemStack itemStack = GunType.getAttachment(item, attachment);
                    if (itemStack != null && itemStack.getItem() != Items.AIR) {
                        AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                        ModelAttachment attachmentModel = (ModelAttachment) attachmentType.model;

                        if (ScopeUtils.isIndsideGunRendering) {
                            if (attachment == AttachmentPresetEnum.Sight) {
                                if (config.attachment.containsKey(attachmentType.internalName)) {
                                    if (!config.attachment.get(attachmentType.internalName).renderInsideSightModel) {
                                        continue;
                                    }
                                } else {
                                    continue;
                                }
                            }
                        }

                        if (attachmentModel != null) {
                            String binding = "gunModel";
                            if (config.attachment.containsKey(attachmentType.internalName)) {
                                binding = config.attachment.get(attachmentType.internalName).binding;
                            }
                            model.applyGlobalTransformToOther(binding, () -> {
                                if (attachmentType.sameTextureAsGun) {
                                    bindTexture("guns", gunPath);
                                } else {
                                    int attachmentsSkinId = 0;
                                    if (itemStack.hasTagCompound()) {
                                        if (itemStack.getTagCompound().hasKey("skinId")) {
                                            attachmentsSkinId = itemStack.getTagCompound().getInteger("skinId");
                                        }
                                    }
                                    String attachmentsPath = attachmentsSkinId > 0 ? attachmentType.modelSkins[attachmentsSkinId].getSkin()
                                            : attachmentType.modelSkins[0].getSkin();
                                    bindTexture("attachments", attachmentsPath);
                                }
                                renderAttachment(config, attachment.typeName, attachmentType.internalName, () -> {
                                    attachmentModel.renderAttachment(worldScale);
                                    ObjModelRenderer.glowTxtureMode = false;
                                    if (attachment == AttachmentPresetEnum.Sight) {
                                        renderScopeGlass(attachmentType, attachmentModel, controller.ADS > 0, worldScale);
                                    }
                                    ObjModelRenderer.glowTxtureMode = true;
                                });
                            });
                        }

                        if (attachment == AttachmentPresetEnum.Sight) {
                            ClientRenderHooks.isAiming = false;
                            ClientRenderHooks.isAimingScope = false;
                            WeaponScopeModeType modeType = attachmentType.sight.modeType;
                            if (modeType.isMirror) {
                                if (controller.ADS == 1) {
                                    if (!ClientRenderHooks.isAimingScope) {
                                        ClientRenderHooks.isAimingScope = true;
                                    }
                                } else {
                                    if (ClientRenderHooks.isAimingScope) {
                                        ClientRenderHooks.isAimingScope = false;
                                    }
                                }
                            }
                        }
                    }
                }

                /**
                 *  flashmodel
                 *  */

                ObjModelRenderer.glowTxtureMode = false;
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                boolean shouldRenderFlash = true;
                if ((GunType.getAttachment(item, AttachmentPresetEnum.Barrel) != null)) {
                    AttachmentType attachmentType = ((ItemAttachment) GunType.getAttachment(item, AttachmentPresetEnum.Barrel).getItem()).type;
                    if (attachmentType.attachmentType == AttachmentPresetEnum.Barrel) {
                        shouldRenderFlash = !attachmentType.barrel.hideFlash;
                    }
                }

                if (shouldRenderFlash && anim.shooting && anim.getShootingAnimationType() == AnimationType.FIRE && !player.isInWater()) {
                    GlStateManager.disableLighting();
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                    TextureType flashType = gunType.flashType;
                    bindTexture(flashType.resourceLocations.get(anim.flashCount % flashType.resourceLocations.size()));
                    model.renderPart("flashModel");
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
                    GlStateManager.enableLighting();
                }

                //model.renderPart("smokeModel");
                GlStateManager.depthMask(true);
                ObjModelRenderer.glowTxtureMode = true;
            }
        });

        /**
         * LEFT HAND GROUP
         * */
        blendTransform(model, item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float) controller.SPRINT, "sprint_lefthand", applySprint, false, () -> {
            if (isRenderHand0) {
                /**
                 * player left hand
                 * */
                if (gunType.handsTextureType != null) {
                    bindCustomHands(gunType.handsTextureType);
                } else {
                    bindPlayerSkin();
                }
                ObjModelRenderer.glowTxtureMode = false;
                renderHandAndArmor(EnumHandSide.LEFT, player, config, modelPlayer, model);
                ObjModelRenderer.glowTxtureMode = true;
            }
        });

        if (sightRendering != null) {
            if (!ScopeUtils.isIndsideGunRendering) {
                if (!sightRendering.type.sight.modeType.isPIP) {
                    if (!OptifineHelper.isShadersEnabled()) {
                        copyMirrorTexture();
                        ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
                        eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model, controller.ADS > 0, worldScale);
                    } else {
                        if (isRenderHand0) {
                            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);

                            GL11.glDepthRange(0, 1);
                            copyMirrorTexture();
                            ClientProxy.scopeUtils.renderPostScope(partialTicks, true, false, true, 1);
                            eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model, controller.ADS > 0, worldScale);
                            writeScopeSoildDepth(controller.ADS > 0);

                            GL11.glPopAttrib();
                        } else {
                            ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
                        }
                    }
                }
            }
        }
        ObjModelRenderer.glowTxtureMode = glowMode;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        GlStateManager.disableBlend();
    }

    public void renderHandAndArmor(EnumHandSide side, AbstractClientPlayer player, EnhancedRenderConfig config,
                                   ModelPlayer modelPlayer, EnhancedModel model) {
        if (side == EnumHandSide.LEFT) {
            if (config.showHandArmorType != ShowHandArmorType.NONE) {
                PreFirstLayer leftFirst = new PreFirstLayer(this, EnumHandSide.LEFT);
                PreSecondLayer leftSecond = new PreSecondLayer(this, EnumHandSide.LEFT);
                MinecraftForge.EVENT_BUS.post(leftFirst);
                MinecraftForge.EVENT_BUS.post(leftSecond);
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    if (!leftFirst.isCanceled()) {
                        if (modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                            model.renderPart("leftArmModel");
                        }
                    }
                    if (!leftSecond.isCanceled()) {
                        if (modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                            model.renderPart("leftArmLayerModel");
                        }
                    }
                } else {
                    if (!leftFirst.isCanceled()) {
                        if (modelPlayer.bipedLeftArm.showModel && !modelPlayer.bipedLeftArm.isHidden) {
                            model.renderPart("leftArmSlimModel");
                        }
                    }
                    if (!leftSecond.isCanceled()) {
                        if (modelPlayer.bipedLeftArmwear.showModel && !modelPlayer.bipedLeftArmwear.isHidden) {
                            model.renderPart("leftArmLayerSlimModel");
                        }
                    }
                }
                if (player.inventory.armorItemInSlot(2) != null) {
                    ItemStack armorStack = player.inventory.armorItemInSlot(2);
                    if (armorStack.getItem() instanceof ItemMWArmor) {
                        int skinId = 0;
                        String path = skinId > 0 ? ((ItemMWArmor) armorStack.getItem()).type.modelSkins[skinId].getSkin()
                                : ((ItemMWArmor) armorStack.getItem()).type.modelSkins[0].getSkin();

                        if (!((ItemMWArmor) armorStack.getItem()).type.simpleArmor) {
                            ModelCustomArmor modelArmor =
                                    ((ModelCustomArmor) ((ItemMWArmor) armorStack.getItem()).type.bipedModel);

                            bindTexture("armor", path);
                            if (modelArmor.enhancedArmModel != null) {
                                modelArmor.enhancedArmModel.loadAnimation(model,
                                        config.showHandArmorType == ShowHandArmorType.SKIN);
                                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                                    if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                        modelArmor.enhancedArmModel.renderPart("leftArmModel");
                                    }
                                    if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                        modelArmor.enhancedArmModel.renderPart("leftArmModel_bone");
                                    }
                                } else {
                                    if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                        modelArmor.enhancedArmModel.renderPart("leftArmSlimModel");
                                    }
                                    if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                        modelArmor.enhancedArmModel.renderPart("leftArmSlimModel_bone");
                                    }
                                }
                            }
                        }
                    }
                }
                MinecraftForge.EVENT_BUS.post(new RenderHandSleeveEnhancedEvent.Post(this, EnumHandSide.LEFT, model));
            } else {
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    model.renderPart(LEFT_HAND_PART);
                } else {
                    model.renderPart(LEFT_SLIM_HAND_PART);
                }
            }
        } else {
            if (config.showHandArmorType != ShowHandArmorType.NONE) {
                PreFirstLayer rightFirst = new PreFirstLayer(this, EnumHandSide.RIGHT);
                PreSecondLayer rightSecond = new PreSecondLayer(this, EnumHandSide.RIGHT);
                MinecraftForge.EVENT_BUS.post(rightFirst);
                MinecraftForge.EVENT_BUS.post(rightSecond);
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    if (!rightFirst.isCanceled()) {
                        if (modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                            model.renderPart("rightArmModel");
                        }
                    }
                    if (!rightSecond.isCanceled()) {
                        if (modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                            model.renderPart("rightArmLayerModel");
                        }
                    }
                } else {
                    if (!rightFirst.isCanceled()) {
                        if (modelPlayer.bipedRightArm.showModel && !modelPlayer.bipedRightArm.isHidden) {
                            model.renderPart("rightArmSlimModel");
                        }
                    }
                    if (!rightSecond.isCanceled()) {
                        if (modelPlayer.bipedRightArmwear.showModel && !modelPlayer.bipedRightArmwear.isHidden) {
                            model.renderPart("rightArmLayerSlimModel");
                        }
                    }
                }
                if (player.inventory.armorItemInSlot(2) != null) {
                    ItemStack armorStack = player.inventory.armorItemInSlot(2);
                    if (armorStack.getItem() instanceof ItemMWArmor) {
                        int skinId = 0;
                        String path = skinId > 0 ? ((ItemMWArmor) armorStack.getItem()).type.modelSkins[skinId].getSkin()
                                : ((ItemMWArmor) armorStack.getItem()).type.modelSkins[0].getSkin();

                        if (!((ItemMWArmor) armorStack.getItem()).type.simpleArmor) {
                            ModelCustomArmor modelArmor =
                                    ((ModelCustomArmor) ((ItemMWArmor) armorStack.getItem()).type.bipedModel);

                            bindTexture("armor", path);
                            if (modelArmor.enhancedArmModel != null) {
                                modelArmor.enhancedArmModel.loadAnimation(model,
                                        config.showHandArmorType == ShowHandArmorType.SKIN);
                                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                                    if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                        modelArmor.enhancedArmModel.renderPart("rightArmModel");
                                    }
                                    if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                        modelArmor.enhancedArmModel.renderPart("rightArmModel_bone");
                                    }
                                } else {
                                    if (config.showHandArmorType == ShowHandArmorType.STATIC) {
                                        modelArmor.enhancedArmModel.renderPart("rightArmSlimModel");
                                    }
                                    if (config.showHandArmorType == ShowHandArmorType.SKIN) {
                                        modelArmor.enhancedArmModel.renderPart("rightArmSlimModel_bone");
                                    }
                                }
                            }
                        }
                    }
                }
                MinecraftForge.EVENT_BUS.post(new RenderHandSleeveEnhancedEvent.Post(this, EnumHandSide.RIGHT, model));
            } else {
                if (!Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                    model.renderPart(RIGHT_HAND_PART);
                } else {
                    model.renderPart(RIGHT_SLIM_HAND_PART);
                }
            }
        }
    }

    public void drawThirdGun(RenderPlayer renderPlayer, RenderType renderType, EntityPlayer player, ItemStack demoStack) {
        if (!(demoStack.getItem() instanceof ItemGun))
            return;
        GunType gunType = ((ItemGun) demoStack.getItem()).type;
        if (gunType == null)
            return;
        EnhancedModel model = gunType.enhancedModel;
        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) model.config;
        AnimationController controller;
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        //这里可以考虑一下 是否可以对骨骼枪做一些优化
        if (player != null) {
            controller = ClientProxy.gunEnhancedRenderer.getController(player, config);
            if (controller.getPlayingAnimation() == AnimationType.DEFAULT
                    || controller.getPlayingAnimation() == AnimationType.PRE_FIRE
                    || controller.getPlayingAnimation() == AnimationType.FIRE
                    || controller.getPlayingAnimation() == AnimationType.POST_FIRE) {
                model.updateAnimation(controller.getTime(), true);
            } else {
                model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS), true);
            }
        } else {
            model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS), true);
        }


        HashSet<String> exceptParts = new HashSet<String>();
        exceptParts.addAll(config.defaultHidePart);
        exceptParts.addAll(config.thirdHidePart);
        exceptParts.removeAll(config.thirdShowPart);
        //exceptParts.addAll(DEFAULT_EXCEPT);

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;
        for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
            ItemStack itemStack = GunType.getAttachment(demoStack, attachment);
            if (itemStack != null && itemStack.getItem() != Items.AIR) {
                AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                String binding = "gunModel";
                if (config.attachmentGroup.containsKey(attachment.typeName)) {
                    if (config.attachmentGroup.get(attachment.typeName).hidePart != null) {
                        exceptParts.addAll(config.attachmentGroup.get(attachment.typeName).hidePart);
                    }
                }
                if (config.attachment.containsKey(attachmentType.internalName)) {
                    if (config.attachment.get(attachmentType.internalName).hidePart != null) {
                        exceptParts.addAll(config.attachment.get(attachmentType.internalName).hidePart);
                    }
                }
            }
        }

        for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
            ItemStack itemStack = GunType.getAttachment(demoStack, attachment);
            if (itemStack != null && itemStack.getItem() != Items.AIR) {
                AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                String binding = "gunModel";
                if (config.attachmentGroup.containsKey(attachment.typeName)) {
                    if (config.attachmentGroup.get(attachment.typeName).showPart != null) {
                        exceptParts.removeAll(config.attachmentGroup.get(attachment.typeName).showPart);
                    }
                }
                if (config.attachment.containsKey(attachmentType.internalName)) {
                    if (config.attachment.get(attachmentType.internalName).showPart != null) {
                        exceptParts.removeAll(config.attachment.get(attachmentType.internalName).showPart);
                    }
                }
            }
        }

        exceptParts.addAll(RenderGunEnhanced.DEFAULT_EXCEPT);

        float worldScale = 1;
        HashSet<String> exceptPartsRendering = exceptParts;

        GlStateManager.pushMatrix();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        ClientProxy.gunEnhancedRenderer.color(1, 1, 1, 1f);

        if (player != null && player.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }

        /*
        if (renderConfigElement.bindding.equals("head")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedHead.postRender(0.0625F);
        } else if (renderConfigElement.bindding.equals("body")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedBody.postRender(0.0625F);
        } else if (renderConfigElement.bindding.equals("rightArm")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedRightArm.postRender(0.0625F);
        } else if (renderConfigElement.bindding.equals("leftArm")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedLeftArm.postRender(0.0625F);
        } else if (renderConfigElement.bindding.equals("rightLeg")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedRightLeg.postRender(0.0625F);
        } else if (renderConfigElement.bindding.equals("leftLeg")) {
            ((ModelBiped) this.renderPlayer.getMainModel()).bipedLeftLeg.postRender(0.0625F);
        }

        GlStateManager.translate(renderConfigElement.pos[0], renderConfigElement.pos[1], renderConfigElement.pos[2]);
        GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
        GlStateManager.scale(renderConfigElement.size[0], renderConfigElement.size[1], renderConfigElement.size[2]);
        GlStateManager.rotate(renderConfigElement.rot[1], 0, -1, 0);
        GlStateManager.rotate(renderConfigElement.rot[0], -1, 0, 0);
        GlStateManager.rotate(renderConfigElement.rot[2], 0, 0, -1);
         */
        if (renderPlayer != null) {
            renderPlayer.getMainModel().bipedRightArm.postRender(0.0625F);
        }
        RenderElement renderConfigElement = config.thirdPerson.renderElements.get(renderType.serializedName);
        GlStateManager.translate(renderConfigElement.pos.x, renderConfigElement.pos.y, renderConfigElement.pos.z);
        GlStateManager.scale(1 / 10f, 1 / 10f, 1 / 10f);
        GlStateManager.scale(renderConfigElement.size.x, renderConfigElement.size.y, renderConfigElement.size.z);
        GlStateManager.rotate(renderConfigElement.rot.y, 0, -1, 0);
        GlStateManager.rotate(renderConfigElement.rot.x, -1, 0, 0);
        GlStateManager.rotate(renderConfigElement.rot.z, 0, 0, -1);

        /**
         * gun
         * */
        int skinId = 0;
        if (demoStack.hasTagCompound()) {
            if (demoStack.getTagCompound().hasKey("skinId")) {
                skinId = demoStack.getTagCompound().getInteger("skinId");
            }
        }
        String gunPath = skinId > 0 ? gunType.modelSkins[skinId].getSkin() : gunType.modelSkins[0].getSkin();
        ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);
        model.renderPartExcept(exceptParts);

        /**
         * ammo and bullet
         * */
        boolean flagDynamicAmmoRendered = false;
        ItemStack stackAmmo = ItemStack.EMPTY;
        ItemStack bulletStack = ItemStack.EMPTY;
        if (demoStack.hasTagCompound()) {
            stackAmmo = new ItemStack(demoStack.getTagCompound().getCompoundTag("ammo"));
        }
        ItemStack orignalAmmo = stackAmmo;
        //stackAmmo=controller.getRenderAmmo(stackAmmo);
        ItemStack renderAmmo = stackAmmo;
        boolean defaultAmmoFlag = true;

        VarBoolean defaultBulletFlag = new VarBoolean();
        defaultBulletFlag.b = true;
        int currentAmmoCount = 0;

        if (gunType.acceptedBullets != null && demoStack.hasTagCompound()) {
            currentAmmoCount = demoStack.getTagCompound().getInteger("ammocount");
            bulletStack = new ItemStack(demoStack.getTagCompound().getCompoundTag("bullet"));
        }

        if (bulletStack != null) {
            if (bulletStack.getItem() instanceof ItemBullet) {
                BulletType bulletType = ((ItemBullet) bulletStack.getItem()).type;
                if (bulletType.isDynamicBullet && bulletType.model != null) {
                    int skinIdBullet = 0;
                    if (bulletStack.hasTagCompound()) {
                        if (bulletStack.getTagCompound().hasKey("skinId")) {
                            skinIdBullet = bulletStack.getTagCompound().getInteger("skinId");
                        }
                    }
                    if (bulletType.sameTextureAsGun) {
                        ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);
                    } else {
                        String pathAmmo = skinIdBullet > 0 ? bulletType.modelSkins[skinIdBullet].getSkin()
                                : bulletType.modelSkins[0].getSkin();
                        ClientProxy.gunEnhancedRenderer.bindTexture("bullets", pathAmmo);
                    }
                    for (int bullet = 0; bullet < currentAmmoCount && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                        int renderBullet = bullet;
                        model.applyGlobalTransformToOther("bulletModel_" + bullet, () -> {
                            ClientProxy.gunEnhancedRenderer.renderAttachment(config, "bullet", bulletType.internalName, () -> {
                                bulletType.model.renderPart("bulletModel", worldScale);
                            });
                        });
                    }
                    model.applyGlobalTransformToOther("bulletModel", () -> {
                        ClientProxy.gunEnhancedRenderer.renderAttachment(config, "bullet", bulletType.internalName, () -> {
                            bulletType.model.renderPart("bulletModel", worldScale);
                        });
                    });
                    defaultBulletFlag.b = false;
                }
            }
        }
        ItemStack[] ammoList = new ItemStack[]{stackAmmo};
        String[] binddings = new String[]{"ammoModel"};
        for (int x = 0; x < 1; x++) {
            ItemStack stackAmmoX = ammoList[x];
            if (stackAmmoX == null || stackAmmoX.isEmpty()) {
                continue;
            }
            if (!model.existPart(binddings[x])) {
                continue;
            }
            if (stackAmmoX.getItem() instanceof ItemAmmo) {
                ItemAmmo itemAmmo = (ItemAmmo) stackAmmoX.getItem();
                AmmoType ammoType = itemAmmo.type;
                if (ammoType.isDynamicAmmo && ammoType.model != null) {
                    int skinIdAmmo = 0;

                    if (ammoType.sameTextureAsGun) {
                        ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);
                    } else {
                        String pathAmmo = skinIdAmmo > 0 ? ammoType.modelSkins[skinIdAmmo].getSkin()
                                : ammoType.modelSkins[0].getSkin();
                        ClientProxy.gunEnhancedRenderer.bindTexture("ammo", pathAmmo);
                    }

                    model.applyGlobalTransformToOther("ammoModel", () -> {
                        GlStateManager.pushMatrix();
                        if (renderAmmo.getTagCompound().hasKey("magcount")) {
                            if (config.attachment.containsKey(itemAmmo.type.internalName)) {
                                if (config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform != null) {
                                    if (renderAmmo.getTagCompound().getInteger("magcount") <= config.attachment
                                            .get(itemAmmo.type.internalName).multiMagazineTransform.size()) {
                                        //be careful, don't mod the config
                                        Transform ammoTransform = config.attachment
                                                .get(itemAmmo.type.internalName).multiMagazineTransform
                                                .get(renderAmmo.getTagCompound().getInteger("magcount") - 1);
                                        Transform renderTransform = ammoTransform;

                                        GlStateManager.translate(renderTransform.translate.x,
                                                renderTransform.translate.y, renderTransform.translate.z);
                                        GlStateManager.scale(renderTransform.scale.x, renderTransform.scale.y,
                                                renderTransform.scale.z);
                                        GlStateManager.rotate(renderTransform.rotate.y, 0, 1, 0);
                                        GlStateManager.rotate(renderTransform.rotate.x, 1, 0, 0);
                                        GlStateManager.rotate(renderTransform.rotate.z, 0, 0, 1);
                                    }
                                }
                            }
                        }
                        ClientProxy.gunEnhancedRenderer.renderAttachment(config, "ammo", ammoType.internalName, () -> {
                            ammoType.model.renderPart("ammoModel", worldScale);
                        });
                        GlStateManager.popMatrix();
                    });
                    flagDynamicAmmoRendered = true;
                    defaultAmmoFlag = false;

                }
            }
        }

        /**
         * default bullet and ammo
         * */

        ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);

        if (defaultBulletFlag.b) {
            for (int bullet = 0; bullet < currentAmmoCount && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                model.renderPart("bulletModel_" + bullet);
            }
        }

        if (!renderAmmo.isEmpty() && defaultAmmoFlag) {
            model.renderPart("ammoModel");
        }

        /**
         * attachment
         * */

        for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
            ItemStack itemStack = GunType.getAttachment(demoStack, attachment);
            if (itemStack != null && itemStack.getItem() != Items.AIR) {
                AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                ModelAttachment attachmentModel = (ModelAttachment) attachmentType.model;

                if (ScopeUtils.isIndsideGunRendering) {
                    if (attachment == AttachmentPresetEnum.Sight) {
                        if (config.attachment.containsKey(attachmentType.internalName)) {
                            if (!config.attachment.get(attachmentType.internalName).renderInsideSightModel) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                if (attachmentModel != null) {
                    String binding = "gunModel";
                    if (config.attachment.containsKey(attachmentType.internalName)) {
                        binding = config.attachment.get(attachmentType.internalName).binding;
                    }
                    model.applyGlobalTransformToOther(binding, () -> {
                        if (attachmentType.sameTextureAsGun) {
                            ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);
                        } else {
                            int attachmentsSkinId = 0;
                            if (itemStack.hasTagCompound()) {
                                if (itemStack.getTagCompound().hasKey("skinId")) {
                                    attachmentsSkinId = itemStack.getTagCompound().getInteger("skinId");
                                }
                            }
                            String attachmentsPath = attachmentsSkinId > 0
                                    ? attachmentType.modelSkins[attachmentsSkinId].getSkin()
                                    : attachmentType.modelSkins[0].getSkin();
                            ClientProxy.gunEnhancedRenderer.bindTexture("attachments", attachmentsPath);
                        }
                        ClientProxy.gunEnhancedRenderer.renderAttachment(config, attachment.typeName,
                                attachmentType.internalName, () -> {
                                    attachmentModel.renderAttachment(worldScale);
                                    if (attachment == AttachmentPresetEnum.Sight) {
                                        ObjModelRenderer.glowTxtureMode = false;
                                        ClientProxy.gunEnhancedRenderer.renderScopeGlass(attachmentType,
                                                attachmentModel, false, worldScale);
                                        ObjModelRenderer.glowTxtureMode = true;
                                    }
                                });
                    });
                }
            }
        }

        ObjModelRenderer.glowTxtureMode = glowTxtureMode;

        /**
         *  flashmodel
         *  */
        boolean shouldRenderFlash = true;
        if ((GunType.getAttachment(demoStack, AttachmentPresetEnum.Barrel) != null)) {
            AttachmentType attachmentType = ((ItemAttachment) GunType.getAttachment(demoStack, AttachmentPresetEnum.Barrel).getItem()).type;
            if (attachmentType.attachmentType == AttachmentPresetEnum.Barrel) {
                shouldRenderFlash = !attachmentType.barrel.hideFlash;
            }
        }

        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;

        if (shouldRenderFlash && anim.shooting && anim.getShootingAnimationType() == AnimationType.FIRE && !player.isInWater()) {
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            TextureType flashType = gunType.flashType;
            bindTexture(flashType.resourceLocations.get(anim.flashCount % flashType.resourceLocations.size()));
            model.renderPart("flashModel");
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
            GlStateManager.enableLighting();
        }

        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    public void writeScopeGlassDepth(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming, float worldScale, boolean mask) {
        if (ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if (Minecraft.getMinecraft().world != null) {
            if (isAiming) {
                GlStateManager.colorMask(mask, mask, mask, mask);
                renderWorldOntoScope(attachmentType, modelAttachment, worldScale, false);
                GlStateManager.colorMask(true, true, true, true);
            }
        }

    }

    /**
     * 将blurFramebuffer图案保存到SCOPE_MASK_TEX
     */
    public void copyMirrorTexture() {
        if (ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if (!OptifineHelper.isShadersEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GL43.glCopyImageSubData(ClientProxy.scopeUtils.blurFramebuffer.framebufferTexture, GL_TEXTURE_2D, 0, 0, 0, 0, ScopeUtils.SCOPE_MASK_TEX, GL_TEXTURE_2D, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight, 1);
    }

    /**
     * 以SCOPE_MASK_TEX为遮罩  将深度改为cofnig.eraseScopeDepth
     ***/
    @SideOnly(Side.CLIENT)
    public void eraseScopeGlassDepth(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming, float worldScale) {
        if (ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if (!OptifineHelper.isShadersEnabled()) {
            return;
        }
        if (Minecraft.getMinecraft().world != null) {
            if (isAiming) {
                GlStateManager.colorMask(false, false, false, false);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                ClientProxy.scopeUtils.setupOverlayRendering();
                ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());

                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.pushProgram();
                    Shaders.useProgram(Shaders.ProgramNone);
                }

                GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);

                GL11.glDepthRange(ModConfig.INSTANCE.hud.eraseScopeDepth, ModConfig.INSTANCE.hud.eraseScopeDepth);
                GlStateManager.alphaFunc(GL11.GL_GREATER, 0f);
                GlStateManager.depthFunc(GL11.GL_ALWAYS);
                GlStateManager.bindTexture(ScopeUtils.SCOPE_MASK_TEX);
                ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(), resolution.getScaledHeight(), 1, 1);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1f);
                GL11.glPopAttrib();

                if (ScopeUtils.isRenderHand0) {
                    GL20.glUseProgram(Programs.depthProgram);
                    GlStateManager.bindTexture(ClientProxy.scopeUtils.DEPTH_ERASE_TEX);
                    ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(), resolution.getScaledHeight(), 1, 1);
                    GL20.glUseProgram(0);
                }

                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.popProgram();
                }

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                GlStateManager.colorMask(true, true, true, true);
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public void writeScopeSoildDepth(boolean isAiming) {
        if (ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if (!OptifineHelper.isShadersEnabled()) {
            return;
        }
        if (Minecraft.getMinecraft().world != null) {
            if (isAiming) {
                GlStateManager.colorMask(false, false, false, false);
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.pushMatrix();
                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.pushMatrix();
                ClientProxy.scopeUtils.setupOverlayRendering();
                ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());

                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.pushProgram();
                    Shaders.useProgram(Shaders.ProgramNone);
                }

                GL20.glUseProgram(Programs.alphaDepthProgram);
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE3);
                int tex3 = GlStateManager.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GlStateManager.bindTexture(ClientProxy.scopeUtils.blurFramebuffer.framebufferTexture);
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
                GlStateManager.bindTexture(ClientProxy.scopeUtils.DEPTH_TEX);
                ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(), resolution.getScaledHeight(), 1, 1);
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE3);
                GlStateManager.bindTexture(tex3);
                GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
                GL20.glUseProgram(0);

                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.popProgram();
                }

                GlStateManager.matrixMode(GL11.GL_PROJECTION);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.popMatrix();
                GlStateManager.colorMask(true, true, true, true);
            }
        }

    }

    @SideOnly(Side.CLIENT)
    public void renderScopeGlass(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming, float worldScale) {
        if (ScopeUtils.isIndsideGunRendering) {
            return;
        }
        if (Minecraft.getMinecraft().world != null) {
            if (isAiming) {
                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.pushProgram();
                    Shaders.useProgram(Shaders.ProgramNone);
                }

                Minecraft mc = Minecraft.getMinecraft();
                float alpha = 1 - adsSwitch;

                if (alpha > 0.2) {
                    alpha = 1;
                } else {
                    alpha /= 0.2f;
                }
                GL20.glUseProgram(Programs.normalProgram);
                GL11.glPushMatrix();
                int tex = ClientProxy.scopeUtils.blurFramebuffer.framebufferTexture;

                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);

                GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, ScopeUtils.OVERLAY_TEX, 0);
                GlStateManager.clearColor(0, 0, 0, 0);
                GL11.glClearColor(0, 0, 0, 0);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.depthMask(true);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                copyDepthBuffer();
                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

                //GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                modelAttachment.renderOverlaySolid(worldScale);

                GL20.glUseProgram(0);
                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.popProgram();
                }

                GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
                GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
                if (attachmentType.sight.usedDefaultOverlayModelTexture) {
                    renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/black.png"));
                }
                //必要的colormask(2023.3.26又注:今天看起来是莫名其妙)
                GlStateManager.colorMask(true, true, true, true);
                modelAttachment.renderOverlay(worldScale);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.disableBlend();
                //GlStateManager.enableLighting();


                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, tex, 0);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                copyDepthBuffer();
                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);


                GlStateManager.colorMask(true, true, true, false);
                GlStateManager.disableBlend();
                //忘记这玩意有什么用了 好像和镜面的光照渲染有关系
                renderWorldOntoScope(attachmentType, modelAttachment, worldScale, false);
                GlStateManager.enableBlend();
                GlStateManager.colorMask(true, true, true, true);

                ContextCapabilities contextCapabilities = GLContext.getCapabilities();
                if (contextCapabilities.OpenGL43) {
                    GL43.glCopyImageSubData(tex, GL_TEXTURE_2D, 0, 0, 0, 0, ScopeUtils.SCOPE_LIGHTMAP_TEX, GL_TEXTURE_2D, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight, 1);

                } else {
                    GL11.glBindTexture(GL_TEXTURE_2D, tex);
                    GL11.glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight);
                }
                OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
                GL11.glPopMatrix();

            } else {
                GL11.glPushMatrix();
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                if (attachmentType.sight.usedDefaultOverlayModelTexture) {
                    renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/black.png"));
                }
                modelAttachment.renderOverlay(worldScale);
                GL11.glPopMatrix();
            }
        }
    }

    /**
     * 把世界深度写入blurFramebuffer
     */
    public void copyDepthBuffer() {
        Minecraft mc = Minecraft.getMinecraft();
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, OptifineHelper.getDrawFrameBuffer());
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, ClientProxy.scopeUtils.blurFramebuffer.framebufferObject);
        GlStateManager.colorMask(false, false, false, false);
        GL30.glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight, GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager.colorMask(true, true, true, true);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, GL11.GL_NONE);
    }

    @SideOnly(Side.CLIENT)
    private void renderWorldOntoScope(AttachmentType type, ModelAttachment modelAttachment, float worldScale, boolean isLightOn) {
        GL11.glPushMatrix();

        if (isLightOn) {
            renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/white.png"));
            GL11.glDisable(2896);
            Minecraft.getMinecraft().entityRenderer.disableLightmap();
            //ModelGun.glowOn(1);
            modelAttachment.renderScope(worldScale);
            //ModelGun.glowOff();
            GL11.glEnable(2896);
            Minecraft.getMinecraft().entityRenderer.enableLightmap();
        } else {
            if (debug) {
                renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/black.png"));
            } else {
                renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/white.png"));
            }
            modelAttachment.renderScope(worldScale);
        }
        /*
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) != null && mc.gameSettings.thirdPersonView == 0) {
            if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                final ItemStack gunStack = mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Flashlight) != null) {
                    if (isLightOn) {
                        GL11.glDisable(2896);
                        Minecraft.getMinecraft().entityRenderer.disableLightmap();
                        GL11.glDisable(3042);
                        GL11.glPushMatrix();
                        GL11.glPushAttrib(16384);
                        GL11.glEnable(3042);
                        GL11.glDepthMask(false);
                        GL11.glBlendFunc(774, 770);

                        renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/gui/light.png"));
                        modelAttachment.renderOverlay(worldScale);

                        GL11.glBlendFunc(770, 771);
                        GL11.glDepthMask(true);
                        GL11.glDisable(3042);
                        GL11.glPopAttrib();
                        GL11.glPopMatrix();
                        GL11.glEnable(2896);
                        Minecraft.getMinecraft().entityRenderer.enableLightmap();
                    }
                }
            }
        }
        */
        GL11.glPopMatrix();
    }

    public void renderAttachment(GunEnhancedRenderConfig config, String type, String name, Runnable run) {
        if (config.attachmentGroup.containsKey(type)) {
            applyTransform(config.attachmentGroup.get(type));
        }
        if (config.attachment.containsKey(name)) {
            applyTransform(config.attachment.get(name));
        }
        run.run();
    }

    public void applyTransform(Transform transform) {
        GlStateManager.translate(transform.translate.x, transform.translate.y, transform.translate.z);
        GlStateManager.scale(transform.scale.x, transform.scale.y, transform.scale.z);
        GlStateManager.rotate(transform.rotate.y, 0, 1, 0);
        GlStateManager.rotate(transform.rotate.x, 1, 0, 0);
        GlStateManager.rotate(transform.rotate.z, 0, 0, 1);
    }

    public void blendTransform(ModelEnhancedGun model, ItemStack gunStack, boolean basicSprint, float time, float sprintTime,
                               float alpha, String hand, boolean applySprint, boolean skin, Runnable runnable) {
        float ammoPer = 0;
        if (gunStack.getTagCompound() != null) {
            if (ItemGun.hasAmmoLoaded(gunStack)) {
                ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
                if (ammoStack.getTagCompound() != null && ammoStack.getItem() instanceof ItemAmmo) {

                    ItemAmmo itemAmmo = (ItemAmmo) ammoStack.getItem();
                    Integer currentMagcount = null;
                    if (ammoStack.getTagCompound().hasKey("magcount")) {
                        currentMagcount = ammoStack.getTagCompound().getInteger("magcount");
                    }
                    int currentAmmoCount = ReloadHelper.getBulletOnMag(ammoStack, currentMagcount);
                    ammoPer = currentAmmoCount / (float) itemAmmo.type.ammoCapacity;
                }
            }
            if (ItemGun.getUsedBullet(gunStack, ((ItemGun) (gunStack.getItem())).type) != null) {

            }
        }
        float ammoPerParam = ammoPer;

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
                            if (!node.name.equals("root") && !node.name.equals("sprint_lefthand")
                                    && !node.name.equals("sprint_righthand") && !node.name.equals("root_bone")
                                    && !node.name.equals("sprint_lefthand_bone") && !node.name.equals("sprint_righthand_bone")) {
                                break sprint;
                            }
                            Quaternionf quat = new Quaternionf();
                            quat.setFromUnnormalized(begin_transform);
                            quat.normalize().slerp(end_transform.rot.normalize(), alpha);
                            org.joml.Vector3f pos = new org.joml.Vector3f();
                            begin_transform.getTranslation(pos);
                            pos.set(pos.x + (end_transform.pos.x - pos.x) * alpha, pos.y + (end_transform.pos.y - pos.y) * alpha,
                                    pos.z + (end_transform.pos.z - pos.z) * alpha);
                            org.joml.Vector3f size = new org.joml.Vector3f();
                            begin_transform.getScale(size);
                            size.set(size.x + (end_transform.size.x - size.x) * alpha,
                                    size.y + (end_transform.size.y - size.y) * alpha, size.z + (end_transform.size.z - size.z) * alpha);
                            mat.identity();
                            mat.translate(pos);
                            mat.scale(size);
                            mat.rotate(quat);
                        }
                    }
                }
                ObjectControl cfg = ((GunEnhancedRenderConfig) model.config).objectControl.get(node.name);
                if (cfg != null) {
                    float per = ammoPerParam;
                    if (!cfg.progress) {
                        per = 1 - per;
                    }
                    //System.out.println(per);
                    mat.translate(cfg.translate.x * per, cfg.translate.y * per, cfg.translate.z * per);
                    mat.rotate(cfg.rotate.y * per * 3.14f / 180, 0, 1, 0);
                    mat.rotate(cfg.rotate.x * per * 3.14f / 180, 1, 0, 0);
                    mat.rotate(cfg.rotate.z * per * 3.14f / 180, 0, 0, 1);
                }
            }
        });
        model.updateAnimation(time, skin);
        runnable.run();
        model.setAnimationCalBlender(null);
    }

    public org.joml.Matrix4f getGlobalTransform(EnhancedModel model, String name) {
        return model.getGlobalTransform(name);
    }

    private Matrix3f genMatrixFromQuaternion(Quaternion quaternion) {
        Matrix3f matrix3f = new Matrix3f();
        matrix3f.m00 = 1 - 2 * quaternion.y * quaternion.y - 2 * quaternion.z * quaternion.z;
        matrix3f.m01 = 2 * quaternion.x * quaternion.y + 2 * quaternion.w * quaternion.z;
        matrix3f.m02 = 2 * quaternion.x * quaternion.z - 2 * quaternion.w * quaternion.y;

        matrix3f.m10 = 2 * quaternion.x * quaternion.y - 2 * quaternion.w * quaternion.z;
        matrix3f.m11 = 1 - 2 * quaternion.x * quaternion.x - 2 * quaternion.z * quaternion.z;
        matrix3f.m12 = 2 * quaternion.y * quaternion.z + 2 * quaternion.w * quaternion.x;

        matrix3f.m20 = 2 * quaternion.x * quaternion.z + 2 * quaternion.w * quaternion.y;
        matrix3f.m21 = 2 * quaternion.y * quaternion.z - 2 * quaternion.w * quaternion.x;
        matrix3f.m22 = 1 - 2 * quaternion.x * quaternion.x - 2 * quaternion.y * quaternion.y;
        return matrix3f;
    }

    //4x4 floats
    @Deprecated
    private void genMatrix(Matrix3f m, float[] floats) {
        m.m00 = floats[0];
        m.m01 = floats[4];
        m.m02 = floats[8];

        m.m10 = floats[1];
        m.m11 = floats[5];
        m.m12 = floats[9];

        m.m20 = floats[2];
        m.m21 = floats[6];
        m.m22 = floats[10];
    }

    public boolean onGltfRenderCallback(String part) {
        return false;
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
//        String pathFormat = "skins/%s/%s.png";
//        bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, String.format(pathFormat, type, fileName)));
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
