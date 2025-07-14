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

// 注意：该实体需要在EntityRegistry中注册，详见EntityRegistrySIZVehicle.java
public class EntityCar extends EntitySIZVehicle implements IControllableVehicleGround, IPhysicsObject {
    // 移除静态model字段，因为它会在服务器端触发LWJGL类加载
    // private static final Model model = new Model(new GltfRenderModel(GltfDataModel.load(new ResourceLocation("modularwarfare:gltf/赛博基尼.glb"))));

    private static final float TIME_PERTICK = 1 / 20f;
    private static final Vector3f AXI_Z = new Vector3f(0, 0, 1);
    private static final float SPEED_THRESHOLD = 0.01f;

    private TypeCar type;
    private CarState state = CarState.Drive;

    //普通驾驶
    public float speed = 0;
    public float lastSpeed = 0;

    //物理模拟
    @Deprecated
    private MassPoint massPoint = new MassPoint();

    //姿态
    public Pose syncPose = new Pose();
    @Deprecated
    private Pose pose = new Pose();
    @Deprecated
    private Pose lastPose = new Pose();

    //输入
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

    //客户端效果参数
    public float whellProcess;
    public float lastWhellProcess;
    
    public boolean heroad=false;

    public Vector3f debugPoint1 = new Vector3f();
    public Vector3f debugPoint2 = new Vector3f();
    public Vector3f debugPoint3 = new Vector3f();
    public Vector3f debugPoint4 = new Vector3f();
    
    // 姿态缓冲变量，用于平滑微小抖动
    private float lastRollAngle = 0.0f;
    private float lastPitchAngle = 0.0f;
    private static final float SMOOTHING_FACTOR = 0.3f; // 平滑因子，值越小越平滑
    private static final float MIN_ANGLE_THRESHOLD = 1f; // 最小角度阈值，小于此值的变化将被忽略
    private static final float LARGE_ANGLE_THRESHOLD = 15.0f; // 大角度阈值，超过此值的变化将立即响应

    // 轮子位置缓存（由客户端发送给服务器）
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
        this.type.maxWhellAngle = 30;
        this.type.brakeAcceleration = 20;
        this.type.maxSafeSteeringSpeed = 8;
        
        // 初始化默认轮子位置（如果客户端没有发送）
        this.leftFrontOffset = new Matrix4f().translate(-1.0f, -0.5f, 2.0f);
        this.rightFrontOffset = new Matrix4f().translate(1.0f, -0.5f, 2.0f);
        this.leftBackOffset = new Matrix4f().translate(-1.0f, -0.5f, -2.0f);
        this.rightBackOffset = new Matrix4f().translate(1.0f, -0.5f, -2.0f);
    }

    /**
     * 设置轮子位置偏移量（由客户端通过网络包调用）
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
//                System.out.println(this.state+","+lastInputPowerFactor+","+inputAngleFactor+","+lastInputBrake+","+lastInputShift+","+this.speed+","+this.onGround);
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
        
        // 根据四个轮子坐标调整车辆姿态
        float centerHeight = (float)(forwardLeft.y+forwardRight.y+backwardLeft.y+backwardRight.y)/4;
        float carLength = 4.5f; // 车辆长度
        float carWidth = 2.0f;  // 车辆宽度
        
        // 计算roll角度（左右倾斜）
        float leftAvgHeight = (forwardLeft.y + backwardLeft.y) / 2.0f;
        float rightAvgHeight = (forwardRight.y + backwardRight.y) / 2.0f;
        float heightDiffLR = rightAvgHeight - leftAvgHeight;
        float rawRollAngle = (float) Math.toDegrees(Math.atan2(heightDiffLR, carWidth));
        
        // 计算pitch角度（前后俯仰）
        float frontAvgHeight = (forwardLeft.y + forwardRight.y) / 2.0f;
        float backAvgHeight = (backwardLeft.y + backwardRight.y) / 2.0f;
        float heightDiffFB = backAvgHeight - frontAvgHeight;
        float rawPitchAngle = (float) Math.toDegrees(Math.atan2(heightDiffFB, carLength));
        
        // 应用智能缓冲系统，平衡平滑性和响应性
        float rollAngleDiff = rawRollAngle - lastRollAngle;
        float pitchAngleDiff = rawPitchAngle - lastPitchAngle;
        
        float smoothedRollAngle, smoothedPitchAngle;
        
        // 处理roll角度
        if (Math.abs(rollAngleDiff) < MIN_ANGLE_THRESHOLD) {
            // 微小变化：忽略
            smoothedRollAngle = lastRollAngle;
        } else if (Math.abs(rollAngleDiff) > LARGE_ANGLE_THRESHOLD) {
            // 大变化：立即响应
            smoothedRollAngle = rawRollAngle;
        } else {
            // 中等变化：平滑过渡
            smoothedRollAngle = lastRollAngle + rollAngleDiff * SMOOTHING_FACTOR;
        }
        
        // 处理pitch角度
        if (Math.abs(pitchAngleDiff) < MIN_ANGLE_THRESHOLD) {
            // 微小变化：忽略
            smoothedPitchAngle = lastPitchAngle;
        } else if (Math.abs(pitchAngleDiff) > LARGE_ANGLE_THRESHOLD) {
            // 大变化：立即响应
            smoothedPitchAngle = rawPitchAngle;
        } else {
            // 中等变化：平滑过渡
            smoothedPitchAngle = lastPitchAngle + pitchAngleDiff * SMOOTHING_FACTOR;
        }
        
        // 更新上一帧的角度值
        lastRollAngle = smoothedRollAngle;
        lastPitchAngle = smoothedPitchAngle;
        
        // 应用智能缓冲后的roll和pitch角度
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
        // TODO Auto-generated method stub
        return getEntityBoundingBox().grow(2, 0, 2);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox() {
        // TODO Auto-generated method stub
        return super.getCollisionBoundingBox();
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entityIn) {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return false;
    }

    public void handleSteering() {
        float carLength = 4.5f;
        //wv是一个界定是否为安全驾驶的指标
        float wv = getMaxSafeSteeringSpeed() * getMaxSafeSteeringSpeed() * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle()))) / carLength;
        if (this.speed * this.speed * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * Math.abs(getInputAngleFactor())))) / carLength <= wv) {
            //使用完全抓地情况下车辆转弯半径推导而来
            this.rotationYaw += Math.toDegrees(this.speed * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * getInputAngleFactor()))) / carLength * TIME_PERTICK);
        } else {
            if (getInputAngleFactor() >= 0) {
                this.rotationYaw += Math.toDegrees(wv / this.speed * TIME_PERTICK);
            } else {
                this.rotationYaw += Math.toDegrees(-wv / this.speed * TIME_PERTICK);
            }
        }
    }

    public void handleShift() {
        //显然 车必须得动才能漂移
        if (this.getMassPoint().getSpeed().length() <= 0) {
            return;
        }
        if (this.fallDistance>1) {
            return;
        }
        float carLength = 4.5f;
        float fac = 0.8f;
        this.rotationYaw += Math.toDegrees(Math.min(getMaxSafeSteeringSpeed(), this.getMassPoint().getSpeed().length()) * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * getInputAngleFactor()))) / (carLength * fac) * TIME_PERTICK);
        //如果速度向量与左向量或右向量越接近 减速越快
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
     * 获取车辆的质点对象，用于访问物理数据
     */
    public MassPoint getMassPoint() {
        return massPoint;
    }

    /**
     * 设置玩家驾驶输入
     * @param powerFactor 动力因子 (-1.0 到 1.0)
     * @param angleFactor 转向因子 (-1.0 到 1.0)
     * @param brake 刹车状态
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
        // TODO Auto-generated method stub
        return this.type;
    }

    @Override
    public float getInputPowerFactor() {
        // TODO Auto-generated method stub
        return this.inputPowerFactor;
    }

    @Override
    public float getInputAngleFactor() {
        // TODO Auto-generated method stub
        return this.inputAngleFactor;
    }

    @Override
    public boolean isInputBrake() {
        // TODO Auto-generated method stub
        return this.inputBrake;
    }

    @Override
    public boolean isInputShift() {
        // TODO Auto-generated method stub
        return this.inputShift;
    }
}
