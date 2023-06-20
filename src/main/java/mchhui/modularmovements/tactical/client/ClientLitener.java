package mchhui.modularmovements.tactical.client;

import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.type.BaseItem;
import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.network.TacticalHandler;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.entity.EntityPlayerSPHelper;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.PlayerSPPushOutOfBlocksEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class ClientLitener {
    public static KeyBinding sit = new KeyBinding("Sit/Sliding", 46, "ModularMovements");
    public static KeyBinding crawling = new KeyBinding("Crawling", 44, "ModularMovements");
    public static KeyBinding leftProbe = new KeyBinding("Left Probe", 16, "ModularMovements");
    public static KeyBinding rightProbe = new KeyBinding("Right Probe", 18, "ModularMovements");

    public static PlayerState clientPlayerState = new PlayerState.ClientPlayerState();
    public static Map<Integer, PlayerState> ohterPlayerStateMap = new HashMap<Integer, PlayerState>();

    public static Vec3d clientPlayerSitMoveVec3d = new Vec3d(0, 0, 0);
    public static double clientPlayerSitMoveAmplifierCharging = 0;
    public static double clientPlayerSitMoveAmplifierCharged = 0;
    public static double clientPlayerSitMoveAmplifier = 0;

    public static boolean enableForceGravity = false;
    public static double forceGravity = 1.0;
    public static double clientPlayerSitMoveAmplifierUser = 0.8;
    public static double clientPlayerSitMoveLess = 0.1;
    public static double clientPlayerSitMoveMax = 1.0;
    public static int crawlingMousePosXMove = 0;
    public static double cameraOffsetY = 0;
    public static float cameraProbeOffset = 0;
    private static World world;
    private static boolean sitKeyLock = false;
    private static boolean crawlingKeyLock = false;
    private static boolean probeKeyLock = false;
    private static boolean isSneaking = false;
    private static boolean wannaSliding = false;
    private static long lastSyncTime = 0;
    private static Field speedInAir;
    private static AxisAlignedBB lastAABB;
    private static AxisAlignedBB lastModAABB;

    public static Vec3d onGetPositionEyes(EntityPlayer player, float partialTicks, Vec3d vec3d) {
        PlayerState state = null;
        float offest = 0;
        if (player == Minecraft.getMinecraft().player) {
            state = clientPlayerState;
            offest = cameraProbeOffset;
        } else {
            if (!ohterPlayerStateMap.containsKey(player.getEntityId())) {
                return vec3d;
            }
            state = ohterPlayerStateMap.get(player.getEntityId());
            offest = state.probeOffset;
        }

        if (offest != 0) {
            return vec3d.add(new Vec3d(offest * -0.6, 0, 0)
                    .rotateYaw((float) (-Minecraft.getMinecraft().player.rotationYaw * Math.PI / 180f)));
        }
        return vec3d;
    }

    public static boolean applyRotations(RenderLivingBase renderer, EntityLivingBase entityLiving, float p_77043_2_,
                                         float rotationYaw, float partialTicks) {
        if (entityLiving == Minecraft.getMinecraft().player && entityLiving.isEntityAlive()) {
            if (clientPlayerState.isSitting) {
                GlStateManager.translate(0, -0.5, 0);
            }
            if (clientPlayerState.isCrawling) {
                GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.translate(0, -1.3, 0.1);
                GlStateManager.translate(cameraProbeOffset * 0.4, 0, 0);
                return true;
            }
            if (cameraProbeOffset != 0) {
                GlStateManager.rotate(180.0F - entityLiving.rotationYawHead, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(cameraProbeOffset * 0.1, 0, 0);
                GlStateManager.rotate(180.0F - entityLiving.rotationYawHead, 0.0F, -1.0F, 0.0F);
                GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(cameraProbeOffset * -20.0F, 0, 0, 1);
                return true;
            }
        }
        if (entityLiving != Minecraft.getMinecraft().player && entityLiving instanceof EntityPlayer
                && entityLiving.isEntityAlive()) {
            if (ohterPlayerStateMap.containsKey(entityLiving.getEntityId())) {
                PlayerState state = ohterPlayerStateMap.get(entityLiving.getEntityId());
                if (state.isSitting) {
                    GlStateManager.translate(0, -0.5, 0);
                }
                if (state.isCrawling) {
                    GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                    GlStateManager.translate(0, -1.3, 0.1);
                    GlStateManager.translate(state.probeOffset * 0.4, 0, 0);
                    return true;
                }
                state.updateOffset();
                if (state.probeOffset != 0) {
                    GlStateManager.rotate(180.0F - entityLiving.rotationYawHead, 0.0F, 1.0F, 0.0F);
                    GlStateManager.translate(state.probeOffset * 0.1, 0, 0);
                    GlStateManager.rotate(180.0F - entityLiving.rotationYawHead, 0.0F, -1.0F, 0.0F);
                    GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);
                    GlStateManager.rotate(state.probeOffset * -20.0F, 0, 0, 1);
                    return true;
                }
            }
        }
        return false;
    }

    public static void onMouseMove(MouseHelper mouseHelper) {
        if (clientPlayerState.probe != 0 && ModularMovements.CONFIG.lean.mouseCorrection) {
            Vec3d vec = Vec3d.ZERO.addVector(mouseHelper.deltaX, 0, mouseHelper.deltaY);
            vec = vec.rotateYaw((float) (cameraProbeOffset * 10 * Math.PI / 180d));
            mouseHelper.deltaX = Math.round((float) vec.x);
            mouseHelper.deltaY = Math.round((float) vec.z);
        }
        if (clientPlayerState.isCrawling & ModularMovements.CONFIG.crawl.blockView) {
            float angle = (float) (ModularMovements.CONFIG.crawl.blockAngle * Math.PI);
            if (Math.abs(crawlingMousePosXMove + mouseHelper.deltaX) > angle) {
                if (mouseHelper.deltaX > 0) {
                    mouseHelper.deltaX = (int) (angle - crawlingMousePosXMove);
                } else {
                    mouseHelper.deltaX = (int) (-angle - crawlingMousePosXMove);
                }
            }
            crawlingMousePosXMove += mouseHelper.deltaX;
        }
    }

    public static void setRotationAngles(ModelPlayer model, float limbSwing, float limbSwingAmount, float ageInTicks,
                                         float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        setRotationAngles((ModelBiped) model, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch,
                scaleFactor, entityIn);
        model.copyModelAngles(model.bipedLeftLeg, model.bipedLeftLegwear);
        model.copyModelAngles(model.bipedRightLeg, model.bipedRightLegwear);
        model.copyModelAngles(model.bipedLeftArm, model.bipedLeftArmwear);
        model.copyModelAngles(model.bipedRightArm, model.bipedRightArmwear);
        model.copyModelAngles(model.bipedBody, model.bipedBodyWear);
        model.copyModelAngles(model.bipedHead, model.bipedHeadwear);
    }

    public static void setRotationAngles(ModelBiped model, float limbSwing, float limbSwingAmount, float ageInTicks,
                                         float netHeadYaw, float headPitch, float scaleFactor, Entity entityIn) {
        if (entityIn instanceof EntityPlayer && entityIn.isEntityAlive()) {
            PlayerState state = null;
            float offest = 0;
            if (entityIn == Minecraft.getMinecraft().player) {
                state = clientPlayerState;
                offest = cameraProbeOffset;
            } else {
                if (!ohterPlayerStateMap.containsKey(entityIn.getEntityId())) {
                    return;
                }
                state = ohterPlayerStateMap.get(entityIn.getEntityId());
                offest = state.probeOffset;
            }

            if (state.isSitting) {
                model.bipedRightLeg.rotateAngleX = -1.4137167F;
                model.bipedRightLeg.rotateAngleY = ((float) Math.PI / 10F);
                model.bipedRightLeg.rotateAngleZ = 0.07853982F;
                model.bipedLeftLeg.rotateAngleX = -1.4137167F;
                model.bipedLeftLeg.rotateAngleY = -((float) Math.PI / 10F);
                model.bipedLeftLeg.rotateAngleZ = -0.07853982F;
            }

            if (state.isCrawling) {
                model.bipedHead.rotateAngleX -= 70 * 3.14 / 180;
                model.bipedRightArm.rotateAngleX *= 0.2;
                model.bipedLeftArm.rotateAngleX *= 0.2;
                model.bipedRightArm.rotateAngleX += 180 * 3.14 / 180;
                model.bipedLeftArm.rotateAngleX += 180 * 3.14 / 180;
                if (entityIn instanceof AbstractClientPlayer) {
                    ItemStack itemstack = ((AbstractClientPlayer) entityIn).getHeldItemMainhand();
                    if (itemstack != ItemStack.EMPTY && !itemstack.isEmpty()) {
                        if (ModularMovements.mwfEnable) {
                            if (itemstack.getItem() instanceof BaseItem) {
                                model.bipedLeftArm.rotateAngleY = 0;
                                model.bipedRightArm.rotateAngleY = 0;
                                model.bipedLeftArm.rotateAngleX = (float) (180 * 3.14 / 180);
                                model.bipedRightArm.rotateAngleX = (float) (180 * 3.14 / 180);
                            }
                        }
                    }
                }
                model.bipedRightLeg.rotateAngleX *= 0.2;
                model.bipedLeftLeg.rotateAngleX *= 0.2;
            }
            if (offest >= 0) {
                model.bipedRightLeg.rotateAngleZ += offest * 20 * 3.14 / 180;
            } else {
                model.bipedLeftLeg.rotateAngleZ += offest * 20 * 3.14 / 180;
            }
        }
    }

    private static boolean isButtonDown(int id) {
        try {
            if (id < 0) {
                return Mouse.isButtonDown(id + 100);
            }
            return Keyboard.isKeyDown(id);
        } catch (IndexOutOfBoundsException err) {

        }
        return false;
    }

    private static boolean isButtonsDownAll(int... id) {
        boolean flag = true;
        for (int i = 0; i < id.length; i++) {
            flag = flag && isButtonDown(id[i]);
            if (!flag) {
                break;
            }
        }
        return flag;
    }

    private static boolean isButtonsDownOne(int... id) {
        boolean flag = false;
        for (int i = 0; i < id.length; i++) {
            flag = flag || isButtonDown(id[i]);
            if (flag) {
                break;
            }
        }
        return flag;
    }

    public static boolean isSitting(Integer id) {
        if (!ohterPlayerStateMap.containsKey(id)) {
            return false;
        }
        return ohterPlayerStateMap.get(id).isSitting;
    }

    public static boolean isCrawling(Integer id) {
        if (!ohterPlayerStateMap.containsKey(id)) {
            return false;
        }
        return ohterPlayerStateMap.get(id).isCrawling;
    }

    @SubscribeEvent
    public void onTickClient(ClientTickEvent event) {
        if (event.phase == Phase.START) {
            return;
        }
        if (Minecraft.getMinecraft().world != world) {
            clientPlayerSitMoveAmplifier = 0;
            cameraOffsetY = 0;
            cameraProbeOffset = 0;
            clientPlayerSitMoveAmplifierCharging = 0;
            clientPlayerSitMoveAmplifierCharged = 0;
            clientPlayerState.reset();
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.setPrimaryHand(Minecraft.getMinecraft().gameSettings.mainHand);
            }
        }
        if (Minecraft.getMinecraft().player != null) {
            if (Minecraft.getMinecraft().player.posX != Minecraft.getMinecraft().player.lastTickPosX) {
                crawlingMousePosXMove = 0;
            }
            if (Minecraft.getMinecraft().player.posZ != Minecraft.getMinecraft().player.lastTickPosZ) {
                crawlingMousePosXMove = 0;
            }
            if (Minecraft.getMinecraft().player.isSprinting() && !clientPlayerState.isSitting) {
                clientPlayerSitMoveAmplifierCharging += 1.0 / (20.0 * 1.0);
                clientPlayerSitMoveAmplifierCharging = Math.min(clientPlayerSitMoveAmplifierCharging, 1);
            } else {
                clientPlayerSitMoveAmplifierCharging = 0;
            }
        }
        world = Minecraft.getMinecraft().world;
    }

    public void onFMLInit(FMLInitializationEvent event) {
        ClientRegistry.registerKeyBinding(sit);
        ClientRegistry.registerKeyBinding(crawling);
        ClientRegistry.registerKeyBinding(leftProbe);
        ClientRegistry.registerKeyBinding(rightProbe);
        speedInAir = ReflectionHelper.findField(EntityPlayer.class, "speedInAir", "field_71102_ce");
    }

    public void onFMLInitPost(FMLPostInitializationEvent event) {
        Field field = ReflectionHelper.findField(RenderManager.class, "skinMap", "field_178636_l");
        try {
            Map<String, RenderPlayer> skinMap = (Map<String, RenderPlayer>) field
                    .get(Minecraft.getMinecraft().getRenderManager());
            skinMap.clear();
            skinMap.put("default", new FakeRenderPlayer(Minecraft.getMinecraft().getRenderManager()));
            skinMap.put("slim", new FakeRenderPlayer(Minecraft.getMinecraft().getRenderManager(), true));
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        field = ReflectionHelper.findField(Minecraft.class, "tutorial", "field_193035_aW");
        try {
            field.set(Minecraft.getMinecraft(), new FakeTutorial(Minecraft.getMinecraft()));
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Minecraft.getMinecraft().getTutorial().reload();
    }

    private void onSit() {
        EntityPlayer clientPlayer = Minecraft.getMinecraft().player;
        if (clientPlayer == null) {
            return;
        }
        if (Minecraft.getMinecraft().currentScreen != null) {
            return;
        }

        //sit
        if (clientPlayerState.canSit()) {
            if ((!sitKeyLock && isButtonDown(sit.getKeyCode())) || wannaSliding) {
                if (!clientPlayerState.isSitting || wannaSliding) {
                    if (Minecraft.getMinecraft().player.onGround) {
                        sitKeyLock = true;
                        wannaSliding = false;
                        AxisAlignedBB axisalignedbb;
                        float w1 = clientPlayer.width / 2;
                        if (clientPlayerState.isCrawling) {
                            w1 *= 1.2f;
                        }
                        axisalignedbb = new AxisAlignedBB(clientPlayer.posX - w1, clientPlayer.posY + 0.1,
                                clientPlayer.posZ - w1, clientPlayer.posX + w1, clientPlayer.posY + 1.2,
                                clientPlayer.posZ + w1);
                        if (!clientPlayer.world.collidesWithAnyBlock(axisalignedbb)) {
                            if (!clientPlayerState.isSitting) {
                                clientPlayerState.enableSit();
                            }

                            if (Minecraft.getMinecraft().player.isSprinting() && ModularMovements.CONFIG.slide.enable) {
                                if (wannaSliding) {
                                    clientPlayerSitMoveAmplifierCharging = 1;
                                }
                                clientPlayerSitMoveAmplifierCharged = clientPlayerSitMoveAmplifierCharging;
                                clientPlayerSitMoveAmplifier = ModularMovements.CONFIG.slide.maxForce;
                                clientPlayerSitMoveVec3d = new Vec3d(clientPlayer.posX - clientPlayer.lastTickPosX, 0,
                                        clientPlayer.posZ - clientPlayer.lastTickPosZ).normalize();
                            }
                        }
                    }
                } else {
                    sitKeyLock = true;
                    double d0 = 0.3;
                    if (!clientPlayer.world.collidesWithAnyBlock(
                            new AxisAlignedBB(clientPlayer.posX - d0, clientPlayer.posY, clientPlayer.posZ - d0,
                                    clientPlayer.posX + d0, clientPlayer.posY + 1.8, clientPlayer.posZ + d0))) {
                        clientPlayerSitMoveAmplifier = 0;
                        clientPlayerState.disableSit();
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyInput(KeyInputEvent event) {
        EntityPlayer clientPlayer = Minecraft.getMinecraft().player;
        //sit
        onSit();

        //crawling
        if (clientPlayerState.canCrawl()) {
            if (!crawlingKeyLock && isButtonDown(crawling.getKeyCode())) {
                crawlingKeyLock = true;
                if (!clientPlayerState.isCrawling) {
                    if (Minecraft.getMinecraft().player.onGround) {
                        clientPlayerState.enableCrawling();
                        if (Minecraft.getMinecraft().player.isSprinting()) {
                            Vec3d vec3d = new Vec3d(clientPlayer.posX - clientPlayer.lastTickPosX, 0,
                                    clientPlayer.posZ - clientPlayer.lastTickPosZ).normalize();
                            Minecraft.getMinecraft().player.motionX = vec3d.x * clientPlayerSitMoveAmplifierCharging;
                            Minecraft.getMinecraft().player.motionY = 0.35 * clientPlayerSitMoveAmplifierCharging;
                            Minecraft.getMinecraft().player.motionZ = vec3d.z * clientPlayerSitMoveAmplifierCharging;
                        }
                    }
                } else {
                    double d0 = 0.3;
                    if (!clientPlayer.world.collidesWithAnyBlock(
                            new AxisAlignedBB(clientPlayer.posX - d0, clientPlayer.posY, clientPlayer.posZ - d0,
                                    clientPlayer.posX + d0, clientPlayer.posY + 1.8, clientPlayer.posZ + d0))) {
                        clientPlayerState.disableCrawling();
                    }
                }
            }
        }

        if (ModularMovements.CONFIG.lean.withGunsOnly) {
            if (Loader.isModLoaded("modularwarfare")) {
                if (Minecraft.getMinecraft().player.getHeldItemMainhand() != null) {
                    if (!(Minecraft.getMinecraft().player.getHeldItemMainhand().getItem() instanceof ItemGun)) {
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = false)
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        isSneaking = event.getEntityPlayer().isSneaking();
        if (!clientPlayerState.isCrawling && !clientPlayerState.isSitting) {
            return;
        }
        if (event.getEntityPlayer() instanceof EntityPlayerSP) {
            ((EntityPlayerSP) event.getEntityPlayer()).movementInput.sneak = false;
        } else {
            event.getEntityPlayer().setSneaking(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (event.getEntityPlayer() instanceof EntityPlayerSP) {
            ((EntityPlayerSP) event.getEntityPlayer()).movementInput.sneak = isSneaking;
        } else {
            event.getEntityPlayer().setSneaking(isSneaking);
        }
    }

    @SubscribeEvent
    public void onCameraUpdate(CameraSetup event) {
        float pitch = event.getPitch();
        float yaw = event.getYaw();
        double playerPosX = Minecraft.getMinecraft().player.posX + (Minecraft.getMinecraft().player.posX - Minecraft.getMinecraft().player.lastTickPosX) * event.getRenderPartialTicks();
        double playerPosY = Minecraft.getMinecraft().player.posY + (Minecraft.getMinecraft().player.posY - Minecraft.getMinecraft().player.lastTickPosY) * event.getRenderPartialTicks();
        double playerPosZ = Minecraft.getMinecraft().player.posZ + (Minecraft.getMinecraft().player.posZ - Minecraft.getMinecraft().player.lastTickPosZ) * event.getRenderPartialTicks();

        if (clientPlayerState.probe != 0) {
            float f = 0.22f;
            float f1 = 0.2f;
            float f2 = 0.15f;
            Vec3d vec3d = null;
            if (clientPlayerState.probe == -1) {
                vec3d = new Vec3d(0.6, 0, 0)
                        .rotateYaw((float) (-(yaw - 180) * Math.PI / 180f));
            }
            if (clientPlayerState.probe == 1) {
                vec3d = new Vec3d(-0.6, 0, 0)
                        .rotateYaw((float) (-(yaw - 180) * Math.PI / 180f));
            }
            AxisAlignedBB axisalignedbb = Minecraft.getMinecraft().player.getEntityBoundingBox();
            int a = (int) (Math.sqrt((Minecraft.getMinecraft().player.posX - Minecraft.getMinecraft().player.lastTickPosX) * (Minecraft.getMinecraft().player.posX - Minecraft.getMinecraft().player.lastTickPosX) + (Minecraft.getMinecraft().player.posZ - Minecraft.getMinecraft().player.lastTickPosZ) * (Minecraft.getMinecraft().player.posZ - Minecraft.getMinecraft().player.lastTickPosZ)) / 0.1f);
            int testCount = 10 + a;
            for (int i = 0; i <= testCount; i++) {
                axisalignedbb = new AxisAlignedBB(
                        playerPosX + vec3d.x * (i / (float) testCount) - f,
                        playerPosY + 0.6f,
                        playerPosZ + vec3d.z * (i / (float) testCount) - f,
                        playerPosX + vec3d.x * (i / (float) testCount) + f,
                        playerPosY + Minecraft.getMinecraft().player.getEyeHeight() + f1,
                        playerPosZ + vec3d.z * (i / (float) testCount) + f);
                if (Minecraft.getMinecraft().player.world.collidesWithAnyBlock(axisalignedbb)) {
                    clientPlayerState.resetProbe();
                }
            }
        }
        if (cameraProbeOffset != 0) {
            float f = 0.22f;
            float f1 = 0.2f;
            float f2 = 0.1f;
            Vec3d vec3d = null;
            vec3d = new Vec3d(-0.6, 0, 0).rotateYaw((float) (-(yaw - 180) * Math.PI / 180f));
            int testCount = 10;
            for (int i = 0; i <= testCount; i++) {
                AxisAlignedBB axisalignedbb = Minecraft.getMinecraft().player.getEntityBoundingBox();
                axisalignedbb = new AxisAlignedBB(playerPosX + vec3d.x * (cameraProbeOffset) * (i / (float) testCount) - f,
                        playerPosY + Minecraft.getMinecraft().player.getEyeHeight() - f1,
                        playerPosZ + vec3d.z * (cameraProbeOffset) * (i / (float) testCount) - f,
                        playerPosX + vec3d.x * (cameraProbeOffset) * (i / (float) testCount) + f,
                        playerPosY + Minecraft.getMinecraft().player.getEyeHeight() + f1,
                        playerPosZ + vec3d.z * (cameraProbeOffset) * (i / (float) testCount) + f);
                axisalignedbb = axisalignedbb.grow(f2 - f, 0, f2 - f);
                if (Minecraft.getMinecraft().player.world.collidesWithAnyBlock(axisalignedbb)) {
                    clientPlayerState.resetProbe();
                    cameraProbeOffset = 0;
                }
            }
        }

        if (clientPlayerSitMoveAmplifier > 0) {
            if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
                float partialTicks = (float) event.getRenderPartialTicks();
                EntityPlayer entityplayer = (EntityPlayer) Minecraft.getMinecraft().getRenderViewEntity();
                float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
                float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
                float f2 = entityplayer.prevCameraYaw
                        + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
                float f3 = entityplayer.prevCameraPitch
                        + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
                GlStateManager.rotate(f3, -1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, -1.0F, 0.0F,
                        0.0F);
                GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, -1.0F);
                GlStateManager.translate(-MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F,
                        Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            }
        }
        event.setPitch(0);
        event.setYaw(0);
        event.setRoll(0);
        GlStateManager.translate(-0.6 * cameraProbeOffset, 0, 0);
        GlStateManager.rotate(10 * cameraProbeOffset, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0, -cameraOffsetY, 0);
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);

        EntityPlayer player = Minecraft.getMinecraft().player;
        if (clientPlayerState.isSitting) {
            if (player.eyeHeight != 1.1f) {
                cameraOffsetY = player.eyeHeight - 1.1f;
                player.eyeHeight = 1.1f;
            }
        } else if (clientPlayerState.isCrawling) {

            if (player.eyeHeight != 0.7f) {
                cameraOffsetY = player.eyeHeight - 0.7f;
                player.eyeHeight = 0.7f;
            }
        } else if (player.eyeHeight == 0.7f) {
            cameraOffsetY = player.eyeHeight - player.getDefaultEyeHeight();
            player.eyeHeight = player.getDefaultEyeHeight();
        } else if (player.eyeHeight == 1.1f) {
            cameraOffsetY = player.eyeHeight - player.getDefaultEyeHeight();
            player.eyeHeight = player.getDefaultEyeHeight();
        }

        double amplifer = (Minecraft.getSystemTime() - lastSyncTime) * (60 / 1000d);
        lastSyncTime = Minecraft.getSystemTime();
        if (clientPlayerState.probe == -1) {
            if (cameraProbeOffset > -1) {
                cameraProbeOffset -= 0.1 * amplifer;
            }
            if (cameraProbeOffset < -1) {
                cameraProbeOffset = -1;
            }
        }
        if (clientPlayerState.probe == 1) {
            if (cameraProbeOffset < 1) {
                cameraProbeOffset += 0.1 * amplifer;
            }
            if (cameraProbeOffset > 1) {
                cameraProbeOffset = 1;
            }
        }

        if (clientPlayerState.probe == 0) {
            if (Math.abs(cameraProbeOffset) <= 0.1 * amplifer) {
                cameraProbeOffset = 0;
            }
            if (cameraProbeOffset < 0) {
                cameraProbeOffset += 0.1 * amplifer;
            }
            if (cameraProbeOffset > 0) {
                cameraProbeOffset -= 0.1 * amplifer;
            }
        }
        if (Math.abs(cameraOffsetY) <= 0.1 * amplifer) {
            cameraOffsetY = 0;
        }
        if (cameraOffsetY < 0) {
            cameraOffsetY += 0.1 * amplifer;
        }
        if (cameraOffsetY > 0) {
            cameraOffsetY -= 0.1 * amplifer;
        }

    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event) {
        EntityPlayer clientPlayer = Minecraft.getMinecraft().player;

        if (clientPlayerState.isSitting) {
            event.getMovementInput().moveForward *= 0.3;
            event.getMovementInput().moveStrafe *= 0.3;
            if (event.getMovementInput().jump) {
                clientPlayerSitMoveAmplifier = 0;
            }
            if (event.getMovementInput().moveForward != 0) {
                if (isButtonDown(Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode())) {
                    if (clientPlayerSitMoveAmplifier < clientPlayerSitMoveLess * 2
                            && !Minecraft.getMinecraft().player.isSneaking()) {
                        double d0 = 0.3;
                        if (!clientPlayer.world.collidesWithAnyBlock(
                                new AxisAlignedBB(clientPlayer.posX - d0, clientPlayer.posY, clientPlayer.posZ - d0,
                                        clientPlayer.posX + d0, clientPlayer.posY + 1.8, clientPlayer.posZ + d0))) {
                            clientPlayerState.disableSit();
                        }
                    }
                }
            }
        }

        if (clientPlayerState.isCrawling) {
            event.getMovementInput().moveForward *= 0.4;
            event.getMovementInput().moveStrafe *= 0.4;
            if (event.getMovementInput().jump) {
                event.getMovementInput().jump = false;
                double d0 = 0.3;
                if (!clientPlayer.world.collidesWithAnyBlock(
                        new AxisAlignedBB(clientPlayer.posX - d0, clientPlayer.posY, clientPlayer.posZ - d0,
                                clientPlayer.posX + d0, clientPlayer.posY + 1.8, clientPlayer.posZ + d0))) {
                    clientPlayerState.disableCrawling();
                    clientPlayerState.disableSit();
                }
            }
        }

        if (clientPlayerState.probe != 0) {
            event.getMovementInput().moveForward *= 0.9;
            event.getMovementInput().moveStrafe *= 0.9;
        }

        if (Minecraft.getMinecraft().player != null) {
            try {
                speedInAir.set(Minecraft.getMinecraft().player, 0.02f);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent
    public void onTickRender(RenderTickEvent event) {
        if (event.phase == Phase.START) {
            if (!isButtonDown(sit.getKeyCode())) {
                sitKeyLock = false;
            }
            if (isButtonDown(sit.getKeyCode())) {
                if (Minecraft.getMinecraft().player != null) {
                    if (Minecraft.getMinecraft().player.fallDistance > 1) {
                        wannaSliding = true;
                    }
                }
            }
            onSit();
            if (Minecraft.getMinecraft().player != null && Minecraft.getMinecraft().currentScreen == null) {
                if (clientPlayerState.canProbe() && !Minecraft.getMinecraft().player.isRiding() && !Minecraft.getMinecraft().player.isElytraFlying()) {
                    if (!probeKeyLock && isButtonDown(leftProbe.getKeyCode())) {
                        probeKeyLock = true;
                        if (clientPlayerState.probe != -1) {
                            clientPlayerState.leftProbe();
                        } else {
                            clientPlayerState.resetProbe();
                        }
                    }

                    if (!probeKeyLock && isButtonDown(rightProbe.getKeyCode())) {
                        probeKeyLock = true;
                        if (clientPlayerState.probe != 1) {
                            clientPlayerState.rightProbe();
                        } else {
                            clientPlayerState.resetProbe();
                        }
                    }
                }
            }

            if (!isButtonDown(crawling.getKeyCode())) {
                crawlingKeyLock = false;
            }

            if (!isButtonDown(leftProbe.getKeyCode()) && !isButtonDown(rightProbe.getKeyCode())) {
                probeKeyLock = false;
                if (!ModularMovements.CONFIG.lean.autoHold && clientPlayerState.probe != 0) {
                    clientPlayerState.resetProbe();
                }
            } else if (isButtonDown(leftProbe.getKeyCode())) {
                if (!ModularMovements.CONFIG.lean.autoHold && clientPlayerState.probe != -1) {
                    probeKeyLock = false;
                }
            } else if (isButtonDown(rightProbe.getKeyCode())) {
                if (!ModularMovements.CONFIG.lean.autoHold && clientPlayerState.probe != 1) {
                    probeKeyLock = false;
                }
            }
            if (Minecraft.getMinecraft().player != null) {
                if (Minecraft.getMinecraft().player.isRiding() || Minecraft.getMinecraft().player.isElytraFlying()) {
                    clientPlayerSitMoveAmplifier = 0;
                    if (clientPlayerState.isSitting) {
                        clientPlayerState.disableSit();
                    }
                    if (clientPlayerState.isCrawling) {
                        clientPlayerState.disableCrawling();
                    }
                    clientPlayerState.resetProbe();
                }
            }

        }
    }

    //@SubscribeEvent
    public void onBlockPush(PlayerSPPushOutOfBlocksEvent event) {
        if (cameraProbeOffset != 0) {
            //event.setCanceled(true);
            Vec3d vec3d = new Vec3d(-0.6, 0, 0)
                    .rotateYaw((float) (-(event.getEntityPlayer().rotationYaw - 180) * Math.PI / 180f))
                    .scale(-cameraProbeOffset);
            EntityPlayerSPHelper.pushOutOfBlocks((EntityPlayerSP) event.getEntityPlayer(),
                    event.getEntityPlayer().posX + vec3d.x - (double) event.getEntityPlayer().width * 0.35D,
                    lastModAABB.minY + 0.5D,
                    event.getEntityPlayer().posZ + vec3d.z + (double) event.getEntityPlayer().width * 0.35D);
            EntityPlayerSPHelper.pushOutOfBlocks((EntityPlayerSP) event.getEntityPlayer(),
                    event.getEntityPlayer().posX + vec3d.x - (double) event.getEntityPlayer().width * 0.35D,
                    lastModAABB.minY + 0.5D,
                    event.getEntityPlayer().posZ + vec3d.z - (double) event.getEntityPlayer().width * 0.35D);
            EntityPlayerSPHelper.pushOutOfBlocks((EntityPlayerSP) event.getEntityPlayer(),
                    event.getEntityPlayer().posX + vec3d.x + (double) event.getEntityPlayer().width * 0.35D,
                    lastModAABB.minY + 0.5D,
                    event.getEntityPlayer().posZ + vec3d.z - (double) event.getEntityPlayer().width * 0.35D);
            EntityPlayerSPHelper.pushOutOfBlocks((EntityPlayerSP) event.getEntityPlayer(),
                    event.getEntityPlayer().posX + vec3d.x + (double) event.getEntityPlayer().width * 0.35D,
                    lastModAABB.minY + 0.5D,
                    event.getEntityPlayer().posZ + vec3d.z + (double) event.getEntityPlayer().width * 0.35D);
        }
    }

    @SubscribeEvent
    public void onTickPlayer(PlayerTickEvent event) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }
        if (event.player != Minecraft.getMinecraft().player) {
            return;
        }
        if (!event.player.isEntityAlive()) {
            clientPlayerSitMoveAmplifier = 0;
            if (clientPlayerState.isSitting) {
                clientPlayerState.disableSit();
            }
            if (clientPlayerState.isCrawling) {
                clientPlayerState.disableCrawling();
            }
        }
        if (event.phase != Phase.END) {
            if (lastAABB != null && event.player.getEntityBoundingBox() == lastModAABB) {
                event.player.setEntityBoundingBox(lastAABB);
            }

            if (event.player.isSneaking()) {
                clientPlayerSitMoveAmplifier = 0;
            }
            if (clientPlayerSitMoveAmplifier > 0) {
                if (clientPlayerState.isSitting) {
                    if (event.player == Minecraft.getMinecraft().player) {
                        if (enableForceGravity && event.player.motionY <= 0) {
                            event.player.motionY = -forceGravity;
                        }
                        event.player.motionX = clientPlayerSitMoveVec3d.x * clientPlayerSitMoveAmplifier
                                * clientPlayerSitMoveAmplifierUser * clientPlayerSitMoveAmplifierCharged;
                        event.player.motionZ = clientPlayerSitMoveVec3d.z * clientPlayerSitMoveAmplifier
                                * clientPlayerSitMoveAmplifierUser * clientPlayerSitMoveAmplifierCharged;
                        if (Minecraft.getMinecraft().player.onGround) {
                            Minecraft.getMinecraft().player
                                    .playSound(SoundEvents.BLOCK_GRASS_HIT,
                                            2 * (float) (clientPlayerSitMoveAmplifier
                                                    * clientPlayerSitMoveAmplifierCharged / ModularMovements.CONFIG.slide.maxForce),
                                            0.8f);
                        }
                    }
                }
                clientPlayerSitMoveAmplifier -= clientPlayerSitMoveLess;
                if (!event.player.onGround && event.player.fallDistance > 1 && !event.player.isInWater()) {
                    clientPlayerSitMoveAmplifier += clientPlayerSitMoveLess;
                }
                if (clientPlayerSitMoveAmplifier <= 0) {
                    if (ModularMovements.CONFIG.sit.autoHold) {
                        if (!isButtonDown(sit.getKeyCode())) {
                            clientPlayerState.disableSit();
                        }
                    } else {
                        if (isButtonDown(sit.getKeyCode())) {
                            clientPlayerState.disableSit();
                        }
                    }
                }
            }
            return;
        }
        float f = event.player.width;
        float f1 = event.player.height;
        if (clientPlayerState.isSitting) {
            f1 = 1.2f;
        } else if (clientPlayerState.isCrawling) {
            f1 = 0.8f;
        }
        if (f != event.player.width || f1 != event.player.height) {
            AxisAlignedBB axisalignedbb = event.player.getEntityBoundingBox();
            axisalignedbb = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
                    axisalignedbb.minX + (double) f, axisalignedbb.minY + (double) f1, axisalignedbb.minZ + (double) f);

            if (!event.player.world.collidesWithAnyBlock(axisalignedbb)) {
                try {
                    ServerListener.setSize.invoke(event.player, f, f1);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        Vec3d vec3d = new Vec3d(-0.6, 0, 0).rotateYaw((float) (-(event.player.rotationYaw - 180) * Math.PI / 180f));
        lastAABB = event.player.getEntityBoundingBox();
        lastModAABB = event.player.getEntityBoundingBox().offset(vec3d.scale(-cameraProbeOffset));
        event.player.setEntityBoundingBox(lastModAABB);

        if (clientPlayerSitMoveAmplifier > 0) {
            TacticalHandler.sendNoStep(100);
        }
        if (event.player.fallDistance > 1) {
            if (wannaSliding) {
                //TacticalHandler.sendNoFall();
            }
        }
        TacticalHandler.sendToServer(clientPlayerState.writeCode());
    }

    @SubscribeEvent
    public void onTickOtherPlayer(PlayerTickEvent event) {
        if (event.side == Side.CLIENT) {
            if (event.phase != Phase.END) {
                PlayerState state = ohterPlayerStateMap.get(event.player.getEntityId());
                if (state != null) {
                    if (state.lastAABB != null && event.player.getEntityBoundingBox() == state.lastModAABB) {
                        event.player.setEntityBoundingBox(state.lastAABB);
                    }
                }
            } else {
                float f = event.player.width;
                float f1 = event.player.height;
                if (isSitting(event.player.getEntityId())) {
                    f1 = 1.2f;
                } else if (isCrawling(event.player.getEntityId())) {
                    f1 = 0.8f;
                }
                PlayerState state = ohterPlayerStateMap.get(event.player.getEntityId());
                float cameraProbeOffset = 0;
                if (state != null) {
                    cameraProbeOffset = state.probeOffset;
                }
                if (f != event.player.width || f1 != event.player.height) {
                    AxisAlignedBB axisalignedbb = event.player.getEntityBoundingBox();
                    axisalignedbb = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY, axisalignedbb.minZ,
                            axisalignedbb.minX + (double) f, axisalignedbb.minY + (double) f1,
                            axisalignedbb.minZ + (double) f);

                    if (!event.player.world.collidesWithAnyBlock(axisalignedbb)) {
                        try {
                            ServerListener.setSize.invoke(event.player, f, f1);
                        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (state != null) {
                    Vec3d vec3d = new Vec3d(-0.6, 0, 0).rotateYaw((float) (-(event.player.rotationYaw - 180) * Math.PI / 180f));
                    state.lastAABB = event.player.getEntityBoundingBox();
                    state.lastModAABB = state.lastAABB.offset(vec3d.scale(-cameraProbeOffset));
                    event.player.setEntityBoundingBox(state.lastModAABB);
                }
            }
        }
    }

    @SubscribeEvent
    public void onHandBobing(RenderHandEvent event) {
        if (clientPlayerSitMoveAmplifier > 0) {
            if (Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer) {
                float partialTicks = event.getPartialTicks();
                EntityPlayer entityplayer = (EntityPlayer) Minecraft.getMinecraft().getRenderViewEntity();
                float f = entityplayer.distanceWalkedModified - entityplayer.prevDistanceWalkedModified;
                float f1 = -(entityplayer.distanceWalkedModified + f * partialTicks);
                float f2 = entityplayer.prevCameraYaw
                        + (entityplayer.cameraYaw - entityplayer.prevCameraYaw) * partialTicks;
                float f3 = entityplayer.prevCameraPitch
                        + (entityplayer.cameraPitch - entityplayer.prevCameraPitch) * partialTicks;
                GlStateManager.rotate(f3, -1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 5.0F, -1.0F, 0.0F,
                        0.0F);
                GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 3.0F, 0.0F, 0.0F, -1.0F);
                GlStateManager.translate(-MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.5F,
                        Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2), 0.0F);
            }
        }
    }

    @SubscribeEvent
    public void onPlaySoundAtEntity(PlaySoundAtEntityEvent event) {
        if (Minecraft.getMinecraft().player != null) {
            if (event.getEntity() == Minecraft.getMinecraft().player) {
                if (clientPlayerSitMoveAmplifier > 0) {
                    if (SoundEvent.REGISTRY.getNameForObject(event.getSound()).toString().contains("step")) {
                        event.setVolume(0);
                    }
                }
            }
        }
    }
}
