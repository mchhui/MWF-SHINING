package com.modularwarfare.client.fpp.enhanced.renderers;

import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.animation.melee.AnimationMeleeController;
import com.modularwarfare.client.fpp.enhanced.configs.GrenadeEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.MeleeRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ThirdPerson.RenderElement;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGrenade;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.loader.api.model.ObjModelRenderer;

import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Timer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;


import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
//import org.lwjgl.util.vector.Matrix4f;
//import org.lwjgl.util.vector.Vector3f;

import org.joml.AxisAngle4d;
import org.joml.AxisAngle4f;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Quaternion;


import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.*;

public class RenderMelee extends CustomItemRenderer {

    public static final float PI = 3.14159265f;
    private static final String[] LEFT_HAND_PART = new String[] {"leftArmModel", "leftArmLayerModel"};
    private static final String[] LEFT_SLIM_HAND_PART = new String[] {"leftArmSlimModel", "leftArmLayerSlimModel"};
    private static final String[] RIGHT_HAND_PART = new String[] {"rightArmModel", "rightArmLayerModel"};
    private static final String[] RIGHT_SLIM_HAND_PART = new String[] {"rightArmSlimModel", "rightArmLayerSlimModel"};
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    public static final List<String> defaultHideList = Arrays.asList("leftArmModel", "leftArmLayerModel", "leftArmSlimModel", "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel", "rightArmLayerSlimModel", "sprint_righthand", "sprint_lefthand");
    static {
        for (String str : defaultHideList) {
            DEFAULT_EXCEPT.add(str);
        }
    }
    public static AnimationMeleeController controller;
    public FloatBuffer floatBuffer = BufferUtils.createFloatBuffer(16);
    private Timer timer;

    public static Vector3f mwf_camera_pos = new Vector3f();
    public static AxisAngle4d mwf_camera_rot = new AxisAngle4d();

    float prev_f1;

    public static float toRadians(float angdeg) {
        return angdeg / 180.0f * PI;
    }

    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        if (!(item.getItem() instanceof ItemMelee))
            return;

        MeleeType meleeType = ((ItemMelee) item.getItem()).type;
        if (meleeType == null)
            return;

        if (this.timer == null) {
            this.timer = ReflectionHelper.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "timer",
                    "field_71428_T");
        }
        float partialTicks = this.timer.renderPartialTicks;

        EnhancedModel model = meleeType.enhancedModel;

        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager()
                .getEntityRenderObject(Minecraft.getMinecraft().player);
        RenderPlayer renderplayer = (RenderPlayer) render;
        ModelPlayer modelPlayer = renderplayer.getMainModel();
        ClientProxy.renderHooks.hidePlayerModel((AbstractClientPlayer) Minecraft.getMinecraft().getRenderViewEntity(),
                renderplayer);

        MeleeRenderConfig config = (MeleeRenderConfig) model.config;
        if (this.controller == null || this.controller.getConfig() != config) {
            this.controller = new AnimationMeleeController(config, meleeType);
        }
        
        HashSet<String> renderSetExpect=new HashSet<>(DEFAULT_EXCEPT);
        config.defaultHidePart.forEach((part)->{
            renderSetExpect.add(part);
        });
        
        if (type.equals(CustomItemRenderType.EQUIPPED_FIRST_PERSON)) {

            EntityPlayerSP player = (EntityPlayerSP) Minecraft.getMinecraft().getRenderViewEntity();

            Matrix4f mat = new Matrix4f();

            GlStateManager.pushMatrix();
            {
                boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
                ObjModelRenderer.glowTxtureMode = true;

                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.loadIdentity();

                /**
                 * DEFAULT TRANSFORM
                 */
                // mat.translate(new Vector3f(0,1.3f,-1.8f));
                float zFar = Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16F * 2;
                mat.rotate(toRadians(90.0F), new Vector3f(0, 1, 0));
                mat.scale(new Vector3f(1 / zFar, 1 / zFar, 1 / zFar));
                // Do hand rotations
                float f5 = player.prevRenderArmPitch
                        + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
                float f6 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
                mat.rotate(toRadians((player.rotationPitch - f5) * 0.1F), new Vector3f(1, 0, 0));
                mat.rotate(toRadians((player.rotationYaw - f6) * 0.1F), new Vector3f(0, 1, 0));

                mat.rotate(toRadians(90), new Vector3f(0, 1, 0));
                mat.translate(new Vector3f(config.global.globalTranslate.x, config.global.globalTranslate.y,
                        config.global.globalTranslate.z));
                mat.rotate(toRadians(-90), new Vector3f(0, 1, 0));
                mat.rotate(config.global.globalRotate.y / 180 * 3.14f, new Vector3f(0, 1, 0));
                mat.rotate(config.global.globalRotate.x / 180 * 3.14f, new Vector3f(1, 0, 0));
                mat.rotate(config.global.globalRotate.z / 180 * 3.14f, new Vector3f(0, 0, 1));

                /**
                 * ACTION GUN MOTION
                 */
                float gunRotX = RenderParameters.GUN_ROT_X_LAST
                        + (RenderParameters.GUN_ROT_X - RenderParameters.GUN_ROT_X_LAST)
                                * ClientProxy.renderHooks.partialTicks;
                float gunRotY = RenderParameters.GUN_ROT_Y_LAST
                        + (RenderParameters.GUN_ROT_Y - RenderParameters.GUN_ROT_Y_LAST)
                                * ClientProxy.renderHooks.partialTicks;
                mat.rotate(toRadians(gunRotX), new Vector3f(0, -1, 0));
                mat.rotate(toRadians(gunRotY), new Vector3f(0, 0, -1));

                /**
                 * ACTION GUN BALANCING X / Y
                 */
                float rotateX = 0;
                mat.translate(new Vector3f(
                        (float) (0.1f * GUN_BALANCING_X * Math.cos(Math.PI * RenderParameters.SMOOTH_SWING / 50)), 0,
                        0));
                rotateX -= (GUN_BALANCING_X * 4F)
                        + (float) (GUN_BALANCING_X * Math.sin(Math.PI * RenderParameters.SMOOTH_SWING / 35));
                rotateX -= (float) Math.sin(Math.PI * GUN_BALANCING_X);
                rotateX -= (GUN_BALANCING_X) * 0.4F;
                /**
                 * ACTION PROBE
                 */
                if (Loader.isModLoaded("modularmovements")) {
                    rotateX += 15F * ClientListener.cameraProbeOffset;
                }
                mat.rotate(toRadians(rotateX), new Vector3f(1f, 0f, 0f));

                /**
                 * ACTION SPRINT
                 */
                RenderParameters.VALSPRINT = (float) (Math.cos(controller.SPRINT_RANDOM*2*Math.PI)) * meleeType.moveSpeedModifier;
                RenderParameters.VALSPRINT2 = (float)(Math.sin(controller.SPRINT_RANDOM*2*Math.PI)) * meleeType.moveSpeedModifier;

                Vector3f customSprintRotation = new Vector3f();
                Vector3f customSprintTranslate = new Vector3f();

                customSprintRotation = new org.joml.Vector3f((config.extra.sprintRotation.x * (float) controller.SPRINT), (config.extra.sprintRotation.y * (float) controller.SPRINT), (config.extra.sprintRotation.z * (float) controller.SPRINT));
                customSprintTranslate = new org.joml.Vector3f((config.extra.sprintOffset.x * (float) controller.SPRINT), (config.extra.sprintOffset.y * (float) controller.SPRINT), (config.extra.sprintOffset.z * (float) controller.SPRINT));

                customSprintRotation.mul(1F - (float) controller.INSPECT);
                customSprintTranslate.mul(1F - (float) controller.INSPECT);

                // Custom view bobbing applies to gun models
                EntityPlayer entityplayer = (EntityPlayer) Minecraft.getMinecraft().getRenderViewEntity();
                float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;

                float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
                float f1_1 = prev_f1 + ((f1) - prev_f1) * partialTicks;
                prev_f1 = f1;

                float f2 = entityplayer.prevCameraYaw
                        + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
                float f3 = entityplayer.prevCameraPitch
                        + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
                GlStateManager.translate(
                        config.extra.bobbingFactor * 0.05f * MathHelper.sin(f1_1 * (float) Math.PI) * f2 * 0.5F,
                        config.extra.bobbingFactor * 0.05f * -Math.abs(MathHelper.cos(f1_1 * (float) Math.PI) * f2),
                        0.0F);
                GlStateManager.rotate(MathHelper.sin(f1_1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(Math.abs(MathHelper.cos(f1_1 * (float) Math.PI - 0.2F) * f2) * 5.0F, 1.0F, 0.0F,
                        0.0F);
                GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);

                //镜头动画
                model.updateAnimation(controller.getTime());

                if(model.model.geoModel.nodes.get("mwf_camera")!=null) {
                    Matrix4f cameraMat=model.getGlobalTransform("mwf_camera");
                    AxisAngle4d cam_aa=mwf_camera_rot;
                    Vector3f cam_pos=mwf_camera_pos;
                    Vector3f cam_begin_pos=model.model.geoModel.nodes.get("mwf_camera").pos;
                    cameraMat.getRotation(cam_aa);
                    cameraMat.getTranslation(cam_pos);
                    cam_pos.sub(cam_begin_pos).negate();
                    mat.translate(cam_pos);
                    mat.rotate((float)cam_aa.angle,(float)-cam_aa.x,(float)-cam_aa.y,(float)-cam_aa.z);
                }else {
                    mwf_camera_pos.set(0,0,0);
                    mwf_camera_rot.set(0, 0, 0, 0);
                }

                floatBuffer.clear();
                mat.get(floatBuffer);
                floatBuffer.rewind();

                GL11.glMultMatrix(floatBuffer);

                //model.updateAnimation(controller.getTime());

                /**
                 * player right hand
                 */
                ObjModelRenderer.glowTxtureMode = false;
                bindPlayerSkin();
                // if (Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                // model.renderPart(RIGHT_SLIM_HAND_PART);
                // } else {
                // model.renderPart(RIGHT_HAND_PART);
                // }
                blendTransform(model, item, !config.meleeAnimations.containsKey(AnimationMeleeType.SPRINT),
                    controller.getTime(), controller.getSprintTime(), (float)controller.SPRINT,
                    "sprint_righthand", true, true, () -> {
                    ObjModelRenderer.glowTxtureMode = false;
                    bindPlayerSkin();
                    ClientProxy.gunEnhancedRenderer.renderHandAndArmor(EnumHandSide.RIGHT, player, config, modelPlayer, model);
                    ObjModelRenderer.glowTxtureMode = true;
                });

                /**
                 * gun
                 */
                int skinId = 0;
                if (item.hasTagCompound()) {
                    if (item.getTagCompound().hasKey("skinId")) {
                        skinId = item.getTagCompound().getInteger("skinId");
                    }
                }
                String meleePath = skinId > 0 ? meleeType.modelSkins[skinId].getSkin()
                        : meleeType.modelSkins[0].getSkin();
                bindTexture("melee", meleePath);
                model.renderPartExcept(renderSetExpect);

                /**
                 * player left hand
                 */
                ObjModelRenderer.glowTxtureMode = false;
                bindPlayerSkin();
                // if (Minecraft.getMinecraft().player.getSkinType().equals("slim")) {
                // model.renderPart(LEFT_SLIM_HAND_PART);
                // } else {
                // model.renderPart(LEFT_HAND_PART);
                // }
                blendTransform(model, item, !config.meleeAnimations.containsKey(AnimationMeleeType.SPRINT),
                    controller.getTime(), controller.getSprintTime(), (float)controller.SPRINT,
                    "sprint_lefthand", true, true, () -> {
                    ObjModelRenderer.glowTxtureMode = false;
                    bindPlayerSkin();
                    ClientProxy.gunEnhancedRenderer.renderHandAndArmor(EnumHandSide.LEFT, player, config, modelPlayer, model);
                    ObjModelRenderer.glowTxtureMode = true;
                });

                ObjModelRenderer.glowTxtureMode = glowTxtureMode;
            }
            GlStateManager.popMatrix();
        }
    }
    
    public void drawThirdMelee(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player, ItemStack demoStack, boolean sneakFlag) {
        if (!(demoStack.getItem() instanceof ItemMelee)) {
            return;
        }
        BaseType type = ((BaseItem)demoStack.getItem()).baseType;
        if (!type.hasModel()) {
            return;
        }

        EnhancedModel model = type.enhancedModel;
        MeleeRenderConfig config = (MeleeRenderConfig)type.enhancedModel.config;
        
        HashSet<String> renderSetExpect=new HashSet<>(DEFAULT_EXCEPT);
        config.defaultHidePart.forEach((part)->{
            renderSetExpect.add(part);
        });
        config.thirdHidePart.forEach((part)->{
            renderSetExpect.add(part);
        });
        config.thirdShowPart.forEach((part)->{
            renderSetExpect.remove(part);
        });
        
        GlStateManager.pushMatrix();

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
        
        model.updateAnimation((float)config.meleeAnimations.get(AnimationMeleeType.DEFAULT).get(0).getStartTime(config.FPS));

        int skinId = 0;
        if (demoStack.hasTagCompound()) {
            if (demoStack.getTagCompound().hasKey("skinId")) {
                skinId = demoStack.getTagCompound().getInteger("skinId");
            }
        }
        String path = skinId > 0 ? type.modelSkins[skinId].getSkin() : type.modelSkins[0].getSkin();
        RenderMelee meleeRender = ClientProxy.meleeRenderer;
        meleeRender.bindTexture("melee", path);

        boolean glowTxtureMode = ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode = true;

        model.renderPartExcept(RenderParameters.partsWithAmmo);

        ObjModelRenderer.glowTxtureMode = glowTxtureMode;
        GlStateManager.popMatrix();
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

    public void blendTransform(EnhancedModel model, ItemStack gunStack, boolean basicSprint, float time,
            float sprintTime,
            float alpha, String hand, boolean applySprint, boolean skin, Runnable runnable) {

        model.setAnimationCalBlender(new NodeAnimationBlender("FirstPersonBlender") {

            @Override
            public void handle(DataNode node, org.joml.Matrix4f mat) {
                if (!basicSprint) {
                    if (alpha != 0) {
                        sprint: {
                            org.joml.Matrix4f begin_transform = mat;
                            mchhui.hegltf.DataAnimation.Transform end_transform = model.findLocalTransform(node.name,
                                    sprintTime);
                            if (end_transform == null) {
                                break sprint;
                            }
                            if (!node.name.equals("root") && !node.name.equals("sprint_lefthand")
                                    && !node.name.equals("sprint_righthand") && !node.name.equals("root_bone")
                                    && !node.name.equals("sprint_lefthand_bone")
                                    && !node.name.equals("sprint_righthand_bone")
                                    && !node.name.equals("meleeModel")
                                    && !node.name.equals("meleeModel_bone")
                                    && !node.name.endsWith("_sprint")) {
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
            }
        });
        model.updateAnimation(time, skin);
        runnable.run();
        model.setAnimationCalBlender(null);
    }
}
