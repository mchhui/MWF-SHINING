package mchhui.sizvehicle.common.entity;

import java.util.ArrayList;
import java.util.List;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import mchhui.sizvehicle.network.ServerSIZVehicle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import mchhui.hegltf.GltfDataModel;
import mchhui.hegltf.GltfRenderModel;
import mchhui.sizvehicle.client.handler.DebugRenderHandler;
import mchhui.sizvehicle.common.model.Model;
import mchhui.sizvehicle.common.physics.MassPoint;
import mchhui.sizvehicle.common.physics.Pose;
import mchhui.sizvehicle.common.type.TypeBase;
import mchhui.sizvehicle.common.type.TypeCar;

public class EntityCar extends EntitySIZVehicle implements IControllableVehicleGround, IPhysicsObject {

    private static final float TIME_PERTICK = 1 / 20f;
    private static final Vector3f AXI_Z = new Vector3f(0, 0, 1);
    private static final float SPEED_THRESHOLD = 0.01f;

    private TypeCar type;
    private CarState state = CarState.Drive;

    public float speed = 0;
    public float lastSpeed = 0;

    @Deprecated
    private MassPoint massPoint = new MassPoint();

    public Pose syncPose = new Pose();
    @Deprecated
    private Pose pose = new Pose();
    @Deprecated
    private Pose lastPose = new Pose();

    @Deprecated
    private float inputPowerFactor = 0;
    @Deprecated
    private float inputAngleFactor = 0;
    @Deprecated
    private boolean inputBrake = false;
    @Deprecated
    private boolean inputShift = false;

    public float lastInputPowerFactor = 0;
    public float lastInputAngleFactor = 0;
    public boolean lastInputBrake = false;
    public boolean lastInputShift = false;

    public float whellProcess;
    public float lastWhellProcess;
    
    public boolean heroad=false;

    public Vector3f debugPoint1 = new Vector3f();
    public Vector3f debugPoint2 = new Vector3f();
    public Vector3f debugPoint3 = new Vector3f();
    public Vector3f debugPoint4 = new Vector3f();
    
    private float lastRollAngle = 0.0f;
    private float lastPitchAngle = 0.0f;
    private static final float SMOOTHING_FACTOR = 0.3f;
    private static final float MIN_ANGLE_THRESHOLD = 1f;
    private static final float LARGE_ANGLE_THRESHOLD = 15.0f;

    private Matrix4f leftFrontOffset;
    private Matrix4f rightFrontOffset;
    private Matrix4f leftBackOffset;
    private Matrix4f rightBackOffset;
    private boolean wheelOffsetsInitialized = false;

    public static enum CarState {
        Drive, Shift, Physics
    }

    public EntityCar(World worldIn) {
        super(worldIn);
        this.type = new TypeCar();
        this.type.mass = 1000;
        this.type.maxForwardSpeed = 30;
        this.type.maxForwardAcceleration = 2.8f;
        this.type.maxBackwardSpeed = 3;
        this.type.maxBackwardAcceleration = 1.5f;
        this.type.maxWhellAngle = 20;
        this.type.brakeAcceleration = 20;
        this.type.maxSafeSteeringSpeed = 15;
        this.type.relativeFrictionCoefficient = 1.0f;
        this.type.relativeFrictionCoefficientInSteering = 2.2f;
        
        this.leftFrontOffset = new Matrix4f().translate(-1.0f, -0.5f, 2.0f);
        this.rightFrontOffset = new Matrix4f().translate(1.0f, -0.5f, 2.0f);
        this.leftBackOffset = new Matrix4f().translate(-1.0f, -0.5f, -2.0f);
        this.rightBackOffset = new Matrix4f().translate(1.0f, -0.5f, -2.0f);
    }

    /**
     * 设置轮子位置偏移量
     */
    public void setWheelOffsets(Matrix4f leftFrontOffset, Matrix4f rightFrontOffset, 
                               Matrix4f leftBackOffset, Matrix4f rightBackOffset) {
        this.leftFrontOffset = new Matrix4f(leftFrontOffset);
        this.rightFrontOffset = new Matrix4f(rightFrontOffset);
        this.leftBackOffset = new Matrix4f(leftBackOffset);
        this.rightBackOffset = new Matrix4f(rightBackOffset);
        this.wheelOffsetsInitialized = true;
    }

    /**
     * 检查轮子位置是否已初始化
     */
    public boolean areWheelOffsetsInitialized() {
        return wheelOffsetsInitialized;
    }

    @Override
    public void onUpdate() {
        this.stepHeight = 1f;
        boolean flag=true;
        for(int x=-1;x<=1;x++) {
            for(int z=-1;z<=1;z++) {
                for(int y=-1;y<=0;y++) {
                    BlockPos pos=getPosition().add(x, y, z);
                    if(!world.isAirBlock(pos)) {
                        if(!world.getBlockState(pos).getBlock().getRegistryName().equals(new ResourceLocation("hueihueaengine:placeholder")))
                        flag=false;
                    }
                }
            }
        }
        this.heroad=flag;
        this.setSize(3f, 2);
        super.onUpdate();
        this.prevRotationYaw = this.rotationYaw;
        if (!this.world.isRemote) {
            this.getMassPoint().setMass(getMass());
            if (isInputShift()) {
                if (state == CarState.Drive) {
                    Vector3f moveVec = this.getPose().getForward().mul(this.speed);
                    this.getMassPoint().setSpeed(moveVec.x, moveVec.y, moveVec.z);
                }
                state = CarState.Shift;
            } else {
                if (state == CarState.Shift) {
                    if (this.getMassPoint().getSpeed().dot(this.getPose().getLeft()) >= -1f && this.getMassPoint().getSpeed().dot(this.getPose().getLeft()) <= 1f) {
                        this.speed = this.getMassPoint().getSpeed().dot(this.getPose().getForward());
                        state = CarState.Drive;
                    }
                }
            }
            switch (state) {
                case Drive:
                    this.handleSteering();
                    this.updateSpeed();
                    Vector3f moveVec = this.getPose().getForward().mul(this.speed * TIME_PERTICK);
                    this.move(MoverType.SELF, moveVec.x, 0, moveVec.z);
                    break;
                case Shift:
                    this.handleShift();
                    break;
                case Physics:
                    break;
            }
            this.getPose().getQuaternion().identity();
            this.getPose().getQuaternion().rotateY(Math.toRadians(this.rotationYaw));
            terrainTest();
            boolean update = false;
            update = update || lastSpeed != speed;
            update = update || lastInputPowerFactor != getInputPowerFactor();
            update = update || lastInputAngleFactor != getInputAngleFactor();
            update = update || lastInputBrake != isInputBrake();
            update = update || lastInputShift != isInputShift();
            update = update || !this.getLastPose().getQuaternion().equals(this.getPose().getQuaternion());
            if (update) {
                ServerSIZVehicle.boardCastVehiclePose(this);
            }
            lastSpeed = speed;
            lastInputPowerFactor = getInputPowerFactor();
            lastInputAngleFactor = getInputAngleFactor();
            lastInputBrake = isInputBrake();
            lastInputShift = isInputShift();
            this.getLastPose().getQuaternion().set(this.getPose().getQuaternion());
            if (this.getPassengers().size() > 0) {
                ServerSIZVehicle.sendDebugVehicleState((EntityPlayerMP)this.getPassengers().get(0));
            }
        } else {
            this.getLastPose().getQuaternion().set(this.getPose().getQuaternion());
            this.getPose().getQuaternion().set(this.syncPose.getQuaternion());
            this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX, this.posY + 1, this.posZ, 0, 0, 0);
            float whellR = 0.3f;
            lastWhellProcess = whellProcess;
            whellProcess += (this.speed / whellR) * TIME_PERTICK;
            if (isInputBrake()) {
                whellProcess = 0;
            }
            double px=posX;
            double py=posY;
            double pz=posZ;
            AxisAlignedBB testAABB=new AxisAlignedBB(px-0.1f,py-2,pz-0.1f,px+0.1f,py,pz+0.1f);
            List<AxisAlignedBB> result=world.getCollisionBoxes(null, testAABB);
            double maxY=-1;
            for(int i=0;i<result.size();i++) {
                if(result.get(i).maxY>maxY&&result.get(i).maxY<=py) {
                    maxY=result.get(i).maxY;
                }
            }
            if(maxY!=-1) {
                
            }
        }
    }

    public void terrainTest() {
        // 使用缓存的轮子位置偏移量
        float testHeight=stepHeight;
        Vector3f forwardLeft = new Vector3f();
        Vector3f forwardRight = new Vector3f();
        Vector3f backwardLeft = new Vector3f();
        Vector3f backwardRight = new Vector3f();
        
        forwardLeft:{
            Vector4f testPoint = new Vector4f(0, 0, 0, 1).mul(new Matrix4f().translate((float)posX, (float)posY, (float)posZ).rotate(getPose().getQuaternion()).mul(leftFrontOffset));
            AxisAlignedBB testAABB = new AxisAlignedBB(testPoint.x, testPoint.y, testPoint.z, testPoint.x, testPoint.y, testPoint.z).grow(0.2f, testHeight, 0.2f);
            List<AxisAlignedBB> result = world.getCollisionBoxes(null, testAABB);
            double maxY = posY - testHeight;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).maxY > maxY && result.get(i).maxY - posY <= testHeight) {
                    maxY = result.get(i).maxY;
                }
            }
            forwardLeft.set(testPoint.x, maxY, testPoint.z);
            break forwardLeft;
        }
        forwardRight:{
            Vector4f testPoint = new Vector4f(0, 0, 0, 1).mul(new Matrix4f().translate((float)posX, (float)posY, (float)posZ).rotate(getPose().getQuaternion()).mul(rightFrontOffset));
            AxisAlignedBB testAABB = new AxisAlignedBB(testPoint.x, testPoint.y, testPoint.z, testPoint.x, testPoint.y, testPoint.z).grow(0.2f, testHeight, 0.2f);
            List<AxisAlignedBB> result = world.getCollisionBoxes(null, testAABB);
            double maxY = posY - testHeight;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).maxY > maxY && result.get(i).maxY - posY <= testHeight) {
                    maxY = result.get(i).maxY;
                }
            }
            forwardRight.set(testPoint.x, maxY, testPoint.z);
            break forwardRight;
        }
        backwardLeft:{
            Vector4f testPoint = new Vector4f(0, 0, 0, 1).mul(new Matrix4f().translate((float)posX, (float)posY, (float)posZ).rotate(getPose().getQuaternion()).mul(leftBackOffset));
            AxisAlignedBB testAABB = new AxisAlignedBB(testPoint.x, testPoint.y, testPoint.z, testPoint.x, testPoint.y, testPoint.z).grow(0.2f, testHeight, 0.2f);
            List<AxisAlignedBB> result = world.getCollisionBoxes(null, testAABB);
            double maxY = posY - testHeight;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).maxY > maxY && result.get(i).maxY - posY <= testHeight) {
                    maxY = result.get(i).maxY;
                }
            }
            backwardLeft.set(testPoint.x, maxY, testPoint.z);
            break backwardLeft;
        }
        backwardRight:{
            Vector4f testPoint = new Vector4f(0, 0, 0, 1).mul(new Matrix4f().translate((float)posX, (float)posY, (float)posZ).rotate(getPose().getQuaternion()).mul(rightBackOffset));
            AxisAlignedBB testAABB = new AxisAlignedBB(testPoint.x, testPoint.y, testPoint.z, testPoint.x, testPoint.y, testPoint.z).grow(0.2f, testHeight, 0.2f);
            List<AxisAlignedBB> result = world.getCollisionBoxes(null, testAABB);
            double maxY = posY - testHeight;
            for (int i = 0; i < result.size(); i++) {
                if (result.get(i).maxY > maxY && result.get(i).maxY - posY <= testHeight) {
                    maxY = result.get(i).maxY;
                }
            }
            backwardRight.set(testPoint.x, maxY, testPoint.z);
            break backwardRight;
        }
        debugPoint1.set(forwardLeft.x,forwardLeft.y,forwardLeft.z);
        debugPoint2.set(forwardRight.x,forwardRight.y,forwardRight.z);
        debugPoint3.set(backwardLeft.x,backwardLeft.y,backwardLeft.z);
        debugPoint4.set(backwardRight.x,backwardRight.y,backwardRight.z);
        
        float carLength = 4.5f;
        float carWidth = 2.0f;
        
        float leftAvgHeight = (forwardLeft.y + backwardLeft.y) / 2.0f;
        float rightAvgHeight = (forwardRight.y + backwardRight.y) / 2.0f;
        float heightDiffLR = rightAvgHeight - leftAvgHeight;
        float rawRollAngle = (float) Math.toDegrees(Math.atan2(heightDiffLR, carWidth));
        
        float frontAvgHeight = (forwardLeft.y + forwardRight.y) / 2.0f;
        float backAvgHeight = (backwardLeft.y + backwardRight.y) / 2.0f;
        float heightDiffFB = backAvgHeight - frontAvgHeight;
        float rawPitchAngle = (float) Math.toDegrees(Math.atan2(heightDiffFB, carLength));
        
        float rollAngleDiff = rawRollAngle - lastRollAngle;
        float pitchAngleDiff = rawPitchAngle - lastPitchAngle;
        
        float smoothedRollAngle, smoothedPitchAngle;
        
        if (Math.abs(rollAngleDiff) < MIN_ANGLE_THRESHOLD) {
            smoothedRollAngle = lastRollAngle;
        } else if (Math.abs(rollAngleDiff) > LARGE_ANGLE_THRESHOLD) {
            smoothedRollAngle = rawRollAngle;
        } else {
            smoothedRollAngle = lastRollAngle + rollAngleDiff * SMOOTHING_FACTOR;
        }
        
        if (Math.abs(pitchAngleDiff) < MIN_ANGLE_THRESHOLD) {
            smoothedPitchAngle = lastPitchAngle;
        } else if (Math.abs(pitchAngleDiff) > LARGE_ANGLE_THRESHOLD) {
            smoothedPitchAngle = rawPitchAngle;
        } else {
            smoothedPitchAngle = lastPitchAngle + pitchAngleDiff * SMOOTHING_FACTOR;
        }
        
        lastRollAngle = smoothedRollAngle;
        lastPitchAngle = smoothedPitchAngle;
        
        this.getPose().rotateZ(-smoothedRollAngle);
        this.getPose().rotateX(smoothedPitchAngle);
    }

    @Override
    public void applyEntityCollision(Entity entityIn) {
        if (!this.isRidingSameEntity(entityIn)) {
            if (!entityIn.noClip && !this.noClip) {
                double d0 = entityIn.posX - this.posX;
                double d1 = entityIn.posZ - this.posZ;
                double d2 = MathHelper.absMax(d0, d1);

                if (d2 >= 0.009999999776482582D) {
                    d2 = (double)MathHelper.sqrt(d2);
                    d0 = d0 / d2;
                    d1 = d1 / d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D) {
                        d3 = 1.0D;
                    }

                    d0 = d0 * d3;
                    d1 = d1 * d3;
                    d0 = d0 * 0.05000000074505806D;
                    d1 = d1 * 0.05000000074505806D;
                    d0 = d0 * (double)(1.0F - this.entityCollisionReduction);
                    d1 = d1 * (double)(1.0F - this.entityCollisionReduction);

                    if (!entityIn.isBeingRidden()) {
                        entityIn.addVelocity(d0, 0.0D, d1);
                    }
                }
            }
        }
    }

    @Override
    protected void collideWithNearbyEntities() {
        super.collideWithNearbyEntities();
    }

    @Override
    protected void collideWithEntity(Entity entityIn) {
        if (!this.isRidingSameEntity(entityIn)) {
            if (!entityIn.noClip && !this.noClip) {
                double d0 = entityIn.posX - this.posX;
                double d1 = entityIn.posZ - this.posZ;
                double d2 = MathHelper.absMax(d0, d1);

                if (d2 >= 0.009999999776482582D) {
                    d2 = (double)MathHelper.sqrt(d2);
                    d0 = d0 / d2;
                    d1 = d1 / d2;
                    double d3 = 1.0D / d2;

                    if (d3 > 1.0D) {
                        d3 = 1.0D;
                    }

                    d0 = d0 * d3;
                    d1 = d1 * d3;
                    d0 = d0 * 0.05000000074505806D;
                    d1 = d1 * 0.05000000074505806D;
                    d0 = d0 * (double)(1.0F - this.entityCollisionReduction);
                    d1 = d1 * (double)(1.0F - this.entityCollisionReduction);

                    if (!entityIn.isBeingRidden()) {
                        entityIn.addVelocity(d0, 0.0D, d1);
                    }
                }
            }
        }
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getEntityBoundingBox().grow(2, 0, 2);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        return super.getCollisionBoundingBox();
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entityIn) {
        return super.getCollisionBox(entityIn);
    }

    @Override
    public AxisAlignedBB getEntityBoundingBox() {
        return super.getEntityBoundingBox();
    }

    @Override
    public void knockBack(Entity entityIn, float strength, double xRatio, double zRatio) {
        super.knockBack(entityIn, strength / getMass(), xRatio, zRatio);
    }

    @Override
    public boolean canBeCollidedWith() {
        return super.canBeCollidedWith();
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    public void handleSteering() {
        float carLength = 4.5f;
        
        float minSteeringSpeed = 2.0f;
        float comfortableSteeringSpeed = 6.0f;
        float maxComfortableSteeringSpeed = 12.0f;
        
        float speedBasedSteeringFactor = 1.0f;
        float currentSpeed = Math.abs(this.speed);
        
        if (currentSpeed < minSteeringSpeed) {
            speedBasedSteeringFactor = currentSpeed / minSteeringSpeed * 0.2f;
        } else if (currentSpeed < comfortableSteeringSpeed) {
            float ratio = (currentSpeed - minSteeringSpeed) / (comfortableSteeringSpeed - minSteeringSpeed);
            speedBasedSteeringFactor = 0.2f + ratio * 0.4f;
        } else if (currentSpeed < maxComfortableSteeringSpeed) {
            float ratio = (currentSpeed - comfortableSteeringSpeed) / (maxComfortableSteeringSpeed - comfortableSteeringSpeed);
            speedBasedSteeringFactor = 0.6f + ratio * 0.4f;
        } else if (currentSpeed > getMaxSafeSteeringSpeed()) {
            float overspeedRatio = (currentSpeed - getMaxSafeSteeringSpeed()) / (getMaxForwardSpeed() - getMaxSafeSteeringSpeed());
            overspeedRatio = Math.min(overspeedRatio, 1.0f);
            speedBasedSteeringFactor = 1.0f - overspeedRatio * 0.7f;
        }
        
        float effectiveMaxWheelAngle = getMaxWhellAngle();
        if (currentSpeed > comfortableSteeringSpeed) {
            float speedRatio = Math.min(currentSpeed / getMaxForwardSpeed(), 1.0f);
            effectiveMaxWheelAngle = getMaxWhellAngle() * (1.0f - speedRatio * 0.4f);
        }
        
        float effectiveAngleFactor = getInputAngleFactor() * speedBasedSteeringFactor;
        float actualWheelAngle = effectiveMaxWheelAngle * effectiveAngleFactor;
        
        float wv = getMaxSafeSteeringSpeed() * getMaxSafeSteeringSpeed() * 2 * MathHelper.sin((float)(Math.toRadians(effectiveMaxWheelAngle))) / carLength;
        
        if (this.speed * this.speed * 2 * MathHelper.sin((float)(Math.toRadians(Math.abs(actualWheelAngle)))) / carLength <= wv) {
            this.rotationYaw += Math.toDegrees(this.speed * 2 * MathHelper.sin((float)(Math.toRadians(actualWheelAngle))) / carLength * TIME_PERTICK);
        } else {
            float safeSteeringRate = wv / this.speed * TIME_PERTICK * 0.8f;
            if (effectiveAngleFactor >= 0) {
                this.rotationYaw += Math.toDegrees(safeSteeringRate);
            } else {
                this.rotationYaw += Math.toDegrees(-safeSteeringRate);
            }
        }
    }

    public void handleShift() {
        if (this.getMassPoint().getSpeed().length() <= 0) {
            return;
        }
        if (this.fallDistance>1) {
            return;
        }
        float carLength = 4.5f;
        float fac = 0.8f;
        this.rotationYaw += Math.toDegrees(Math.min(getMaxSafeSteeringSpeed(), this.getMassPoint().getSpeed().length()) * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * getInputAngleFactor()))) / (carLength * fac) * TIME_PERTICK);
        
        float sideFactor = this.getMassPoint().getSpeed().dot(this.getPose().getLeft()) / this.getMassPoint().getSpeed().length();
        Vector3f dragForce = this.getMassPoint().getSpeed().normalize().negate().mul(Math.abs(sideFactor) * getMass() * 10 + getMass() * 2);
        this.getMassPoint().addResistanceForce(dragForce.x, dragForce.y, dragForce.z);
        if (!isInputShift()) {
            IBlockState state = this.world.getBlockState(this.getPosition().down());
            Vector3f sideForce = this.getPose().getLeft().mul((sideFactor > 0 ? -1 : 1) * 10 * getMass() * getRelativeFrictionCoefficient() * state.getBlock().getSlipperiness(state, world, this.getPosition().down(), this));
            this.getMassPoint().addResistanceForce(sideForce.x, sideForce.y, sideForce.z);
        }
        float power = (getInputPowerFactor() > 0) ? 1 : ((getInputPowerFactor() < 0) ? 0 : 0.5f);
        Vector3f pushForce = this.getPose().getForward().mul(Math.abs(sideFactor) * getMass() * 20 * power);
        this.getMassPoint().addDriveForce(pushForce.x, pushForce.y, pushForce.z);
        this.getMassPoint().update(TIME_PERTICK);
        Vector3f moveVec = this.getMassPoint().getSpeed().mul(TIME_PERTICK);
        this.move(MoverType.SELF, moveVec.x, 0, moveVec.z);
    }

    public void updateSpeed() {
        if (isInputBrake() && this.onGround) {
            if (this.speed != 0) {
                float preSpeed = this.speed;
                this.speed += (this.speed >= 0) ? -getBrakeAcceleration() * TIME_PERTICK : getBrakeAcceleration() * TIME_PERTICK;
                this.speed = ((preSpeed > 0 && this.speed < 0) || (preSpeed < 0 && this.speed > 0)) ? 0 : this.speed;
            }
        } else {
            if (getInputPowerFactor() != 0 && (this.speed <= getMaxSafeSteeringSpeed() || getInputAngleFactor() == 0) && this.onGround) {
                this.speed += (getInputPowerFactor() >= 0) ? getMaxForwardAcceleration() * getInputPowerFactor() * TIME_PERTICK : getMaxBackwardAcceleration() * getInputPowerFactor() * TIME_PERTICK;
                this.speed = Math.max(-getMaxBackwardSpeed(), Math.min(getMaxForwardSpeed(), this.speed));
            } else if (this.speed != 0) {
                float preSpeed = this.speed;
                float acceleration = 0.01f;
                if (this.onGround) {
                    IBlockState state = this.world.getBlockState(this.getPosition().down());
                    if (getInputAngleFactor() == 0) {
                        acceleration = getRelativeFrictionCoefficient() * state.getBlock().getSlipperiness(state, world, this.getPosition().down(), this);
                    } else {
                        acceleration = getRelativeFrictionCoefficientInSteering() * state.getBlock().getSlipperiness(state, world, this.getPosition().down(), this);
                        
                        float steeringIntensity = Math.abs(getInputAngleFactor());
                        float speedFactor = Math.abs(this.speed) / getMaxForwardSpeed();
                        float steeringDeceleration = steeringIntensity * speedFactor * 4.0f;
                        
                        if (Math.abs(this.speed) > getMaxSafeSteeringSpeed() * 0.8f) {
                            steeringDeceleration *= 1.3f;
                        }
                        
                        acceleration += steeringDeceleration;
                    }
                }
                this.speed += (this.speed >= 0) ? -acceleration * TIME_PERTICK : acceleration * TIME_PERTICK;
                this.speed = ((preSpeed > 0 && this.speed < 0) || (preSpeed < 0 && this.speed > 0)) ? 0 : this.speed;
            }
        }
    };

    public float getMass() {
        return this.type.mass;
    }

    public float getMaxForwardSpeed() {
        return this.type.maxForwardSpeed;
    }

    public float getMaxBackwardSpeed() {
        return this.type.maxBackwardSpeed;
    }

    public float getMaxForwardAcceleration() {
        return this.type.maxForwardAcceleration;
    }

    public float getMaxBackwardAcceleration() {
        return this.type.maxBackwardAcceleration;
    }

    public float getMaxWhellAngle() {
        return this.type.maxWhellAngle;
    }

    public float getBrakeAcceleration() {
        return this.type.brakeAcceleration;
    }

    public float getRelativeFrictionCoefficient() {
        return this.type.relativeFrictionCoefficient;
    }

    public float getRelativeFrictionCoefficientInSteering() {
        return this.type.relativeFrictionCoefficientInSteering;
    }

    public float getMaxSafeSteeringSpeed() {
        return this.type.maxSafeSteeringSpeed;
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (!this.world.isRemote && player.getRidingEntity() != this) {
            player.startRiding(this);
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    protected boolean canFitPassenger(Entity passenger) {
        return getPassengers().size() < 4;
    }

    @Override
    public void updatePassenger(Entity passenger) {
        if (this.isPassenger(passenger)) {
            passenger.setPosition(this.posX, this.posY, this.posZ);
        }
    }

    public Pose getPose() {
        return pose;
    }

    public Pose getLastPose() {
        return lastPose;
    }

    /**
     * 获取车辆的质点对象
     */
    public MassPoint getMassPoint() {
        return massPoint;
    }

    /**
     * 设置玩家驾驶输入
     */
    public void setPlayerInput(float powerFactor, float angleFactor, boolean brake, boolean shift) {
        this.inputPowerFactor = powerFactor;
        this.inputAngleFactor = angleFactor;
        this.inputBrake = brake;
        this.inputShift = shift;
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugAxis() {
        this.pose.renderDebugAxis();
    }

    @Override
    public TypeBase getType() {
        return this.type;
    }

    @Override
    public float getInputPowerFactor() {
        return this.inputPowerFactor;
    }

    @Override
    public float getInputAngleFactor() {
        return this.inputAngleFactor;
    }

    @Override
    public boolean isInputBrake() {
        return this.inputBrake;
    }

    @Override
    public boolean isInputShift() {
        return this.inputShift;
    }
}
