package com.modularwarfare.melee.client.animation;

import com.google.common.base.Predicate;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.gui.api.GuiUtils;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.melee.client.RenderMelee;
import com.modularwarfare.melee.client.animation.MeleeStateMachine.Phase;
import com.modularwarfare.melee.client.configs.AnimationMeleeType;
import com.modularwarfare.melee.client.configs.MeleeRenderConfig;
import com.modularwarfare.melee.common.melee.ItemMelee;
import com.modularwarfare.melee.common.melee.MeleeType;
import com.modularwarfare.melee.common.melee.MeleeType.AnimationInfo;
import com.modularwarfare.melee.comon.PacketBounced;
import com.modularwarfare.melee.comon.PacketSwing;
import com.modularwarfare.utility.maths.Interpolation;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;

public class AnimationMeleeController {

    public static final float rayStepFactor = 0.5f;
    public static double DEFAULT;

    public static double DRAW;

    public static double INSPECT = 1;

    public static double ATTACK = 1;

    public static double HEAVYATTACK = 1;

    public double SPRINT;
    public double SPRINT_LOOP; 
    public double SPRINT_RANDOM;
    public long sprintCoolTime = 0;
    public long sprintLoopCoolTime = 0;

    public boolean isJumping=false;

    public static MeleeStateMachine stateMachine = new MeleeStateMachine();

    public static int currentOrder = 0;
    public static boolean keepOrder = false;
    public static int oldCurrentItem;
    public static ItemStack oldItemstack;
    public static boolean nextResetDefault = false;
    public MeleeRenderConfig config;
    public MeleeType type;
    private ActionPlaybackMelee playback;

    public AnimationMeleeController(MeleeRenderConfig config, MeleeType type) {
        this.config = config;
        this.type = type;
        this.playback = new ActionPlaybackMelee(config);
        this.playback.action = AnimationMeleeType.DEFAULT;
        currentOrder = 0;
        // currentRandomAnim.put(AnimationMeleeType.DRAW, 0);
        // currentRandomAnim.put(AnimationMeleeType.INSPECT, 0);
        // currentRandomAnim.put(AnimationMeleeType.PREATTACK, 0);
        // currentRandomAnim.put(AnimationMeleeType.ATTACK, 0);
        // currentRandomAnim.put(AnimationMeleeType.ATTACKSEC, 0);
        // currentRandomAnim.put(AnimationMeleeType.POSTATTACK, 0);
        // currentRandomAnim.put(AnimationMeleeType.PREHEAVYATTACK, 0);
        // currentRandomAnim.put(AnimationMeleeType.HEAVYATTACK, 0);
        // currentRandomAnim.put(AnimationMeleeType.HEAVYATTACKSEC, 0);
        // currentRandomAnim.put(AnimationMeleeType.POSTATTACK, 0);
    }

    public void reset(boolean resetSprint) {
        DEFAULT = 0;
        DRAW = 0;
        INSPECT = 1;
        ATTACK = 1;
        HEAVYATTACK = 1;

        if(resetSprint) {
            SPRINT=0;
        }
        SPRINT_LOOP=0;

        currentOrder = 0;
        this.stateMachine.reset();
        updateActionAndTime();
    }

    public void resetView() {
        INSPECT = 1;
        DRAW = 1;
    }

    public void onUpdate() {
        if (Minecraft.getMinecraft().player == null)
            return;
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        // if(this.ATTACK.)
        if (item instanceof ItemMelee) {
            switch (this.stateMachine.attackPhase) {
                case PRE:
                    break;
                case FIRST:
                    if (this.ATTACK < 1) {
                        ArrayList<EntityLivingBase> result = areaCheck(false);
                        if (result == null) {
                            this.stateMachine.attackPhase = this.stateMachine.lastAttackPhase = Phase.POST;
                            this.stateMachine.bounced = true;
                            this.ATTACK = 0;
                            if (this.stateMachine.isHeavy) {
                                ((ItemMelee) item).type.playClientSound(Minecraft.getMinecraft().player,
                                        WeaponSoundType.MeleeBouncedHeavy);
                                AnimationInfo ai = ((ItemMelee) Minecraft.getMinecraft().player.getHeldItemMainhand()
                                        .getItem()).type.getAnimationInfo(AnimationMeleeType.HEAVYATTACK,
                                                RenderMelee.controller.currentOrder);
                                ModularWarfare.NETWORK.sendToServer(new PacketBounced(ai.animationName));
                                ModularWarfare.NETWORK.sendToServer(new PacketSwing(ai.checkBounced.animationName));
                            } else {
                                ((ItemMelee) item).type.playClientSound(Minecraft.getMinecraft().player,
                                        WeaponSoundType.MeleeBounced);
                                AnimationInfo ai = ((ItemMelee) Minecraft.getMinecraft().player.getHeldItemMainhand()
                                        .getItem()).type.getAnimationInfo(AnimationMeleeType.ATTACK,
                                                RenderMelee.controller.currentOrder);
                                ModularWarfare.NETWORK.sendToServer(new PacketBounced(ai.animationName));
                                ModularWarfare.NETWORK.sendToServer(new PacketSwing(ai.checkBounced.animationName));
                            }
                        }
                    }
                    break;
                case SECOND:
                case POST:
                default:
                    break;

            }
            // if(result!=null) {
            // for(EntityLiving entityLiving:result) {
            //
            // }
            // }
        }

    }

    public ArrayList<EntityLivingBase> areaCheck(boolean includeEntities) {
        Minecraft mc = Minecraft.getMinecraft();
        float partialTicks = mc.getRenderPartialTicks();
        ArrayList<EntityLivingBase> result = new ArrayList();
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        boolean isHeavy = false;
        MeleeType meleeType = ((ItemMelee) item).type;
        AnimationMeleeType animation = getPlayingAnimation();
        if (animation == null)
            return result;
        AnimationInfo ai = meleeType.getAnimationInfo(animation, currentOrder);
        if (ai == null)
            return result;
        float startYaw = ai.yawStart;
        float endYaw = ai.yawAngle;
        float startPitch = ai.pitchStart;
        float endPitch = ai.pitchAngle;
        float attackRange = ai.range;
        float attackYawStep = ai.yawStep;
        float attackPitchStep = ai.pitchStep;
        boolean canBounce = ai.canBounced;
        // bounce check
        if (canBounce && !includeEntities) {
            startYaw = ai.checkBounced.yawStart;
            endYaw = ai.checkBounced.yawAngle;
            startPitch = ai.checkBounced.pitchStart;
            endPitch = ai.checkBounced.pitchAngle;
            attackRange = ai.checkBounced.range;
            attackYawStep = -1;
            attackPitchStep = -1;
        }
        ArrayList<Vec3d[]> rays = getRays(startYaw, endYaw, startPitch, endPitch, attackRange, attackYawStep,
                attackPitchStep);
        List<Entity> list = null;
        if (includeEntities) {
            list = Minecraft.getMinecraft().player.world.getEntitiesWithinAABBExcludingEntity(null,
                    getAttackBoundingBox(Minecraft.getMinecraft().player, null, 1, attackRange, 1.2f));
        }
        ArrayList<EntityLivingBase> inRangeEntityList = new ArrayList();
        if (list != null) {
            for (Entity entity : list) {
                if (entity instanceof EntityLivingBase && entity != Minecraft.getMinecraft().player) {
                    inRangeEntityList.add((EntityLivingBase) entity);
                }
            }
        }
        for (Vec3d[] ray : rays) {
            RayTraceResult traceResult = forwardsRaycast(inRangeEntityList, ray[0], ray[1], partialTicks,
                    includeEntities, includeEntities, Minecraft.getMinecraft().player);
            if (traceResult != null) {
                if (canBounce && !includeEntities && traceResult.typeOfHit == RayTraceResult.Type.BLOCK) {
                    return null;
                } else if (traceResult.typeOfHit == RayTraceResult.Type.ENTITY) {
                    if (!result.contains(traceResult.entityHit)) {
                        result.add((EntityLivingBase) traceResult.entityHit);
                        if (ai.attackPenetration)
                            inRangeEntityList.remove(traceResult.entityHit);
                    }
                }
            }
        }
        return result;
    }

    public ArrayList<Vec3d[]> getRays(float yawBegin, float yawEnd, float pitchBegin, float pitchEnd, float distance,
            float yawStep, float pitchStep) {
        ArrayList<Vec3d[]> result = new ArrayList();
        Minecraft mc = Minecraft.getMinecraft();
        float partialTicks = mc.getRenderPartialTicks();
        if (yawStep < 0)
            yawStep = (int) (Math.abs(yawEnd - yawBegin) * rayStepFactor);
        if (pitchStep < 0)
            pitchStep = (int) (Math.abs(pitchEnd - pitchBegin) * rayStepFactor);
        for (int i = 0; i < (int) yawStep + 1; i++) {
            for (int j = 0; j < (int) pitchStep + 1; j++) {
                Vec3d center = Minecraft.getMinecraft().player.getPositionEyes(partialTicks);
                Vec3d vector = getVectorForRotation(0, 90 - (float) (yawBegin + (yawEnd * (i / yawStep))));//
                vector = vector.rotatePitch((float) (-90 + pitchBegin + (pitchEnd * (j / pitchStep))) * 0.017453292F);
                vector = vector.rotatePitch((-Minecraft.getMinecraft().player.rotationPitch) * 0.017453292F);
                vector = vector.rotateYaw((-Minecraft.getMinecraft().player.rotationYaw) * 0.017453292F);

                Vec3d target = center.add(vector.x * distance, vector.y * distance, vector.z * distance);
                result.add(new Vec3d[] { center, target });
            }
        }
        return result;
    }

    public AxisAlignedBB getAttackBoundingBox(EntityPlayer player, Vec3d offset, float partialTicks, float distance,
            float scale) {
        Vec3d playerpe = player.getPositionEyes(partialTicks);
        if (offset != null) {
            playerpe = playerpe.add(offset);
        }
        return new AxisAlignedBB(playerpe.x, playerpe.y, playerpe.z, playerpe.x, playerpe.y, playerpe.z)
                .grow(distance * scale, distance * scale, distance * scale);
    }

    public RayTraceResult forwardsRaycast(List<EntityLivingBase> inRangeEntityList, Vec3d begin, Vec3d end,
            float partialTicks, boolean includeEntities, boolean ignoreExcludedEntity, Entity excludedEntity) {
        double d0 = begin.x;
        double d1 = begin.y;
        double d2 = begin.z;
        double d3 = end.x;
        double d4 = end.y;
        double d5 = end.z;
        World world = Minecraft.getMinecraft().player.world;
        Vec3d vec3d = new Vec3d(d0, d1, d2);
        Vec3d vec3d1 = new Vec3d(d3, d4, d5);
        RayTraceResult raytraceresult = world.rayTraceBlocks(vec3d, vec3d1, false, true, false);

        if (includeEntities) {
            if (raytraceresult != null) {
                vec3d1 = new Vec3d(raytraceresult.hitVec.x, raytraceresult.hitVec.y, raytraceresult.hitVec.z);
                if (!includeEntities) {
                    return raytraceresult;
                }
            }

            Entity entity = null;
            Vec3d eyePos = Minecraft.getMinecraft().player.getPositionEyes(partialTicks);
            AxisAlignedBB rayArea = new AxisAlignedBB(eyePos.x, eyePos.y, eyePos.z, vec3d1.x, vec3d1.y, vec3d1.z)
                    .grow(0.1D);
            // GuiUtils.renderBoundingBox(rayArea);

            ArrayList<EntityLivingBase> entityLivingbaseInsideAABB = new ArrayList();

            // List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null,
            // rayArea);
            for (EntityLivingBase entityLivingbase : inRangeEntityList) {
                if (entityLivingbase.getEntityBoundingBox().intersects(rayArea))// && entity != entityIn
                {
                    Predicate<? super Entity> filter = EntitySelectors.NOT_SPECTATING;
                    if (filter == null || filter.apply(entityLivingbase)) {
                        entityLivingbaseInsideAABB.add(entityLivingbase);
                    }
                    Entity[] aentity = entityLivingbase.getParts();

                    if (aentity != null) {
                        for (Entity entity1 : aentity) {// entity1 != entityIn &&
                            if (entity1.getEntityBoundingBox().intersects(rayArea)
                                    && (filter == null || filter.apply(entity1))) {
                                entityLivingbaseInsideAABB.add(entityLivingbase);
                            }
                        }
                    }
                }
            }

            double d6 = 0.0D;

            for (int i = 0; i < entityLivingbaseInsideAABB.size(); ++i) {
                Entity entity1 = entityLivingbaseInsideAABB.get(i);

                if (entity1.canBeCollidedWith() && (ignoreExcludedEntity || !entity1.isEntityEqual(excludedEntity))
                        && !entity1.noClip) {
                    AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(0.10000001192092896D);// 0.30000001192092896D
                    RayTraceResult raytraceresult1 = axisalignedbb.calculateIntercept(vec3d, vec3d1);

                    if (raytraceresult1 != null) {
                        double d7 = vec3d.squareDistanceTo(raytraceresult1.hitVec);

                        if (d7 < d6 || d6 == 0.0D) {
                            entity = entity1;
                            d6 = d7;
                        }
                    }
                }
            }

            if (entity != null) {
                raytraceresult = new RayTraceResult(entity);
            }
        }

        return raytraceresult;
    }

    public void debugAttackAreaDraw() {
        if (this.ATTACK == 1) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        float partialTicks = mc.getRenderPartialTicks();
        double renderPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double) partialTicks;
        double renderPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double) partialTicks;
        double renderPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double) partialTicks;
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        if (item instanceof ItemMelee) {
            boolean isHeavy = false;
            MeleeType meleeType = ((ItemMelee) item).type;
            AnimationMeleeType animation = getPlayingAnimation();
            if (animation == null)
                return;
            AnimationInfo ai = meleeType.getAnimationInfo(animation, currentOrder);
            if (ai == null)
                return;
            float startYaw = ai.yawStart;
            float endYaw = ai.yawAngle;
            float startPitch = ai.pitchStart;
            float endPitch = ai.pitchAngle;
            float attackRange = ai.range;
            float attackYawStep = ai.yawStep;
            float attackPitchStep = ai.pitchStep;
            boolean canBounce = ai.canBounced;

            ArrayList<Vec3d[]> rays = getRays(startYaw, endYaw, startPitch, endPitch, attackRange, attackYawStep,
                    attackPitchStep);
            Vec3d offset = new Vec3d(-renderPosX, -renderPosY, -renderPosZ);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.disableTexture2D();
            // GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
            // GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GlStateManager.color(1, 1, 1, 1f);
            GlStateManager.pushMatrix();
            GlStateManager.glLineWidth(10);
            GuiUtils.renderBoundingBox(getAttackBoundingBox(player, offset, partialTicks, attackRange, 1.2f));
            GlStateManager.translate(offset.x, offset.y, offset.z);

            GlStateManager.popMatrix();
            GlStateManager.enableTexture2D();
            GlStateManager.enableAlpha();
            GlStateManager.disableBlend();

            for (Vec3d[] ray : rays) {
                Vec3d center = ray[0].add(offset);
                Vec3d target = ray[1].add(offset);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                // GlStateManager.matrixMode(5888);
                GlStateManager.enableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.disableTexture2D();
                // GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                // GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.color(1, 0, 0, 0.8f);
                GlStateManager.pushMatrix();
                GlStateManager.glLineWidth(2);
                // GlStateManager.scale(100, 100, 100);
                bufferbuilder.begin(1, DefaultVertexFormats.POSITION);
                bufferbuilder.pos(center.x, center.y, center.z).endVertex();
                bufferbuilder.pos(target.x, target.y, target.z).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
            }
            if (canBounce) {
                startYaw = ai.checkBounced.yawStart;
                endYaw = ai.checkBounced.yawAngle;
                startPitch = ai.checkBounced.pitchStart;
                endPitch = ai.checkBounced.pitchAngle;
                attackRange = ai.checkBounced.range;
                attackYawStep = -1;
                attackPitchStep = -1;
            }
            rays = getRays(startYaw, endYaw, startPitch, endPitch, attackRange, attackYawStep, attackPitchStep);
            for (Vec3d[] ray : rays) {
                Vec3d center = ray[0].add(offset);
                Vec3d target = ray[1].add(offset);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder bufferbuilder = tessellator.getBuffer();
                // GlStateManager.matrixMode(5888);
                GlStateManager.enableBlend();
                GlStateManager.disableAlpha();
                GlStateManager.disableTexture2D();
                // GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                // GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                GlStateManager.color(0, 1, 0, 0.8f);
                GlStateManager.pushMatrix();
                GlStateManager.glLineWidth(2);
                // GlStateManager.scale(100, 100, 100);
                bufferbuilder.begin(1, DefaultVertexFormats.POSITION);
                bufferbuilder.pos(center.x, center.y, center.z).endVertex();
                bufferbuilder.pos(target.x, target.y, target.z).endVertex();
                tessellator.draw();
                GlStateManager.popMatrix();
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                GlStateManager.disableBlend();
            }
        }
    }

    public Vec3d getVectorForRotation(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * 0.017453292F - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * 0.017453292F - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * 0.017453292F);
        float f3 = MathHelper.sin(-pitch * 0.017453292F);
        return new Vec3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public void onTickRender(float stepTick) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player != null && !(player.getHeldItemMainhand().getItem() instanceof ItemMelee)) {
            return;
        }
        stateMachine.onRenderTickUpdate(stepTick);
        /** DEFAULT **/
        double defaultSpeed = config.meleeAnimations.get(AnimationMeleeType.DEFAULT).get(0).getSpeed(config.FPS) * stepTick;
        DEFAULT = Math.max(0F, DEFAULT + defaultSpeed);
        if (DEFAULT > 1) {
            DEFAULT = 0;
        }

        /** DRAW **/
        double drawSpeed = config.meleeAnimations.get(AnimationMeleeType.DRAW).get(0).getSpeed(config.FPS) * stepTick;
        DRAW = Math.max(0, DRAW + drawSpeed);
        if (DRAW > 1F) {
            DRAW = 1F;
        }
        /**
         * Sprinting
         */
        float moveDistance=player.distanceWalkedModified-player.prevDistanceWalkedModified;
        long time=System.currentTimeMillis();
        double sprintSpeed = Math.sin(SPRINT * 3.14) * 0.09f;
        if (sprintSpeed < 0.03f) {
            sprintSpeed = 0.03f;
        }
        sprintSpeed *= stepTick;
        double sprintValue = 0;
        
        if(player instanceof EntityPlayerSP) {
            if(((EntityPlayerSP)player).movementInput.jump) {
                isJumping=true;
            }else if(player.onGround) {
                isJumping=false;
            }  
        }

        boolean flag=(player.onGround||player.fallDistance<2f)&&!isJumping;

        if (player.isSprinting() && moveDistance > 0.05 && flag) {
            if (time > sprintCoolTime) {
                sprintValue = SPRINT + sprintSpeed;
            }
        } else {
            sprintCoolTime = time + 100;
            sprintValue = SPRINT - sprintSpeed;
        }
        if (ATTACK < 1 || INSPECT < 1) {
            sprintValue = SPRINT - sprintSpeed * 2.5f;
        }

        SPRINT = Math.max(0, Math.min(1, sprintValue));

        /** SPRINT_LOOP **/
        if (!config.meleeAnimations.containsKey(AnimationMeleeType.SPRINT)) {
            SPRINT_LOOP = 0;
            SPRINT_RANDOM = 0;
        } else {
            double sprintLoopSpeed = config.meleeAnimations.get(AnimationMeleeType.SPRINT).get(0).getSpeed(config.FPS) * stepTick
                    * (moveDistance / 0.15f);
            boolean flagSprintRand = false;
            if (flag) {
                if (time > sprintLoopCoolTime) {
                    if (player.isSprinting()) {
                        SPRINT_LOOP += sprintLoopSpeed;
                        SPRINT_RANDOM += sprintLoopSpeed;
                        flagSprintRand = true;
                    }
                }
            } else {
                sprintLoopCoolTime = time + 100;
            }
            if (!flagSprintRand) {
                SPRINT_RANDOM -= config.meleeAnimations.get(AnimationMeleeType.SPRINT).get(0).getSpeed(config.FPS) * 3 * stepTick;
            }
            if (SPRINT_LOOP > 1) {
                SPRINT_LOOP = 0;
            }
            if (SPRINT_RANDOM > 1) {
                SPRINT_RANDOM = 0;
            }
            if (SPRINT_RANDOM < 0) {
                SPRINT_RANDOM = 0;
            }
            if (Double.isNaN(SPRINT_RANDOM)) {
                SPRINT_RANDOM = 0;
            }
        }
        /** INSPECT **/
        if (!config.meleeAnimations.containsKey(AnimationMeleeType.INSPECT)) {
            INSPECT = 1;
        } else {
            double modeChangeVal = config.meleeAnimations.get(AnimationMeleeType.INSPECT).get(0).getSpeed(config.FPS)
                    * stepTick;
            INSPECT += modeChangeVal;
            if (INSPECT >= 1) {
                INSPECT = 1;
            }
        }
        updateActionAndTime();
    }

    public AnimationMeleeType getPlayingAnimation() {
        return this.playback.action;
    }

    public void updateCurrentItem() {
        boolean playSound = true;
        ItemStack stack = Minecraft.getMinecraft().player.getHeldItemMainhand();
        if (oldCurrentItem != Minecraft.getMinecraft().player.inventory.currentItem) {
            reset(true);
            oldCurrentItem = Minecraft.getMinecraft().player.inventory.currentItem;
            if (stack.getItem() instanceof ItemMelee) {
                if (playSound)
                    ((ItemMelee) stack.getItem()).type.playClientSound(Minecraft.getMinecraft().player,
                            WeaponSoundType.MeleeDraw);
            }
        }
        if (oldItemstack != Minecraft.getMinecraft().player.getHeldItemMainhand()) {
            if (oldItemstack == null || oldItemstack.isEmpty()) {
                reset(true);
            }
            oldItemstack = Minecraft.getMinecraft().player.getHeldItemMainhand();
        }
    }

    public void updateAction() {
        boolean flag = nextResetDefault;
        nextResetDefault = false;
        if (ATTACK < 1) {
            // this.playback.action = AnimationMeleeType.ATTACK;
            this.playback.action = this.stateMachine.getAttackAnimationType();
            resetView();
        } else if (DRAW < 1F) {
            this.playback.action = AnimationMeleeType.DRAW;
        } else if (INSPECT < 1) {
            this.playback.action = AnimationMeleeType.INSPECT;
        } else if (this.playback.hasPlayed || this.playback.action != AnimationMeleeType.DEFAULT) {
            if (flag) {
                this.playback.action = AnimationMeleeType.DEFAULT;
            }
            nextResetDefault = true;
        }
    }

    public void updateTime() {
        if (this.playback.action == null) {
            return;
        }
        switch (this.playback.action) {
            case DEFAULT:
                this.playback.updateTime(0, DEFAULT);
                break;
            case DRAW:
                this.playback.updateTime(0, DRAW);
                break;
            case INSPECT:
                this.playback.updateTime(0, INSPECT);
                break;
            default:
                this.playback.updateTime(currentOrder, ATTACK);//
                break;
        }
    }

    public void updateActionAndTime() {
        updateAction();
        updateTime();
    }

    public float getTime() {
        return (float) playback.time;
    }

    public float getSprintTime(){
        if(!config.meleeAnimations.containsKey(AnimationMeleeType.SPRINT)) {
            return 0;
        }
        double startTime = config.meleeAnimations.get(AnimationMeleeType.SPRINT).get(0).getStartTime(config.FPS);
        double endTime = config.meleeAnimations.get(AnimationMeleeType.SPRINT).get(0).getEndTime(config.FPS);
        double result = Interpolation.LINEAR.interpolate(startTime, endTime, SPRINT_LOOP);
        if(Double.isNaN(result)) {
            return 0;
        }
        return (float) result;
    }

    public MeleeRenderConfig getConfig() {
        return this.config;
    }

    public void setConfig(MeleeRenderConfig config) {
        this.config = config;
    }

    public boolean isDrawing() {
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        if (item instanceof ItemMelee) {
            return this.playback.action == AnimationMeleeType.DRAW;
        }
        return false;
    }

    public boolean isCouldAttack() {
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        if (item instanceof ItemMelee) {
            if (isDrawing()) {
                return false;
            }
        }
        return true;
    }

    public boolean canAttack() {
        if (stateMachine.bounced) {
            return true;
        } else if (stateMachine.canDealDamage) {
            if (stateMachine.attackPhase == Phase.POST && ATTACK >= 1) {
                return true;
            }
            if (type.resetAttackOnClick) {
                if (!stateMachine.isHeavy) {
                    if (type.attack.length > currentOrder) {
                        keepOrder = type.attack[currentOrder].keepOrder = true;
                        keepOrder = type.attack[currentOrder].keepOrder;
                    }
                } else {
                    if (type.attackHeavy.length > currentOrder) {
                        keepOrder = type.attackHeavy[currentOrder].keepOrder;
                    }
                }
                return true;
            }
        } else if (stateMachine.attackPhase == Phase.POST) {
            if (ATTACK == 1) {
                return true;
            } else if (type.resetPostOnClick) {
                return true;
            }
        }
        return false;
    }

    public boolean attack() {
        if (canAttack()) {
            stateMachine.reset();
            stateMachine.canDealDamage = true;
            stateMachine.attackPhase = Phase.PRE;
            // applyAnim(AnimationMeleeType.PREATTACK);
            applyArrayAnimOrder(AnimationMeleeType.PREATTACK);
            ATTACK = 0;
            return true;
        }
        return false;
    }

    public boolean attackHeavy() {
        if (canAttack()) {
            this.stateMachine.reset();
            this.stateMachine.isHeavy = true;
            this.stateMachine.canDealDamage = true;
            this.stateMachine.attackPhase = Phase.PRE;
            // this.currentOrder=0;
            // applyAnim(AnimationMeleeType.PREHEAVYATTACK);
            applyArrayAnimOrder(AnimationMeleeType.PREHEAVYATTACK);
            ATTACK = 0;
            return true;
        }
        return false;
    }

    public void applyAnim(AnimationMeleeType type) {
        boolean playSound = true;
        Item item = Minecraft.getMinecraft().player.getHeldItemMainhand().getItem();
        if (item instanceof ItemMelee) {
            MeleeType meleeType = ((ItemMelee) item).type;
            switch (type) {
                case INSPECT:
                    if (INSPECT == 1F) {
                        // applyRandomAnim(AnimationMeleeType.INSPECT);
                        INSPECT = 0;
                        if (playSound)
                            ((ItemMelee) item).type.playClientSound(Minecraft.getMinecraft().player,
                                    WeaponSoundType.MeleeInspect);
                    }
                    break;
                default:
                    // if(meleeType.resetAttackOnClick) {
                    // applyRandomAnim(AnimationMeleeType.ATTACK);
                    // ATTACK = 0;
                    // if(playSound)
                    // ((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttack);
                    // } else if(ATTACK == 1F){
                    // applyRandomAnim(AnimationMeleeType.ATTACK);
                    // ATTACK = 0;
                    // if(playSound)
                    // ((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttack);
                    // }
                    break;
            }
        }
    }

    public void applyArrayAnimOrder(AnimationMeleeType meleeType) {
        boolean isSequence = false;
        if (keepOrder) {
            keepOrder = false;
            return;
        }
        if (!stateMachine.isHeavy) {
            if (type.attack.length > currentOrder) {
                if (type.attack[currentOrder].nextPhase != -1) {
                    currentOrder = type.attack[currentOrder].nextPhase;
                    isSequence = true;
                }
            }
        } else {
            if (type.attackHeavy.length > currentOrder) {
                if (type.attackHeavy[currentOrder].nextPhase != -1) {
                    currentOrder = type.attackHeavy[currentOrder].nextPhase;
                    isSequence = true;
                }
            }
        }
        if (!isSequence) {
            Random rand = new Random();
            currentOrder = rand.nextInt(config.meleeAnimations.get(meleeType).size());
            // currentRandomAnim.put(meleeType,
            // rand.nextInt(config.animations.get(meleeType).size()));
        }
    }

}
