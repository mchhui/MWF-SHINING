package com.modularwarfare.client.fpp.enhanced.renderers;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreFirstLayer;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent.PreSecondLayer;
import com.modularwarfare.api.RenderHandSleeveEnhancedEvent;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.model.ModelAttachment;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.client.objloader.MWModelBase;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;
import com.modularwarfare.client.fpp.basic.configs.AttachmentRenderConfig;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderType;
import com.modularwarfare.client.fpp.basic.models.objects.CustomItemRenderer;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.animation.AnimationController;
import com.modularwarfare.client.fpp.enhanced.animation.EnhancedStateMachine;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.Attachment;
import com.modularwarfare.client.fpp.enhanced.configs.GunEnhancedRenderConfig.SpecialEffect.EjectionGroup;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ObjectControl;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ShowHandArmorType;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.Transform;
import com.modularwarfare.client.fpp.enhanced.configs.EnhancedRenderConfig.ThirdPerson.RenderElement;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import com.modularwarfare.client.fpp.enhanced.models.EnhancedModel;
import com.modularwarfare.client.fpp.enhanced.models.ModelEnhancedGun;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.client.laser.LaserRenderManager;
import com.modularwarfare.client.scope.ScopeUtils;
import com.modularwarfare.client.shader.Programs;
import com.modularwarfare.common.armor.ItemMWArmor;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.data.VarBoolean;
import com.modularwarfare.common.textures.TextureType;
import com.modularwarfare.utility.OptifineHelper;
import com.modularwarfare.utility.ReloadHelper;
import com.modularwarfare.utility.maths.Interpolation;

import mchhui.hegltf.DataNode;
import mchhui.hegltf.GltfRenderModel.NodeAnimationBlender;
import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
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

import org.joml.AxisAngle4d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.util.vector.Quaternion;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static com.modularwarfare.client.fpp.basic.renderers.RenderParameters.*;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;

public class RenderGunEnhanced extends CustomItemRendererEnhanced {
    public static float diversion = 64f;
    public static float[] es_joint = new float[(int) (diversion + 1)];
    public static float[] strafing_joint = new float[(int) (diversion + 1)];
    public static float[] forward_joint = new float[(int) (diversion + 1)];
    public static float postSmokeTp = 0;
    public static float postSmokeWind = 1;
    public static float postSmokeAlpha = 1;
    public static float ejectionTp = 0;

    public static long shellTime = 0;
    public static ShellEffect[] shellEffects = new ShellEffect[16];
    private static MWModelBase shellModel;
    private static String shellTexType;
    private static String shellTexPath;

    public static Vector3f mwf_camera_pos = new Vector3f();
    public static AxisAngle4d mwf_camera_rot = new AxisAngle4d();

    public static boolean debug = false;
    public static boolean debug1 = false;

    private ShortBuffer pixelBuffer = null;
    private int lastWidth;
    private int lastHeight;

    

    // 后坐力参数
    private static final float RECOIL_COMPLETE_TIME = 0.02f;
    private static final float RECOIL_RECOVER_TIME = 0.08f; 
    private static final float RECOIL_FAST_RECOVER_THRESHOLD = 3.0f;
    private static final float RECOIL_FAST_RECOVER_FACTOR = 2.5f;
    private static final float RECOIL_BOUNCE_FACTOR = 0.4f;
    private static final float ADS_RECOIL_FACTOR = 0.2f;
    private static final float MAX_TIME_DELTA = 0.1f;
    private static final float BOUNCE_FREQUENCY = 0.08f;
    
    private static float currentRecoilX = 0f;
    private static float currentRecoilY = 0f;
    private static float currentRecoilZ = 0f;
    private static float lastRecoilX = 0f;
    private static float lastRecoilY = 0f;
    private static float lastRecoilZ = 0f;
    private static float targetRecoilX = 0f;
    private static float targetRecoilY = 0f;
    private static float targetRecoilZ = 0f;
    private static float bounceRecoilX = 0f;
    private static float bounceRecoilY = 0f;
    private static float bounceRecoilZ = 0f;
    private static long lastRecoilTime = 0L;
    private static boolean isRecovering = false;

    private static FloatBuffer lightBuf = BufferUtils.createFloatBuffer(4);

    private boolean renderingMagazine = true;

    private float linearInterpolation(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }

    public static final int BULLET_MAX_RENDER = 256;
    private static float theata90 = (float) Math.toRadians(90);
    public static final HashSet<String> DEFAULT_EXCEPT = new HashSet<String>();
    public static final List<String> defaultHideList = Arrays.asList("ammoModel", "leftArmModel", "leftArmLayerModel",
            "leftArmSlimModel",
            "leftArmLayerSlimModel", "rightArmModel", "rightArmLayerModel", "rightArmSlimModel",
            "rightArmLayerSlimModel", "flashModel", "smokeModel", "sprint_righthand", "sprint_lefthand",
            "selector_semi", "selector_full", "selector_brust", "bulletModel", "shellEffect", "panelModel",
            "translucentModel");
    static {
        for (String str : defaultHideList) {
            DEFAULT_EXCEPT.add(str);
        }
        for (int i = 0; i < BULLET_MAX_RENDER; i++) {
            DEFAULT_EXCEPT.add("bulletModel_" + i);
        }
        for (int i = 0; i < BULLET_MAX_RENDER; i++) {
            DEFAULT_EXCEPT.add("shellModel_" + i);
        }
    }

    public static class ShellEffect {
        public String bindding;
        public Matrix4f mat = null;
        public Vector3f pos = new Vector3f();
        public Vector3f vec = new Vector3f();
        public Vector3f rot = new Vector3f();
    }

    public void renderItem(CustomItemRenderType type, EnumHand hand, ItemStack item, Object... data) {
        EntityPlayerSP player = (EntityPlayerSP) Minecraft.getMinecraft().player;
        if (!(item.getItem() instanceof ItemGun))
            return;

        GunType gunType = ((ItemGun) item.getItem()).type;
        if (gunType == null)
            return;

        ModelEnhancedGun model = getOrCreateModel(gunType, true, player.getUniqueID());
        if(!(Minecraft.getMinecraft().getRenderViewEntity() instanceof AbstractClientPlayer)) {
            return;
        }
        
        if (model == null)
            return;
        
        Render<AbstractClientPlayer> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(Minecraft.getMinecraft().player);
        RenderPlayer renderplayer = (RenderPlayer) render;
        ModelPlayer modelPlayer=renderplayer.getMainModel();
        ClientProxy.renderHooks.hidePlayerModel((AbstractClientPlayer)Minecraft.getMinecraft().getRenderViewEntity(), renderplayer);

        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) model.config;
        if(AnimationController.getClientController() == null || AnimationController.getClientController().getConfig() != config||AnimationController.getClientController().player!=Minecraft.getMinecraft().player){
            AnimationController.setClientController(new AnimationController(Minecraft.getMinecraft().player,config));
        }
        AnimationController controller=AnimationController.getClientController();

        if(!item.hasTagCompound())
            return;
        
        float partialTicks = getTimer().renderPartialTicks;
        shellModel=null;

        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);

        if(!model.initCal) {
            model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS), true);
        }
        
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
        mat.rotate(toRadians(90.0F), new Vector3f(0,1,0));
        
        /**
         * 诡异的缩放2023.6.7
         * */
        mat.scale(new Vector3f(1/sizeFactor, 1/sizeFactor, 1/sizeFactor));
        //Do hand rotations
        float f5 = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * partialTicks;
        float f6 = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * partialTicks;
        mat.rotate(toRadians((player.rotationPitch - f5) * 0.1F), new Vector3f(1, 0, 0));
        mat.rotate(toRadians((player.rotationYaw - f6) * 0.1F), new Vector3f(0, 1, 0));

        float rotateX=0;
        float adsModifier = (float) (0.95f - controller.ADS);
        if(player.isElytraFlying()) {
            adsModifier=0f;
        }
        
        /**
         *  global
         * */
        mat.rotate(toRadians(90), new Vector3f(0, 1, 0));
        mat.translate(new Vector3f(config.global.globalTranslate.x, config.global.globalTranslate.y, config.global.globalTranslate.z));
        mat.scale(new Vector3f(config.global.globalScale.x,config.global.globalScale.y,config.global.globalScale.z));
        mat.rotate(toRadians(-90), new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.y/180*3.14f, new Vector3f(0, 1, 0));
        mat.rotate(config.global.globalRotate.x/180*3.14f, new Vector3f(1, 0, 0));
        mat.rotate(config.global.globalRotate.z/180*3.14f, new Vector3f(0, 0, 1));
        
        /**
         * camera
         * 如果想保证mwf和blender视角强一致性
         * 请不要使用hip调位置参数 并在blender中镜头对象默认位置设置在原点 然后在您的动画中调整镜头位置
         * */
        if(model.model.geoModel.nodes.get("mwf_camera")!=null && model == firstPersonModel) {
            Matrix4f cameraMat=model.getGlobalTransform("mwf_camera");
            AxisAngle4d cam_aa=mwf_camera_rot;
            Vector3f cam_pos=mwf_camera_pos;
            Vector3f cam_begin_pos=model.model.geoModel.nodes.get("mwf_camera").pos;
            cameraMat.getRotation(cam_aa);
            cameraMat.getTranslation(cam_pos);
            cam_pos.sub(cam_begin_pos).negate();
            mat.rotate((float)cam_aa.angle,(float)-cam_aa.x,(float)-cam_aa.y,(float)-cam_aa.z);
            mat.translate(cam_pos);
        }else {
            mwf_camera_pos.set(0,0,0);
            mwf_camera_rot.set(0, 0, 0, 0);
        }
        
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
        mat.translate(new Vector3f(0, adsModifier * Interpolation.SINE_IN.interpolate(0F, (-0.2f * (1F - (float)controller.ADS)), GUN_BALANCING_Y),0));
        mat.translate(new Vector3f(0, adsModifier * ((float) (0.05f * (Math.sin(SMOOTH_SWING/10) * GUN_BALANCING_Y))),0));

        mat.rotate(toRadians(adsModifier * 0.1f * Interpolation.SINE_OUT.interpolate(-GUN_BALANCING_Y, GUN_BALANCING_Y, adsModifier * MathHelper.sin(f2 * (float) Math.PI))), new Vector3f(0f,1f, 0f));

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
        mat.translate(new Vector3f(0f, ((VAL / 500) * (0.95f -  (float)controller.ADS)),  ((VAL2 / 500 * (0.95f -  (float)controller.ADS)))));
        mat.rotate(toRadians(adsModifier * VALROT), new Vector3f(1F, 0F, 0F));

        /**
         * ACTION GUN BALANCING X / Y
         */
        mat.translate(new Vector3f((float) (0.1f*GUN_BALANCING_X*Math.cos(Math.PI * RenderParameters.SMOOTH_SWING / 50)) * (1F -  (float)controller.ADS),0,0));
        rotateX-=(GUN_BALANCING_X * 4F) + (float) (GUN_BALANCING_X * Math.sin(Math.PI * RenderParameters.SMOOTH_SWING / 35));
        rotateX-=(float) Math.sin(Math.PI * GUN_BALANCING_X);
        rotateX-=(GUN_BALANCING_X) * 0.4F;
        /**
         * ACTION PROBE
         */
        if(ModularWarfare.isLoadedModularMovements) {
            rotateX+=15F * ClientListener.cameraProbeOffset;
        }
        mat.rotate(toRadians(rotateX),  new Vector3f(1f, 0f, 0f));

        /**
         * ACTION SPRINT
         */
        RenderParameters.VALSPRINT = (float) (Math.cos(controller.SPRINT_RANDOM*2*Math.PI)) * gunType.moveSpeedModifier;
        RenderParameters.VALSPRINT2 = (float)(Math.sin(controller.SPRINT_RANDOM*2*Math.PI)) * gunType.moveSpeedModifier;

        Vector3f customSprintRotation = new Vector3f();
        Vector3f customSprintTranslate = new Vector3f();
        float springModifier = (float) (0.8f - controller.ADS);
        mat.rotate(toRadians(0.2f * VALSPRINT * springModifier), new Vector3f(1, 0, 0));
        mat.rotate(toRadians(VALSPRINT2 * springModifier), new Vector3f(0, 0, 1));
        mat.translate(new Vector3f(VALSPRINT * 0.2f * springModifier, 0, VALSPRINT2 * 0.2f * springModifier));

        customSprintRotation = new Vector3f((config.sprint.sprintRotate.x * (float) controller.SPRINT), (config.sprint.sprintRotate.y * (float) controller.SPRINT), (config.sprint.sprintRotate.z * (float) controller.SPRINT));
        customSprintTranslate = new Vector3f((config.sprint.sprintTranslate.x * (float) controller.SPRINT), (config.sprint.sprintTranslate.y * (float) controller.SPRINT), (config.sprint.sprintTranslate.z * (float) controller.SPRINT));

        customSprintRotation.mul((1F - (float) controller.ADS));
        customSprintTranslate.mul((1F - (float) controller.ADS));
        /**
         * CUSTOM HIP POSITION
         */
        
        Vector3f customHipRotation = new Vector3f(config.aim.rotateHipPosition.x, config.aim.rotateHipPosition.y, config.aim.rotateHipPosition.z);
        Vector3f customHipTranslate = new Vector3f(config.aim.translateHipPosition.x, (config.aim.translateHipPosition.y), (config.aim.translateHipPosition.z));
        
        Vector3f customAimRotation = new Vector3f((config.aim.rotateAimPosition.x *  (float)controller.ADS), (config.aim.rotateAimPosition.y *  (float)controller.ADS), (config.aim.rotateAimPosition.z *  (float)controller.ADS));
        Vector3f customAimTranslate = new Vector3f((config.aim.translateAimPosition.x *  (float)controller.ADS), (config.aim.translateAimPosition.y *  (float)controller.ADS), (config.aim.translateAimPosition.z *  (float)controller.ADS));
        
        mat.rotate(toRadians(customHipRotation.x + customSprintRotation.x+customAimRotation.x), new Vector3f(1f,0f,0f));
        mat.rotate(toRadians(customHipRotation.y + customSprintRotation.y+customAimRotation.y), new Vector3f(0f,1f,0f));
        mat.rotate(toRadians(customHipRotation.z + customSprintRotation.z+customAimRotation.z), new Vector3f(0f,0f,1f));
        mat.translate(new Vector3f(customHipTranslate.x + customSprintTranslate.x+customAimTranslate.x, customHipTranslate.y + customSprintTranslate.y+customAimTranslate.y, customHipTranslate.z + customSprintTranslate.z+customAimTranslate.z));

        float renderInsideGunOffset=5;
        
        /**
         * ATTACHMENT AIM
         * */
        ItemAttachment sight = null;
        if(GunType.getAttachment(item, AttachmentPresetEnum.Sight)!=null) {
            sight = (ItemAttachment) GunType.getAttachment(item, AttachmentPresetEnum.Sight).getItem();
            Attachment sightConfig=config.attachment.get(sight.type.internalName);
            if(sightConfig!=null) {
                //System.out.println("test");
                float ads=(float) controller.ADS;
                Vector3f aimPosOffset = new Vector3f(sightConfig.sightAimPosOffset.x, sightConfig.sightAimPosOffset.y, sightConfig.sightAimPosOffset.z);
                Vector3f aimRotOffset = new Vector3f(sightConfig.sightAimRotOffset.x, sightConfig.sightAimRotOffset.y, sightConfig.sightAimRotOffset.z);
                
                ItemStack handguardStack = GunType.getAttachment(item, AttachmentPresetEnum.Handguard);
                if (handguardStack != null && handguardStack.getItem() instanceof ItemAttachment) {
                    String handguardName = ((ItemAttachment) handguardStack.getItem()).type.internalName;
                    applyHandguardInfluenceToAim(config, sightConfig, handguardName, aimPosOffset, aimRotOffset);
                }
                
                mat.translate(new Vector3f(aimPosOffset.x, aimPosOffset.y, aimPosOffset.z).mul(ads));
                mat.rotate(ads * aimRotOffset.y * 3.14f / 180, new Vector3f(0, 1, 0));
                mat.rotate(ads * aimRotOffset.x * 3.14f / 180, new Vector3f(1, 0, 0));
                mat.rotate(ads * aimRotOffset.z * 3.14f / 180, new Vector3f(0, 0, 1));
                renderInsideGunOffset=sightConfig.renderInsideGunOffset;
            }
        }
        
        /**
         * RECOIL
         */
        float min = -1.5f;
        float max = 1.5f;
        float randomNum = new Random().nextFloat();
        float randomShake = min + (randomNum * (max - min));
        float alpha = anim.lastGunRecoil + (anim.gunRecoil - anim.lastGunRecoil) * partialTicks;

        if(gunType.useNewRecoilSystem) {

            long currentTime = System.currentTimeMillis();
            float timeDelta = Math.min((currentTime - lastRecoilTime) / 1000f, MAX_TIME_DELTA);
            lastRecoilTime = currentTime;
            
            if(anim.shooting) {
                isRecovering = false;
                // 使用playerRecoilYaw影响左右旋转和位移
                targetRecoilX = RenderParameters.playerRecoilYaw * 1.5f;
                // 使用playerRecoilPitch影响上下位移和旋转
                targetRecoilY = RenderParameters.playerRecoilPitch * 0.8f;
                targetRecoilZ = RenderParameters.playerRecoilPitch * 0.6f;
                

                bounceRecoilX = targetRecoilX * RECOIL_BOUNCE_FACTOR * 1.5f;
                bounceRecoilY = targetRecoilY * RECOIL_BOUNCE_FACTOR;
                bounceRecoilZ = targetRecoilZ * RECOIL_BOUNCE_FACTOR;
                

                lastRecoilX = currentRecoilX;
                lastRecoilY = currentRecoilY;
                lastRecoilZ = currentRecoilZ;
            } else {
                isRecovering = true;
                float recoverTime = RECOIL_RECOVER_TIME;

                if(Math.abs(currentRecoilX) > RECOIL_FAST_RECOVER_THRESHOLD || 
                   Math.abs(currentRecoilY) > RECOIL_FAST_RECOVER_THRESHOLD ||
                   Math.abs(currentRecoilZ) > RECOIL_FAST_RECOVER_THRESHOLD) {
                    recoverTime /= RECOIL_FAST_RECOVER_FACTOR;
                }
                
                float recovery = Math.min(timeDelta / recoverTime, 1.0f);
                targetRecoilX *= (1f - recovery);
                targetRecoilY *= (1f - recovery);
                targetRecoilZ *= (1f - recovery);
                

                bounceRecoilX *= (1f - recovery * 2f);
                bounceRecoilY *= (1f - recovery * 2f);
                bounceRecoilZ *= (1f - recovery * 2f);


                lastRecoilX = currentRecoilX;
                lastRecoilY = currentRecoilY;
                lastRecoilZ = currentRecoilZ;
            }
            
            // 使用partialTicks进行平滑插值
            float bounceSin = (float)Math.sin(currentTime * BOUNCE_FREQUENCY);
            float targetX = targetRecoilX + bounceRecoilX * bounceSin;
            float targetY = targetRecoilY + bounceRecoilY * bounceSin;
            float targetZ = targetRecoilZ + bounceRecoilZ * bounceSin;

            // 使用线性插值计算当前值，添加帧率补偿
            float smoothing = isRecovering ? 0.3f : 0.6f;
            float frameComp = Math.min(144f / Math.max(Minecraft.getDebugFPS(), 30f), 4f);
            float lerpFactor = smoothing * frameComp * Math.min(timeDelta * 80f, 2f) * partialTicks;

            currentRecoilX = lastRecoilX + (targetX - lastRecoilX) * lerpFactor;
            currentRecoilY = lastRecoilY + (targetY - lastRecoilY) * lerpFactor;
            currentRecoilZ = lastRecoilZ + (targetZ - lastRecoilZ) * lerpFactor;

            // 防止数值过大
            currentRecoilX = MathHelper.clamp(currentRecoilX, -10f, 10f);
            currentRecoilY = MathHelper.clamp(currentRecoilY, -10f, 10f);
            currentRecoilZ = MathHelper.clamp(currentRecoilZ, -10f, 10f);
        } else {
            float linearRecoil = Interpolation.LINEAR.interpolate(0F, 1F, alpha);
            float sin = MathHelper.sin((float) (2 * Math.PI * linearRecoil));
            float sin10 = MathHelper.sin((float) (2 * Math.PI * linearRecoil)) * 0.05f;
            currentRecoilX = sin * anim.recoilSide;
            currentRecoilY = linearRecoil;
            currentRecoilZ = linearRecoil;
        }
        
        //配件抖动影响参数
        float modelBackwardsFactor = 1.0f;
        float modelUpwardsFactor = 1.0f;
        float modelShakeFactor = 1.0f;
        
        if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof ItemGun) {

            // Stock
            ItemStack itemStock = GunType.getAttachment(player.getHeldItemMainhand(), AttachmentPresetEnum.Stock);
            if (itemStock != null && itemStock.getItem() != Items.AIR) {
                ItemAttachment itemAttachment = (ItemAttachment) itemStock.getItem();
                if(itemAttachment.type.model instanceof ModelAttachment && ((ModelAttachment)itemAttachment.type.model).config.stock != null) {
                    modelBackwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilBackwardsFactor;
                    modelUpwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilUpwardsFactor;
                    modelShakeFactor *= ((ModelAttachment) itemAttachment.type.model).config.stock.modelRecoilShakeFactor;
                }
            }

            // Pistolgrip
            ItemStack itemPistolgrip = GunType.getAttachment(player.getHeldItemMainhand(), AttachmentPresetEnum.Pistolgrip);
            if (itemPistolgrip != null && itemPistolgrip.getItem() != Items.AIR) {
                ItemAttachment itemAttachment = (ItemAttachment) itemPistolgrip.getItem();
                if(itemAttachment.type.model instanceof ModelAttachment && ((ModelAttachment)itemAttachment.type.model).config.pistolgrip != null) {
                    modelBackwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.pistolgrip.modelRecoilBackwardsFactor;
                    modelUpwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.pistolgrip.modelRecoilUpwardsFactor;
                    modelShakeFactor *= ((ModelAttachment) itemAttachment.type.model).config.pistolgrip.modelRecoilShakeFactor;
                }
            }

            // Handguard
            ItemStack itemHandguard = GunType.getAttachment(player.getHeldItemMainhand(), AttachmentPresetEnum.Handguard);
            if (itemHandguard != null && itemHandguard.getItem() != Items.AIR) {
                ItemAttachment itemAttachment = (ItemAttachment) itemHandguard.getItem();
                if(itemAttachment.type.model instanceof ModelAttachment && ((ModelAttachment)itemAttachment.type.model).config.handguard != null) {
                    modelBackwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.handguard.modelRecoilBackwardsFactor;
                    modelUpwardsFactor *= ((ModelAttachment) itemAttachment.type.model).config.handguard.modelRecoilUpwardsFactor;
                    modelShakeFactor *= ((ModelAttachment) itemAttachment.type.model).config.handguard.modelRecoilShakeFactor;
                }
            }

            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(player.getHeldItemMainhand(), attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR && itemStack.getItem() instanceof ItemAttachment) {
                    AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                    if (config.attachment.containsKey(attachmentType.internalName)) {
                        Attachment attachConfig = config.attachment.get(attachmentType.internalName);
                        modelBackwardsFactor *= attachConfig.modelRecoilBackwardsFactor;
                        modelUpwardsFactor *= attachConfig.modelRecoilUpwardsFactor; 
                        modelShakeFactor *= attachConfig.modelRecoilShakeFactor;
                    }
                }
            }
        }


        float aimFactor = gunType.useNewRecoilSystem ? 
            (float)(1.0 - controller.ADS * (1.0 - ADS_RECOIL_FACTOR)) : // 新系统
            (float)(1.0 - controller.ADS); // 旧系统
        
        float aimBackwardsFactor = linearInterpolation(1.0f, config.extra.modelRecoilBackwardsADSFactor, (float)controller.ADS);
        float aimUpwardsFactor = linearInterpolation(1.0f, config.extra.modelRecoilUpwardsADSFactor, (float)controller.ADS);
        float aimShakeFactor = linearInterpolation(1.0f, config.extra.modelRecoilShakeADSFactor, (float)controller.ADS);
        
        if(gunType.useNewRecoilSystem) {
            //前后
            mat.translate(new Vector3f(-currentRecoilZ * config.extra.modelRecoilBackwards * aimFactor * modelBackwardsFactor * aimBackwardsFactor, 0F, 0F));
            mat.translate(new Vector3f(0F, -currentRecoilZ * config.extra.modelRecoilBackwards * modelBackwardsFactor* aimBackwardsFactor * 0.05F , 0F));
            //上下
            mat.translate(new Vector3f(0f,currentRecoilY * config.extra.modelRecoilUpwards * aimFactor * modelUpwardsFactor * aimUpwardsFactor * 0.025F,0f));
            //左右
            mat.translate(new Vector3f(0f,0f,currentRecoilX * config.extra.modelRecoilShake * 3f * aimFactor * modelShakeFactor * aimShakeFactor * 0.025F));
            
            //旋转
            mat.rotate(toRadians(currentRecoilX * config.extra.modelRecoilShake * 3f * aimFactor * modelShakeFactor * aimShakeFactor * 2f), new Vector3f(0F, 1F, 0F)); // 左右摇摆
            mat.rotate(toRadians(currentRecoilY * config.extra.modelRecoilUpwards * aimFactor * modelUpwardsFactor * aimUpwardsFactor * 2f), new Vector3f(0F, 0F, 1F)); // 上下摇摆
        } else {
            float linearRecoil = Interpolation.LINEAR.interpolate(0F, 1F, alpha);
            float sin = MathHelper.sin((float) (2 * Math.PI * linearRecoil));
            float sin10 = MathHelper.sin((float) (2 * Math.PI * linearRecoil)) * 0.05f;
            
            mat.translate(new Vector3f(-(currentRecoilZ) * config.extra.modelRecoilBackwards * aimFactor * modelBackwardsFactor, 0F, 0F));
            mat.translate(new Vector3f(0F, (-(currentRecoilZ) * config.extra.modelRecoilBackwards * modelBackwardsFactor) * 0.05F, 0F));
            mat.translate(new Vector3f(0F, 0F, sin10 * anim.recoilSide * config.extra.modelRecoilUpwards * modelUpwardsFactor));
            mat.rotate(toRadians(currentRecoilX * config.extra.modelRecoilUpwards * modelUpwardsFactor), new Vector3f(0F, 0F, 1F));
            mat.rotate(toRadians(5F * sin10 * anim.recoilSide * config.extra.modelRecoilUpwards * modelUpwardsFactor), new Vector3f(0F, 0F, 1F));
            mat.rotate(toRadians((currentRecoilY) * config.extra.modelRecoilUpwards), new Vector3f(0F, 0F, 1F));
        }

        //抖动效果保持不变
        mat.rotate(toRadians(((-alpha) * randomShake * config.extra.modelRecoilShake * modelShakeFactor)), new Vector3f(0.0f, 1.0f, 0.0f));
        mat.rotate(toRadians(((-alpha) * randomShake * config.extra.modelRecoilShake * modelShakeFactor)), new Vector3f(1.0f, 0.0f, 0.0f));

        if(ScopeUtils.isIndsideGunRendering) {
            mat.translate(new Vector3f(-renderInsideGunOffset, 0, 0));
        }

        boolean shouldRenderFlash1=true;
        if ((GunType.getAttachment(item, AttachmentPresetEnum.Barrel) != null)) {
            AttachmentType attachmentType = ((ItemAttachment) GunType.getAttachment(item, AttachmentPresetEnum.Barrel).getItem()).type;
            if (attachmentType.attachmentType == AttachmentPresetEnum.Barrel) {
                shouldRenderFlash1 = !attachmentType.barrel.hideFlash;
            }
        }

        /**
         * 不支持光影
         * */
        if(ModConfig.INSTANCE.client.gunFlashEffect) {
            if(!OptifineHelper.isShadersEnabled()) {
                if (shouldRenderFlash1 && anim.shooting && anim.getShootingAnimationType().showFlashModel() && !player.isInWater()) {
                    float handLight=(bx+Minecraft.getMinecraft().world.getSunBrightness(partialTicks)*by)/240f;
                    if(handLight<1) {
                        if(handLight<0.9f) {
                            Minecraft.getMinecraft().entityRenderer.disableLightmap();
                        }
                        if(handLight<0.5f) {
                            GlStateManager.disableLight(0);
                            GlStateManager.disableLight(1);
                        }
                        GlStateManager.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, buf(0.8f*handLight, 0.8f*handLight, 0.8f*handLight, 1));
                        GlStateManager.enableLight(2);
                        GlStateManager.glLight(GL11.GL_LIGHT2, GL11.GL_POSITION, buf(1, 5f, -10, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT2, GL11.GL_AMBIENT, buf(0, 0, 0, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT2, GL11.GL_DIFFUSE, buf(10, 10f, 10f, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT2, GL11.GL_SPECULAR, buf(0, 0, 0, 1));
                        GlStateManager.enableLight(3);
                        GlStateManager.glLight(GL11.GL_LIGHT3, GL11.GL_POSITION, buf(-1, 5f, -10, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT3, GL11.GL_AMBIENT, buf(0, 0, 0, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT3, GL11.GL_DIFFUSE, buf(10, 10f, 10f, 1));
                        GlStateManager.glLight(GL11.GL_LIGHT3, GL11.GL_SPECULAR, buf(0, 0, 0, 1));    
                    }
                }      
            }
        }
        
        floatBuffer.clear();
        mat.get(floatBuffer);
        floatBuffer.rewind();
        GlStateManager.pushMatrix();
        GlStateManager.multMatrix(floatBuffer);
        
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        if(ScopeUtils.isIndsideGunRendering) {
            GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
        }
        float worldScale = 1;
        float rotateXRendering=rotateX;
        CROSS_ROTATE=rotateXRendering;
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        color(1, 1, 1, 1f);
        
        boolean applySprint = controller.SPRINT > 0.1 && controller.INSPECT >= 1 && controller.TRANSFORM >= 1;
        boolean isRenderHand0 = ScopeUtils.isRenderHand0||!OptifineHelper.isShadersEnabled();
        boolean isRenderHand1 = (OptifineHelper.isShadersEnabled()&&!ScopeUtils.isRenderHand0)||!OptifineHelper.isShadersEnabled();
        HashSet<String> exceptParts=new HashSet<String>();
        if(isRenderHand0) {
            exceptParts.addAll(config.defaultHidePart);
            if(config.specialEffect.flashModelGroups!=null) {
                config.specialEffect.flashModelGroups.forEach((g)->{
                    exceptParts.add(g.name);
                });
            }
            //exceptParts.addAll(DEFAULT_EXCEPT);
            
            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(item, attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                    String binding = "gunModel";
                    if(config.attachmentGroup.containsKey(attachment.typeName)) {
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
                    if(config.attachmentGroup.containsKey(attachment.typeName)) {
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

            ItemStack loadedAmmo = new ItemStack(item.getTagCompound().getCompoundTag("ammo"));
            if (loadedAmmo.getItem() instanceof ItemAmmo) {
                ItemAmmo itemAmmo = (ItemAmmo) loadedAmmo.getItem();
                AmmoType ammoType = itemAmmo.type;
                if (config.attachment.containsKey(ammoType.internalName)) {
                    if (config.attachment.get(ammoType.internalName).hidePart != null) {
                        exceptParts.addAll(config.attachment.get(ammoType.internalName).hidePart);
                    }
                    if (config.attachment.get(ammoType.internalName).showPart != null) {
                        exceptParts.removeAll(config.attachment.get(ammoType.internalName).showPart);
                    }
                }
            }
            
            
            exceptParts.addAll(DEFAULT_EXCEPT);
        }
        
        HashSet<String> exceptPartsRendering=exceptParts;
        
        
        //model.updateAnimation(controller.getTime(),false);
        
        final ItemAttachment sightRendering=sight;

        boolean glowMode=ObjModelRenderer.glowTxtureMode;
        ObjModelRenderer.glowTxtureMode=true;
        /**
         * 绘制镜面擦除
         * */
        blendTransform(model, item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(),
            controller.getSprintTime(), (float)controller.SPRINT, "sprint_righthand", applySprint, true, () -> {
                if (isRenderHand0) {
                    if (sightRendering != null) {
                        String binding = "gunModel";
                        if (config.attachment.containsKey(sightRendering.type.internalName)) {
                            binding = config.attachment.get(sightRendering.type.internalName).binding;
                        }
                        model.applyGlobalTransformToOther(binding, () -> {
                            renderAttachment(config, AttachmentPresetEnum.Sight.typeName,
                                sightRendering.type.internalName, item, () -> {
                                    writeScopeGlassDepth(sightRendering.type,
                                        (ModelAttachment)sightRendering.type.model, controller.ADS > 0, worldScale,
                                        sightRendering.type.sight.modeType.isPIP);
                                });
                        });
                    }
                }
            });
        /**
         * LEFT HAND GROUP
         * */
        blendTransform(model,item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(), (float) controller.SPRINT, "sprint_lefthand", applySprint, false, () -> {
            if (isRenderHand0) {
                /**
                 * player left hand
                 * */
                if (gunType.handsTextureType != null) {
                    bindCustomHands(gunType.handsTextureType);
                } else {
                    bindPlayerSkin();
                }
                ObjModelRenderer.glowTxtureMode=false;
                renderHandAndArmor(EnumHandSide.LEFT, player, config, modelPlayer, model);
                ObjModelRenderer.glowTxtureMode=true;
            }
        });
        
        /**
         * RIGHT HAND GROUP
         * */
        blendTransform(model,item, !config.animations.containsKey(AnimationType.SPRINT), controller.getTime(), controller.getSprintTime(),(float)controller.SPRINT, "sprint_righthand", applySprint, false, () -> {
            if(isRenderHand0) {
                /**
                 * player right hand
                 * */

                if(gunType.handsTextureType != null){
                    bindCustomHands(gunType.handsTextureType);
                } else {
                    bindPlayerSkin();
                }
                ObjModelRenderer.glowTxtureMode=false;
                renderHandAndArmor(EnumHandSide.RIGHT, player, config, modelPlayer, model);
                ObjModelRenderer.glowTxtureMode=true;
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
                shellTexType="guns";
                shellTexPath=gunPath;
                model.renderPartExcept(exceptPartsRendering);
                
                /**
                 * selecotr
                 * */
                WeaponFireMode fireMode = GunType.getFireMode(item);
                if(fireMode==WeaponFireMode.SEMI) {
                    model.renderPart("selector_semi");
                }else if(fireMode==WeaponFireMode.FULL) {
                    model.renderPart("selector_full");
                }else if(fireMode==WeaponFireMode.BURST){
                    model.renderPart("selector_burst");
                }else if(fireMode==WeaponFireMode.SAFE){
                    model.renderPart("selector_safe");
                }
                
                /**
                 * ammo and bullet
                 * */
                boolean flagDynamicAmmoRendered=false;
                ItemStack stackAmmo = new ItemStack(item.getTagCompound().getCompoundTag("ammo"));
                ItemStack orignalAmmo = stackAmmo;
                stackAmmo=controller.getRenderAmmo(stackAmmo);
                ItemStack renderAmmo=stackAmmo;
                ItemStack prognosisAmmo=ClientTickHandler.reloadEnhancedPrognosisAmmoRendering;
                
                ItemStack bulletStack=ItemStack.EMPTY;
                int currentAmmoCount=0;
                int costAmmoCount=0;
                
                VarBoolean defaultBulletFlag=new VarBoolean();
                defaultBulletFlag.b=true;
                boolean defaultAmmoFlag=true;
                
                if (gunType.acceptedBullets != null) {
                    currentAmmoCount= item.getTagCompound().getInteger("ammocount");
                    if (anim.reloading) {
                        currentAmmoCount += anim.getAmmoCountOffset(true);
                    }
                    bulletStack= new ItemStack(item.getTagCompound().getCompoundTag("bullet"));
                    if (anim.reloading) {
                        bulletStack = AnimationController.getClientController().getRenderAmmo(bulletStack);
                    }
                    costAmmoCount=gunType.internalAmmoStorage-currentAmmoCount;
                }else {
                    Integer currentMagcount=null;
                    if(stackAmmo!=null&&!stackAmmo.isEmpty()&&stackAmmo.hasTagCompound()) {
                        if(stackAmmo.getTagCompound().hasKey("magcount")) {
                            currentMagcount=stackAmmo.getTagCompound().getInteger("magcount");
                        }
                        currentAmmoCount=ReloadHelper.getBulletOnMag(stackAmmo, currentMagcount);
                        bulletStack= new ItemStack(stackAmmo.getTagCompound().getCompoundTag("bullet"));  
                    }
                    if(stackAmmo.getItem() instanceof ItemAmmo) {
                        costAmmoCount=((ItemAmmo)stackAmmo.getItem()).type.ammoCapacity-currentAmmoCount;
                    }
                }
                int currentAmmoCountRendering=currentAmmoCount;
                int costAmmoCountRendering=costAmmoCount;
                
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
                                shellTexType="bullets";
                                shellTexPath=pathAmmo;
                            }
                            for (int bullet = 0; bullet < currentAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                                model.applyGlobalTransformToOther("bulletModel_" + bullet, () -> {
                                    renderAttachment(config, "bullet", bulletType.internalName, item, () -> {
                                        bulletType.model.renderPart("bulletModel", worldScale);
                                    });
                                });
                            }
                            for (int bullet = 0; bullet < costAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                                model.applyGlobalTransformToOther("shellModel_" + bullet, () -> {
                                    renderAttachment(config, "shell", bulletType.internalName, item, () -> {
                                        bulletType.shell.renderPart("shellModel", worldScale);
                                    });
                                });
                            }
                            model.applyGlobalTransformToOther("bulletModel", () -> {
                                renderAttachment(config, "bullet", bulletType.internalName, item, () -> {
                                    bulletType.model.renderPart("bulletModel", worldScale);
                                });
                            });
                            shellModel=bulletType.shell;
                            defaultBulletFlag.b=false;
                        }
                    }
                }
                
                ItemStack[] ammoList=new ItemStack[] {stackAmmo,orignalAmmo,prognosisAmmo};
                String[] binddings=new String[] {"ammoModel","ammoModelPre","ammoModelPost"};
                for(int x=0;x<3;x++) {
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
                                                    EnhancedRenderConfig.Transform ammoTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(renderAmmo.getTagCompound().getInteger("magcount") - 1);
                                                    EnhancedRenderConfig.Transform renderTransform = ammoTransform;
                                                    if (anim.reloading && (anim
                                                            .getReloadAnimationType() == AnimationType.RELOAD_FIRST_QUICKLY)) {
                                                        float magAlpha = (float) controller.RELOAD;
                                                        renderTransform = new EnhancedRenderConfig.Transform();
                                                        ammoTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(prognosisAmmo.getTagCompound().getInteger("magcount") - 1);
                                                        EnhancedRenderConfig.Transform beginTransform = config.attachment.get(itemAmmo.type.internalName).multiMagazineTransform.get(orignalAmmo.getTagCompound().getInteger("magcount") - 1);

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
                                    renderAttachment(config, "ammo", ammoType.internalName, item, () -> {
                                        ammoType.model.renderPart("ammoModel", worldScale);
                                        if (defaultBulletFlag.b) {
                                            if (renderAmmo.getTagCompound().hasKey("magcount")) {
                                                for (int i = 1; i <= ammoType.magazineCount; i++) {
                                                    int count = ReloadHelper.getBulletOnMag(renderAmmo, i);
                                                    for (int bullet = 0; bullet < count && bullet < BULLET_MAX_RENDER; bullet++) {
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
                                if (bulletStack != null) {
                                    if (bulletStack.getItem() instanceof ItemBullet) {
                                        BulletType bulletType = ((ItemBullet)bulletStack.getItem()).type;
                                        if (bulletType.isDynamicBullet && bulletType.model != null) {
                                            shellModel=bulletType.shell;
                                            
                                            int skinIdBullet = 0;
                                            if (bulletStack.hasTagCompound()) {
                                                if (bulletStack.getTagCompound().hasKey("skinId")) {
                                                    skinIdBullet = bulletStack.getTagCompound().getInteger("skinId");
                                                }
                                            }
                                            if (bulletType.sameTextureAsGun) {
                                                bindTexture("guns", gunPath);
                                            } else {
                                                String pathAmmo =
                                                    skinIdBullet > 0 ? bulletType.modelSkins[skinIdBullet].getSkin()
                                                        : bulletType.modelSkins[0].getSkin();
                                                bindTexture("bullets", pathAmmo);
                                                shellTexType="bullets";
                                                shellTexPath=pathAmmo;
                                            }
                                            for (int bullet = 0; bullet < costAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                                                model.applyGlobalTransformToOther("shellModel_" + bullet, () -> {
                                                    renderAttachment(config, "shell", bulletType.internalName, item, () -> {
                                                        bulletType.shell.renderPart("shellModel", worldScale);
                                                    });
                                                });
                                            }
                                            
                                            model.applyGlobalTransformToOther("shellModel", () -> {
                                                renderAttachment(config, "shell", bulletType.internalName, item, () -> {
                                                    bulletType.shell.renderPart("shellModel", worldScale);
                                                });
                                            });
                                        }
                                    }
                                }
                                model.applyGlobalTransformToOther("bulletModel", () -> {
                                    renderAttachment(config, "bullet", ammoType.internalName, item, () -> {
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
//                System.out.println(model.existPart("shellModel_1"));
                if(defaultBulletFlag.b) {
                    for (int bullet = 0; bullet < currentAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                        model.renderPart("bulletModel_" + bullet);
                    }
//                    ItemStack bulletStack=ItemGun.getUsedBullet(bulletStack, gunType);
                    if(bulletStack!=null&&!bulletStack.isEmpty()) {
                        for (int bullet = 0; bullet < costAmmoCount && bullet < BULLET_MAX_RENDER; bullet++) {
                            model.renderPart("shellModel_" + bullet);
                        }  
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
                        
                        if(ScopeUtils.isIndsideGunRendering) {
                            if (attachment == AttachmentPresetEnum.Sight) {
                                if (config.attachment.containsKey(attachmentType.internalName)) {
                                    if(!config.attachment.get(attachmentType.internalName).renderInsideSightModel) {
                                        continue;
                                    }
                                }else {
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
                                renderAttachment(config, attachment.typeName, attachmentType.internalName, item, () -> {
                                    attachmentModel.renderAttachment(worldScale);
                                    ObjModelRenderer.glowTxtureMode=false;
                                    if(attachment==AttachmentPresetEnum.Sight) {
                                        renderScopeGlass(attachmentType, attachmentModel, controller.ADS > 0, worldScale);
                                    }
                                    AnimationType currentAction = null;
                                    if (anim != null && anim.controller != null) {
                                        currentAction = anim.controller.getPlayingAnimation();
                                    }
                                    if(attachment==AttachmentPresetEnum.Laser) {
                                        ItemStack heldStack = player.getHeldItemMainhand();
                                            if (heldStack.getItem() instanceof ItemGun) {
                                                boolean laserEnabled = ((ItemGun) heldStack.getItem()).getLaserEnabled(heldStack);
                                                if(laserEnabled) {
                                                    AttachmentRenderConfig.Laser laserConfig = attachmentModel.config.laser;
                                                    ClientProxy.gunEnhancedRenderer.renderLaserModel(laserConfig, attachmentModel, bx, by, worldScale, currentAction, laserEnabled);
                                                }
                                            }
                                    }
                                    ObjModelRenderer.glowTxtureMode=true;
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
                 *  flashmodel panelModel
                 *  */
                GlStateManager.enableBlend();
                GlStateManager.depthMask(false);
                GlStateManager.disableLighting();
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                boolean shouldRenderFlash=true;
                if ((GunType.getAttachment(item, AttachmentPresetEnum.Barrel) != null)) {
                    AttachmentType attachmentType = ((ItemAttachment) GunType.getAttachment(item, AttachmentPresetEnum.Barrel).getItem()).type;
                    if (attachmentType.attachmentType == AttachmentPresetEnum.Barrel) {
                        shouldRenderFlash = !attachmentType.barrel.hideFlash;
                    }
                }
                if (shouldRenderFlash && anim.shooting && anim.getShootingAnimationType().showFlashModel() && !player.isInWater()) {
                    ObjModelRenderer.glowTxtureMode=false;
                    if (ScopeUtils.isIndsideGunRendering) {
                        GlStateManager.tryBlendFuncSeparate(SourceFactor.ONE, DestFactor.ZERO, SourceFactor.ONE,
                            DestFactor.ZERO);
                    } else {
                        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                            SourceFactor.ONE, DestFactor.ZERO);
                    }
                    
                    GlStateManager.pushMatrix();
                    ItemStack itemStack = GunType.getAttachment(item, AttachmentPresetEnum.Barrel);
                    if (itemStack != null && itemStack.getItem() != Items.AIR) {
                        AttachmentType attachmentType = ((ItemAttachment)itemStack.getItem()).type;
                        if (config.attachment.containsKey(attachmentType.internalName)) {
                            if (config.attachment.get(attachmentType.internalName).flashModelOffset != null) {
                                GlStateManager.translate(
                                    config.attachment.get(attachmentType.internalName).flashModelOffset.x,
                                    config.attachment.get(attachmentType.internalName).flashModelOffset.y,
                                    config.attachment.get(attachmentType.internalName).flashModelOffset.z);
                            }
                        }
                    }
                    TextureType flashType = gunType.flashType;
                    bindTexture(flashType.resourceLocations.get(anim.flashCount % flashType.resourceLocations.size()));
                    if(config.specialEffect.oldFlashModel) { 
                        model.renderPart("flashModel");
                    }
                    if(config.specialEffect.flashModelGroups!=null) {
                        config.specialEffect.flashModelGroups.forEach((group)->{
                            model.renderPart(group.name);
                        });
                    }
                    GlStateManager.popMatrix();
                }
                GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                    SourceFactor.ONE, DestFactor.ZERO);
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
                ObjModelRenderer.glowTxtureMode=true;
                GlStateManager.enableLighting();
                bindTexture("guns", gunPath);
                model.renderPart("translucentModel");
                GlStateManager.disableLighting();
                ObjModelRenderer.glowTxtureMode=false;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
                
                ResourceLocation panelTex=AnimationController.getClientController().getPanelTexture(item, gunType, currentAmmoCount, anim.reloading);
                if(panelTex!=null) {
                    Minecraft.getMinecraft().getTextureManager().bindTexture(panelTex);
                    model.renderPart("panelModel");
                }
                ObjModelRenderer.glowTxtureMode=true;
                OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
                GlStateManager.depthMask(true);
                GlStateManager.enableLighting();
            }
        });
        
        /**
         * POST HANDLE
         * */
        Minecraft.getMinecraft().entityRenderer.enableLightmap();
        GlStateManager.enableLight(0);
        GlStateManager.enableLight(1);
        GlStateManager.disableLight(2);
        GlStateManager.disableLight(3);

        if (OptifineHelper.isShadersEnabled()) {
            Shaders.pushProgram();
            if (ModConfig.INSTANCE.client.gunSmokeCorrectForBSL) {
                Shaders.useProgram(Shaders.ProgramNone);
            }
        }
        /**
         * 这一块光影兼容性不好
         * 推荐在isRenderHand1渲染
         * 但是出于现状考虑 暂时设在isRenderHand0渲染
         * 需要处理深度遮蔽 可以延迟渲染
         * 就像镜面渲染一样 MC的手部半透明一坨大便 只好延迟到hud渲染
        */
        if (isRenderHand0) {
            if (config.specialEffect.postSmokeGroups != null) {
                config.specialEffect.postSmokeGroups.forEach((group) -> {
                    Matrix4f mat2 = new Matrix4f(mat);
                    Matrix4f mat3 = model.getGlobalTransform(group.name);
                    mat2.mul(mat3);
                    AxisAngle4d aa = new AxisAngle4d();
                    mat2.getRotation(aa);
                    model.applyGlobalTransformToOther((group.name), () -> {
                        GlStateManager.rotate((float)Math.toDegrees(aa.angle), -(float)aa.x, -(float)aa.y,
                            -(float)aa.z);
                        GlStateManager.rotate(90, 0, 1, 0);
                        //flashModelOffset for Post Smoke
                        ItemStack itemStack = GunType.getAttachment(item, AttachmentPresetEnum.Barrel);
                        if (itemStack != null && itemStack.getItem() != Items.AIR) {
                            AttachmentType attachmentType = ((ItemAttachment)itemStack.getItem()).type;
                            if (config.attachment.containsKey(attachmentType.internalName)) {
                                if (config.attachment.get(attachmentType.internalName).flashModelOffset != null) {
                                    GlStateManager.translate(
                                        config.attachment.get(attachmentType.internalName).flashModelOffset.x,
                                        config.attachment.get(attachmentType.internalName).flashModelOffset.y,
                                        config.attachment.get(attachmentType.internalName).flashModelOffset.z);
                                }
                            }
                        }
                        drawPostSmoke();
                    });
                });
            }
            //这里不写深度 会导致水反异常反射 不过这玩意不太明显 暂时这样处理吧
            GlStateManager.depthMask(false);
            if (config.specialEffect.ejectionGroups != null) {
                config.specialEffect.ejectionGroups.forEach((group) -> {
                    if (group.ejectSmoke) {
                        model.applyGlobalTransformToOther((group.name), () -> {
                            drawEjectionSmoke(group.ejectSmokeForce);
                        });
                    }
                });
            }
            GlStateManager.depthMask(true);
        }
        if (OptifineHelper.isShadersEnabled()) {
            Shaders.popProgram();
        }
        
        //矩阵结束
        GlStateManager.popMatrix();
        GlStateManager.color(1, 1, 1,1);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        if(isRenderHand0&&!ScopeUtils.isIndsideGunRendering) {
            for(int i=0;i<shellEffects.length;i++) {
                if(shellEffects[i]==null) {
                    continue;
                }
                if(shellEffects[i].mat==null) {
                    shellEffects[i].mat=new Matrix4f(mat).mul(model.getGlobalTransform(shellEffects[i].bindding));
                }
                GlStateManager.pushMatrix();
                floatBuffer.clear();
                shellEffects[i].mat.get(floatBuffer);
                floatBuffer.rewind();
                GlStateManager.multMatrix(floatBuffer);
                GlStateManager.translate(shellEffects[i].pos.x, shellEffects[i].pos.y, shellEffects[i].pos.z);
                GlStateManager.rotate(shellEffects[i].rot.x, 1, 0, 0);
                GlStateManager.rotate(shellEffects[i].rot.y, 0, 1, 0);
                GlStateManager.rotate(shellEffects[i].rot.z, 0, 0, 1);
                if(shellModel!=null) {
                    bindTexture(shellTexType, shellTexPath);
                    shellModel.renderPart("shellModel", worldScale);
                }else {
                    bindTexture(shellTexType, shellTexPath);
                    model.renderPart("shellEffect");
                }
                GlStateManager.popMatrix();
            }
            if(shellEffects.length!=ModConfig.INSTANCE.client.shellEffectCapacity) {
                shellEffects=new ShellEffect[ModConfig.INSTANCE.client.shellEffectCapacity];
            }
            if(shellTime!=-1&&System.currentTimeMillis()>shellTime+5000) {
                for(int i=0;i<shellEffects.length;i++) {
                    shellEffects[i]=null;
                }
                shellTime=-1;
            }  
        }
        
        if(sightRendering!=null) {
            if (!ScopeUtils.isIndsideGunRendering) {
                if(!sightRendering.type.sight.modeType.isPIP) {
                    if (!OptifineHelper.isShadersEnabled()) {
                        copyMirrorTexture();
                        ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
                        eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model,controller.ADS > 0, worldScale);
                    }else {
                        if (isRenderHand0) {
                            GL11.glPushAttrib(GL11.GL_VIEWPORT_BIT);
                            GL11.glDepthRange(0,1);
                            copyMirrorTexture();
                            ClientProxy.scopeUtils.renderPostScope(partialTicks, true, false, true, 1);
                            eraseScopeGlassDepth(sightRendering.type, (ModelAttachment) sightRendering.type.model,controller.ADS > 0, worldScale);
                            writeScopeSoildDepth(controller.ADS > 0);
                            
                            GL11.glPopAttrib();
                        } else {
                            ClientProxy.scopeUtils.renderPostScope(partialTicks, false, true, true, 1);
                        }
                    }  
                }
            }
        }
        ObjModelRenderer.glowTxtureMode=glowMode;
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO);
        GlStateManager.disableBlend();
    }

    private void renderLaserModel(AttachmentRenderConfig.Laser laserConfig, ModelAttachment attachmentModel, float bx, float by, float worldScale, AnimationType currentAction, boolean laserEnabled) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableBlend();
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
            
            GlStateManager.tryBlendFuncSeparate(
                SourceFactor.SRC_ALPHA, 
                DestFactor.ONE,  
                SourceFactor.ONE, 
                DestFactor.ZERO
            );

            GlStateManager.disableLighting();
            ObjModelRenderer.glowTxtureMode = true;
            
            bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/white.png"));
            
            float brightness = 1.2f; 
            GlStateManager.color(
                Math.min(laserConfig.laserColor[0] * brightness, 1.0f),
                Math.min(laserConfig.laserColor[1] * brightness, 1.0f),
                Math.min(laserConfig.laserColor[2] * brightness, 1.0f),
                laserConfig.laserAlpha
            );
            
            attachmentModel.renderPart("laserModel", worldScale);

            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.tryBlendFuncSeparate(
                SourceFactor.SRC_ALPHA, 
                DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ONE, 
                DestFactor.ZERO
            );
            ObjModelRenderer.glowTxtureMode = false;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
            
        } finally {
            GlStateManager.popMatrix();
        }

        if(laserEnabled) {
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            LaserRenderManager.getInstance().addLaserDot(
                laserConfig.laserColor,
                laserConfig.laserAlpha,
                laserConfig.dotSize,
                laserConfig.maxDistance,
                true,
                player.getUniqueID(),
                player.isSprinting(),
                collideFrontDistance,
                currentAction
            );
        }
    }

    
    private static FloatBuffer buf(float x, float y, float z, float w) {
        lightBuf.clear();
        lightBuf.put(x).put(y).put(z).put(w);
        lightBuf.flip();
        return lightBuf;
    }



    public static void addEjectShell(EjectionGroup group, float acc) {
        for (int i = shellEffects.length - 1; i > 0; i--) {
            shellEffects[i] = shellEffects[i - 1];
        }
        shellEffects[0] = new ShellEffect();
        shellEffects[0].bindding = group.name;
        if (group.throwShellMaxForce != null) {
            shellEffects[0].vec = new Vector3f(group.throwShellMaxForce.x, group.throwShellMaxForce.y,
                    group.throwShellMaxForce.z);
            shellEffects[0].vec.rotateX(-(float) (0.2 + Math.random()) * acc * 2);
            shellEffects[0].vec.rotateY((float) (0.2 + Math.random()) * acc * 2);
            shellEffects[0].vec.mul((float) (0.5 + 0.5 * Math.random()));
            shellEffects[0].rot = new Vector3f((float) Math.random() * 360 * acc, (float) Math.random() * 360 * acc,
                    (float) Math.random() * 360 * acc);
        }
        shellTime = System.currentTimeMillis();
    }

    public void drawEjectionSmoke(org.lwjgl.util.vector.Vector3f force) {
        if (ejectionTp == 0 || ejectionTp == 1) {
            return;
        }
        float tp = ejectionTp;
        GlStateManager.pushMatrix();
        // GlStateManager.translate(-1, -2, 0);
        GlStateManager.translate(tp * force.x, tp * force.y, tp * force.z);
        GlStateManager.scale(3, 3, 3);
        GlStateManager.color(0.3f, 0.3f, 0.3f, 1f);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.ONE, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
        Tessellator tessellator = Tessellator.getInstance();
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/particles/smoke_effect.png"));
        tessellator.getInstance().getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        tessellator.getInstance().getBuffer().pos(0, -0.5, -0.5).tex(0, 0).endVertex();
        tessellator.getInstance().getBuffer().pos(0, -0.5, 0.5).tex(0, 1).endVertex();
        tessellator.getInstance().getBuffer().pos(0, 0.5, 0.5).tex(1, 1).endVertex();
        tessellator.getInstance().getBuffer().pos(0, 0.5, -0.5).tex(1, 0).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE,
                DestFactor.ZERO);
    }

    public void drawPostSmoke() {
        if (postSmokeTp == 0 || postSmokeTp == 1) {
            return;
        }
        if (postSmokeAlpha < 0.5f) {
            return;
        }
        GlStateManager.disableLighting();
        GlStateManager.pushMatrix();
        GlStateManager.tryBlendFuncSeparate(SourceFactor.ONE, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/particles/smoke_es.png"));
        // Minecraft.getMinecraft().getTextureManager().bindTexture(new
        // ResourceLocation(ModularWarfare.MOD_ID,
        // "textures/skins/white.png"));
        // GlStateManager.translate(9f, 0, -0.2f);
        float s = 0.05f;
        float tp = postSmokeTp;
        float sinTp = (float) Math.sin(tp * 3.14 / 2);
        float per = (float) Math.sin(Math.sin(tp * 3.14f / 2) * 2);
        float ox = tp / 100;
        float oy = 1 - tp / 50;
        Tessellator tessellator = Tessellator.getInstance();
        float step = 4f / diversion;
        float alpha1 = 1;
        if (tp > 0.5) {
            alpha1 = (float) Math.cos((tp - 0.5f) * 3.14);
            // alpha1=alpha1*alpha1;
        }
        float alpha2 = (1 - 1.2f * tp);
        if (alpha2 < 0) {
            alpha2 = 0;
        }
        alpha2 *= alpha2;

        alpha1 *= (postSmokeAlpha - 0.5f) * 2;
        alpha2 *= (postSmokeAlpha - 0.5f) * 2;
        float wind = 0.2f * tp + 0.3f * postSmokeWind;
        for (int i = 0; i <= diversion; i++) {
            es_joint[i] = 0.5f * wind * tp / diversion * i;
        }

        tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < diversion; i++) {
            float oz = (float) Math.sin(3.14 * 2.5 * wind * per / diversion * i) * tp + es_joint[i + 1]
                    + strafing_joint[i + 1];
            float oz_ = (float) Math.sin(3.14 * 2.5 * wind * per / diversion * (i - 1)) * tp + es_joint[i]
                    + strafing_joint[i];
            float fz = 0.1f / diversion * i * tp * tp;
            float a = alpha1 * (diversion - (i + 1) * alpha1) / diversion;
            // System.out.println(a);
            if (a < 0) {
                a = 0;
            }
            a *= 0.3f;
            // a=alpha1;
            if (i + 1 >= diversion) {
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0 + oz_ - fz)
                        .tex(0 + ox, s / diversion * i * per + oy)
                        .color(1 * a, 1 * a, 1 * a, 1f * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0.15 + oz_ + fz)
                        .tex(s / 20 + ox, s / diversion * i * per + oy)
                        .color(1 * a, 1 * a, 1 * a, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0.15 + oz)
                        .tex(s / 20 + ox, (s + s * i) / diversion * per + oy).color(1 * a, 1 * a, 1 * a, 0 * a)
                        .endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0 + oz)
                        .tex(0 + ox, (s + s * i) / diversion * per + oy)
                        .color(1 * a, 1 * a, 1 * a, 0f * a).endVertex();
            } else {
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0 + oz_ - fz)
                        .tex(0 + ox, s / diversion * i * per + oy)
                        .color(1 * a, 1 * a, 1 * a, 1f * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0.15 + oz_ + fz)
                        .tex(s / 20 + ox, s / diversion * i * per + oy)
                        .color(1 * a, 1 * a, 1 * a, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0.15 + oz + fz)
                        .tex(s / 20 + ox, (s + s * i) / diversion * per + oy).color(1 * a, 1 * a, 1 * a, 0 * a)
                        .endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0 + oz - fz)
                        .tex(0 + ox, (s + s * i) / diversion * per + oy).color(1 * a, 1 * a, 1 * a, 1f * a)
                        .endVertex();
            }
        }
        tessellator.draw();
        Minecraft.getMinecraft().getTextureManager()
                .bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/white.png"));
        GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
        float light = 0.85f;
        tessellator.getBuffer().begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < diversion; i++) {
            float oz = (float) Math.sin(3.14 * 2.5 * wind * per / diversion * i) * tp + es_joint[i + 1]
                    + strafing_joint[i + 1];
            float oz_ = (float) Math.sin(3.14 * 2.5 * wind * per / diversion * (i - 1)) * tp + es_joint[i]
                    + strafing_joint[i];
            float fz = 0.1f / diversion * i * per;
            float a = alpha2 * (diversion - (i + 1) * alpha2) / diversion;
            if (a < 0) {
                a = 0;
            }
            if (i + 1 >= diversion) {
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0 + oz_ - fz).tex(0, 0)
                        .color(1, 1, 1, 1f * a)
                        .endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0.1 + oz_ + fz).tex(1, 0)
                        .color(light, light, light, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0.1 + oz).tex(1, 1)
                        .color(light, light, light, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0 + oz).tex(0, 1)
                        .color(1, 1, 1, 0f * a)
                        .endVertex();
            } else {
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0 + oz_ - fz).tex(0, 0)
                        .color(1, 1, 1, 1f * a)
                        .endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i], step * i * per, 0.1 + oz_ + fz).tex(1, 0)
                        .color(light, light, light, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0.1 + oz + fz).tex(1, 1)
                        .color(light, light, light, 0 * a).endVertex();
                tessellator.getBuffer().pos(0 + forward_joint[i + 1], (step + step * i) * per, 0 + oz - fz).tex(0, 1)
                        .color(1, 1, 1, 1f * a).endVertex();
            }
        }
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.enableLighting();
    }

    public void drawThirdGun(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player,
            ItemStack demoStack) {
        // boolean sneakFlag = false;
        // if (player != null && player.isSneaking()) {
        // sneakFlag = true;
        // }
        drawThirdGun(renderPlayer, renderType, player, demoStack, false);
    }

    public void drawThirdGun(RenderLivingBase renderPlayer, RenderType renderType, EntityLivingBase player,
            ItemStack demoStack, boolean sneakFlag) {
        if (!(demoStack.getItem() instanceof ItemGun))
            return;
        GunType gunType = ((ItemGun) demoStack.getItem()).type;
        if (gunType == null) {
            return;
        }
        ModelEnhancedGun model;
            if (renderType == RenderType.ITEMFRAME || renderType == RenderType.ITEMLOOT) {
                String key = renderType.serializedName;
                if(!thirdPersonModels.containsKey(key) || thirdPersonModels.get(key).baseType != gunType) {
                    ModelEnhancedGun newModel = new ModelEnhancedGun((GunEnhancedRenderConfig)gunType.enhancedModel.config, gunType);
                    newModel.model = gunType.enhancedModel.model;
                    thirdPersonModels.put(key, newModel);
                }
                model = (ModelEnhancedGun)thirdPersonModels.get(key);
            } else {
                model = getOrCreateModel(gunType, false, player != null ? player.getUniqueID() : null);
            }
        GunEnhancedRenderConfig config = (GunEnhancedRenderConfig) model.config;
        AnimationController controller;
        EnhancedStateMachine anim = ClientRenderHooks.getEnhancedAnimMachine(player);
        // 这里可以考虑一下 是否可以对骨骼枪做一些优化
        if (player != null) {
            controller = AnimationController.getController(player, config);
            if (controller.getPlayingAnimation() == AnimationType.DEFAULT
                    || controller.getPlayingAnimation() == AnimationType.PRE_FIRE
                    || controller.getPlayingAnimation() == AnimationType.FIRE
                    || controller.getPlayingAnimation() == AnimationType.POST_FIRE) {
                model.updateAnimation(controller.getTime(), true);
            } else {
                model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS),
                        true);
            }
        } else {
            model.updateAnimation((float) config.animations.get(AnimationType.DEFAULT).getStartTime(config.FPS), true);
        }

        HashSet<String> exceptParts = new HashSet<String>();
        if (config.specialEffect.flashModelGroups != null) {
            config.specialEffect.flashModelGroups.forEach((g) -> {
                exceptParts.add(g.name);
            });
        }
        exceptParts.addAll(config.defaultHidePart);
        if (renderType == RenderType.PLAYER_OFFHAND) {
            exceptParts.addAll(config.thirdHideOffhandPart);
            exceptParts.removeAll(config.thirdShowOffhandPart);
        } else {
            exceptParts.addAll(config.thirdHidePart);
            exceptParts.removeAll(config.thirdShowPart);
        }
        // exceptParts.addAll(DEFAULT_EXCEPT);

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

        if (demoStack.hasTagCompound()) {
            ItemStack loadedAmmo = new ItemStack(demoStack.getTagCompound().getCompoundTag("ammo"));
            if (loadedAmmo.getItem() instanceof ItemAmmo) {
                ItemAmmo itemAmmo = (ItemAmmo) loadedAmmo.getItem();
                AmmoType ammoType = itemAmmo.type;
                if (config.attachment.containsKey(ammoType.internalName)) {
                    if (config.attachment.get(ammoType.internalName).hidePart != null) {
                        exceptParts.addAll(config.attachment.get(ammoType.internalName).hidePart);
                    }
                    if (config.attachment.get(ammoType.internalName).showPart != null) {
                        exceptParts.removeAll(config.attachment.get(ammoType.internalName).showPart);
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

        /**
         * gun
         */
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
         */
        boolean flagDynamicAmmoRendered = false;
        ItemStack stackAmmo = ItemStack.EMPTY;
        ItemStack bulletStack = ItemStack.EMPTY;
        if (demoStack.hasTagCompound()) {
            stackAmmo = new ItemStack(demoStack.getTagCompound().getCompoundTag("ammo"));
        }
        ItemStack orignalAmmo = stackAmmo;
        // stackAmmo=controller.getRenderAmmo(stackAmmo);
        ItemStack renderAmmo = stackAmmo;
        boolean defaultAmmoFlag = true;

        VarBoolean defaultBulletFlag = new VarBoolean();
        defaultBulletFlag.b = true;
        int currentAmmoCount = 0;
        int costAmmoCount = 0;

        if (gunType.acceptedBullets != null && demoStack.hasTagCompound()) {
            currentAmmoCount = demoStack.getTagCompound().getInteger("ammocount");
            bulletStack = new ItemStack(demoStack.getTagCompound().getCompoundTag("bullet"));
            costAmmoCount = gunType.internalAmmoStorage - currentAmmoCount;
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
                    for (int bullet = 0; bullet < currentAmmoCount
                            && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                        int renderBullet = bullet;
                        model.applyGlobalTransformToOther("bulletModel_" + bullet, () -> {
                            ClientProxy.gunEnhancedRenderer.renderAttachment(config, "bullet", bulletType.internalName,
                                    () -> {
                                        bulletType.model.renderPart("bulletModel", worldScale);
                                    });
                        });
                    }
                    for (int bullet = 0; bullet < costAmmoCount
                            && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                        int renderBullet = bullet;
                        model.applyGlobalTransformToOther("shellModel_" + bullet, () -> {
                            ClientProxy.gunEnhancedRenderer.renderAttachment(config, "shell", bulletType.internalName,
                                    () -> {
                                        bulletType.shell.renderPart("shellModel", worldScale);
                                    });
                        });
                    }
                    model.applyGlobalTransformToOther("bulletModel", () -> {
                        ClientProxy.gunEnhancedRenderer.renderAttachment(config, "bullet", bulletType.internalName,
                                () -> {
                                    bulletType.model.renderPart("bulletModel", worldScale);
                                });
                    });
                    defaultBulletFlag.b = false;
                }
            }
        }
        ItemStack[] ammoList = new ItemStack[] { stackAmmo };
        String[] binddings = new String[] { "ammoModel" };
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
                                        // be careful, don't mod the config
                                        EnhancedRenderConfig.Transform ammoTransform = config.attachment
                                                .get(itemAmmo.type.internalName).multiMagazineTransform
                                                .get(renderAmmo.getTagCompound().getInteger("magcount") - 1);
                                        EnhancedRenderConfig.Transform renderTransform = ammoTransform;

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
         */

        ClientProxy.gunEnhancedRenderer.bindTexture("guns", gunPath);

        if (defaultBulletFlag.b) {
            for (int bullet = 0; bullet < currentAmmoCount && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                model.renderPart("bulletModel_" + bullet);
            }
            for (int bullet = 0; bullet < costAmmoCount && bullet < RenderGunEnhanced.BULLET_MAX_RENDER; bullet++) {
                model.renderPart("shellModel_" + bullet);
            }
        }

        if (!renderAmmo.isEmpty() && defaultAmmoFlag) {
            model.renderPart("ammoModel");
        }

        /**
         * attachment
         */

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
                                    AnimationType currentAction = null;
                                    if (anim != null && anim.controller != null) {
                                        currentAction = anim.controller.getPlayingAnimation();
                                    }
                                    if(attachment==AttachmentPresetEnum.Laser) {
                                        boolean shouldRenderLaser = false;
                                        if(player != null) {
                                            UUID playerID = player.getUniqueID();
                                            shouldRenderLaser = LaserRenderManager.getInstance().getLaserState(playerID);
                                        } else {
                                            shouldRenderLaser = ((ItemGun)demoStack.getItem()).getLaserEnabled(demoStack);
                                        }
                                        
                                        if(shouldRenderLaser) {
                                            AttachmentRenderConfig.Laser laserConfig = attachmentModel.config.laser;
                                            float bx = OpenGlHelper.lastBrightnessX;
                                            float by = OpenGlHelper.lastBrightnessY;
                                            ClientProxy.gunEnhancedRenderer.renderLaserModel(laserConfig, attachmentModel, bx, by, worldScale, currentAction, shouldRenderLaser);
                                        }
                                    }
                                });
                    });
                }
            }
        }

        ObjModelRenderer.glowTxtureMode = glowTxtureMode;

        /**
         * flashmodel
         */
        boolean shouldRenderFlash = true;
        if ((GunType.getAttachment(demoStack, AttachmentPresetEnum.Barrel) != null)) {
            AttachmentType attachmentType = ((ItemAttachment) GunType
                    .getAttachment(demoStack, AttachmentPresetEnum.Barrel).getItem()).type;
            if (attachmentType.attachmentType == AttachmentPresetEnum.Barrel) {
                shouldRenderFlash = !attachmentType.barrel.hideFlash;
            }
        }

        float bx = OpenGlHelper.lastBrightnessX;
        float by = OpenGlHelper.lastBrightnessY;

        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
        if (shouldRenderFlash && anim.shooting && anim.getShootingAnimationType().showFlashModel()
                && !player.isInWater()) {
            GlStateManager.pushMatrix();
            ItemStack itemStack = GunType.getAttachment(demoStack, AttachmentPresetEnum.Barrel);
            if (itemStack != null && itemStack.getItem() != Items.AIR) {
                AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                if (config.attachment.containsKey(attachmentType.internalName)) {
                    if (config.attachment.get(attachmentType.internalName).flashModelOffset != null) {
                        GlStateManager.translate(
                                config.attachment.get(attachmentType.internalName).flashModelOffset.x,
                                config.attachment.get(attachmentType.internalName).flashModelOffset.y,
                                config.attachment.get(attachmentType.internalName).flashModelOffset.z);
                    }
                }
            }
            TextureType flashType = gunType.flashType;
            bindTexture(flashType.resourceLocations.get(anim.flashCount % flashType.resourceLocations.size()));
            if (config.specialEffect.oldFlashModel) {
                model.renderPart("flashModel");
            }
            if (config.specialEffect.flashModelGroups != null) {
                config.specialEffect.flashModelGroups.forEach((group) -> {
                    if (renderType == RenderType.PLAYER_OFFHAND) {
                        if (config.thirdHideOffhandPart.contains(group.name)) {
                            return;
                        }
                    } else {
                        if (config.thirdHidePart.contains(group.name)) {
                            return;
                        }
                    }
                    model.renderPart(group.name);
                });
            }
            GlStateManager.popMatrix();
        }
        GlStateManager.enableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, bx, by);
        model.renderPart("translucentModel");
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    public void writeScopeGlassDepth(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming,
            float worldScale, boolean mask) {
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
        GL43.glCopyImageSubData(ClientProxy.scopeUtils.blurFramebuffer.framebufferTexture, GL_TEXTURE_2D, 0, 0, 0, 0,
                ScopeUtils.SCOPE_MASK_TEX, GL_TEXTURE_2D, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight, 1);
    }

    /**
     * 以SCOPE_MASK_TEX为遮罩 将深度改为cofnig.eraseScopeDepth
     ***/
    @SideOnly(Side.CLIENT)
    public void eraseScopeGlassDepth(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming,
            float worldScale) {
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
                ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(),
                        resolution.getScaledHeight(), 1, 1);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.alphaFunc(GL11.GL_GEQUAL, 0.1f);
                GL11.glPopAttrib();

                if (ScopeUtils.isRenderHand0) {
                    GL20.glUseProgram(Programs.depthProgram);
                    GlStateManager.bindTexture(ClientProxy.scopeUtils.DEPTH_ERASE_TEX);
                    ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1,
                            resolution.getScaledWidth(), resolution.getScaledHeight(), 1, 1);
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
                ClientProxy.scopeUtils.drawScaledCustomSizeModalRectFlipY(0, 0, 0, 0, 1, 1, resolution.getScaledWidth(),
                        resolution.getScaledHeight(), 1, 1);
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
    public void renderScopeGlass(AttachmentType attachmentType, ModelAttachment modelAttachment, boolean isAiming,
            float worldScale) {
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

                GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0,
                        GL_TEXTURE_2D, ScopeUtils.OVERLAY_TEX, 0);
                GlStateManager.clearColor(0, 0, 0, 0);
                GL11.glClearColor(0, 0, 0, 0);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.depthMask(true);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                copyDepthBuffer();
                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);

                // GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                modelAttachment.renderOverlaySolid(worldScale);

                GL20.glUseProgram(0);
                if (OptifineHelper.isShadersEnabled()) {
                    Shaders.popProgram();
                }

                GlStateManager.tryBlendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA,
                        SourceFactor.ONE, DestFactor.ZERO);
                GlStateManager.color(1.0f, 1.0f, 1.0f, alpha);
                if (attachmentType.sight.usedDefaultOverlayModelTexture) {
                    renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/black.png"));
                }
                // 必要的colormask(2023.3.26又注:今天看起来是莫名其妙)
                GlStateManager.colorMask(true, true, true, true);
                modelAttachment.renderOverlay(worldScale);
                GlStateManager.colorMask(true, true, true, true);
                GlStateManager.disableBlend();
                // GlStateManager.enableLighting();

                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GL30.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0,
                        GL_TEXTURE_2D, tex, 0);
                GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
                copyDepthBuffer();
                ClientProxy.scopeUtils.blurFramebuffer.bindFramebuffer(false);
                GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

                GlStateManager.colorMask(true, true, true, false);
                GlStateManager.disableBlend();
                // 忘记这玩意有什么用了 好像和镜面的光照渲染有关系
                renderWorldOntoScope(attachmentType, modelAttachment, worldScale, false);
                GlStateManager.enableBlend();
                GlStateManager.colorMask(true, true, true, true);

                ContextCapabilities contextCapabilities = GLContext.getCapabilities();
                if (contextCapabilities.OpenGL43) {
                    GL43.glCopyImageSubData(tex, GL_TEXTURE_2D, 0, 0, 0, 0, ScopeUtils.SCOPE_LIGHTMAP_TEX,
                            GL_TEXTURE_2D, 0, 0, 0, 0, mc.displayWidth, mc.displayHeight, 1);

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
        GL30.glBlitFramebuffer(0, 0, mc.displayWidth, mc.displayHeight, 0, 0, mc.displayWidth, mc.displayHeight,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager.colorMask(true, true, true, true);
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, GL11.GL_NONE);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, GL11.GL_NONE);
    }

    @SideOnly(Side.CLIENT)
    private void renderWorldOntoScope(AttachmentType type, ModelAttachment modelAttachment, float worldScale,
            boolean isLightOn) {
        GL11.glPushMatrix();

        if (isLightOn) {
            renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID, "textures/skins/white.png"));
            GL11.glDisable(2896);
            Minecraft.getMinecraft().entityRenderer.disableLightmap();
            // ModelGun.glowOn(1);
            modelAttachment.renderScope(worldScale);
            // ModelGun.glowOff();
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
         * Minecraft mc = Minecraft.getMinecraft();
         * if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) != null &&
         * mc.gameSettings.thirdPersonView == 0) {
         * if (mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem()
         * instanceof ItemGun) {
         * final ItemStack gunStack =
         * mc.player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
         * if (GunType.getAttachment(gunStack, AttachmentPresetEnum.Flashlight) != null)
         * {
         * if (isLightOn) {
         * GL11.glDisable(2896);
         * Minecraft.getMinecraft().entityRenderer.disableLightmap();
         * GL11.glDisable(3042);
         * GL11.glPushMatrix();
         * GL11.glPushAttrib(16384);
         * GL11.glEnable(3042);
         * GL11.glDepthMask(false);
         * GL11.glBlendFunc(774, 770);
         * 
         * renderEngine.bindTexture(new ResourceLocation(ModularWarfare.MOD_ID,
         * "textures/gui/light.png"));
         * modelAttachment.renderOverlay(worldScale);
         * 
         * GL11.glBlendFunc(770, 771);
         * GL11.glDepthMask(true);
         * GL11.glDisable(3042);
         * GL11.glPopAttrib();
         * GL11.glPopMatrix();
         * GL11.glEnable(2896);
         * Minecraft.getMinecraft().entityRenderer.enableLightmap();
         * }
         * }
         * }
         * }
         */
        GL11.glPopMatrix();
    }

    public void renderAttachment(GunEnhancedRenderConfig config, String type, String name, Runnable run) {
        renderAttachment(config, type, name, null, run);
    }
    
    public void renderAttachment(GunEnhancedRenderConfig config, String type, String name, ItemStack gunStack, Runnable run) {

        String handguardName = null;
        if (gunStack != null && GunType.getAttachment(gunStack, AttachmentPresetEnum.Handguard) != null) {
            ItemStack handguardStack = GunType.getAttachment(gunStack, AttachmentPresetEnum.Handguard);
            if (handguardStack.getItem() instanceof ItemAttachment) {
                handguardName = ((ItemAttachment) handguardStack.getItem()).type.internalName;
            }
        }
        

        if (config.attachmentGroup.containsKey(type)) {
            GunEnhancedRenderConfig.AttachmentGroup group = config.attachmentGroup.get(type);
            applyTransform(group);
            if (handguardName != null && group.handguardInfluence.containsKey(handguardName)) {
                applyTransform(group.handguardInfluence.get(handguardName));
            }
        }
        

        if (config.attachment.containsKey(name)) {
            GunEnhancedRenderConfig.Attachment attachment = config.attachment.get(name);
            applyTransform(attachment);
            if (handguardName != null && attachment.handguardInfluence.containsKey(handguardName)) {
                applyTransform(attachment.handguardInfluence.get(handguardName));
            }
        }
        
        run.run();
    }

    public void applyTransform(EnhancedRenderConfig.Transform transform) {
        GlStateManager.translate(transform.translate.x, transform.translate.y, transform.translate.z);
        GlStateManager.scale(transform.scale.x, transform.scale.y, transform.scale.z);
        GlStateManager.rotate(transform.rotate.y, 0, 1, 0);
        GlStateManager.rotate(transform.rotate.x, 1, 0, 0);
        GlStateManager.rotate(transform.rotate.z, 0, 0, 1);
    }

    public void blendTransform(ModelEnhancedGun model, ItemStack gunStack, boolean basicSprint, float time,
            float sprintTime,
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
                        sprint: {
                            org.joml.Matrix4f begin_transform = mat;
                            mchhui.hegltf.DataAnimation.Transform end_transform = model.findLocalTransform(node.name,
                                    sprintTime);
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
                EnhancedRenderConfig.ObjectControl cfg = ((GunEnhancedRenderConfig) model.config).objectControl.get(node.name);
                if (cfg != null) {
                    float per = ammoPerParam;
                    if (!cfg.progress) {
                        per = 1 - per;
                    }
                    // System.out.println(per);
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

    // 4x4 floats
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
    
    private void applyHandguardInfluenceToAim(GunEnhancedRenderConfig config, GunEnhancedRenderConfig.Attachment sightConfig,
                                               String handguardName, Vector3f aimPosOffset, Vector3f aimRotOffset) {
        // 配件组级别影响
        if (config.attachmentGroup.containsKey(AttachmentPresetEnum.Sight.typeName)) {
            GunEnhancedRenderConfig.AttachmentGroup sightGroup = config.attachmentGroup.get(AttachmentPresetEnum.Sight.typeName);
            GunEnhancedRenderConfig.HandguardInfluence groupInfluence = sightGroup.handguardInfluence.get(handguardName);
            if (groupInfluence != null) {
                aimPosOffset.sub(groupInfluence.translate.x, groupInfluence.translate.y, groupInfluence.translate.z);
                aimRotOffset.sub(groupInfluence.rotate.x, groupInfluence.rotate.y, groupInfluence.rotate.z);
            }
        }
        
        // 具体配件级别影响
        GunEnhancedRenderConfig.HandguardInfluence attachInfluence = sightConfig.handguardInfluence.get(handguardName);
        if (attachInfluence != null) {
            aimPosOffset.sub(attachInfluence.translate.x, attachInfluence.translate.y, attachInfluence.translate.z);
            aimRotOffset.sub(attachInfluence.rotate.x, attachInfluence.rotate.y, attachInfluence.rotate.z);
        }
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
