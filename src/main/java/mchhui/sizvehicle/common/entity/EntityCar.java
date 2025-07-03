package mchhui.sizvehicle.common.entity;

import org.joml.Math;
import org.joml.Vector3f;

import mchhui.sizvehicle.network.ServerSIZVehicle;
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
import mchhui.sizvehicle.client.handler.DebugHUDHandler;
import mchhui.sizvehicle.common.physics.MassPoint;
import mchhui.sizvehicle.common.physics.Pose;
import mchhui.sizvehicle.common.type.IType;
import mchhui.sizvehicle.common.type.TypeCar;

// 注意：该实体需要在EntityRegistry中注册，详见EntityRegistrySIZVehicle.java
public class EntityCar extends EntitySIZVehicle implements IControllableVehicleGround, IPhysicsObject {
    private static final float TIME_PERTICK = 1 / 20f;
    private static final Vector3f AXI_Z = new Vector3f(0, 0, 1);
    private static final float SPEED_THRESHOLD = 0.01f;

    private TypeCar type;
    private CarState state;

    //普通驾驶
    private float speed = 0;

    //物理模拟
    private MassPoint massPoint = new MassPoint();

    //姿态
    private Pose pose = new Pose();
    private Pose lastPose = new Pose();

    //输入
    private float inputPowerFactor = 0;
    private float inputAngleFactor = 0;
    private boolean inputBrake = false;

    public static enum CarState {
        Drive, Shift, Physics
    }

    public EntityCar(World worldIn) {
        super(worldIn);
        setNoGravity(true);
        this.type = new TypeCar();
        this.type.maxForwardSpeed = 30;
        this.type.maxForwardAcceleration = 2.8f;
        this.type.maxBackwardSpeed = 3;
        this.type.maxBackwardAcceleration = 1.5f;
        this.type.maxWhellAngle = 30;
        this.type.brakeAcceleration = 20;
        this.type.maxSafeSteeringSpeed = 10;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote) {
            this.handleSteering();
            this.updateSpeed();
            Vector3f moveVec = pose.getForward().mul(this.speed * TIME_PERTICK);
            this.move(MoverType.SELF, moveVec.x, moveVec.y, moveVec.z);
            if (!this.lastPose.getQuaternion().equals(this.pose.getQuaternion())) {
                ServerSIZVehicle.boardCastVehiclePose(this);
                this.lastPose.getQuaternion().set(this.pose.getQuaternion());
            }
        } else {
            this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, this.posX, this.posY + 1, this.posZ, 0, 0, 0);
        }
    }

    public void handleSteering() {
        if (this.speed <= getMaxSafeSteeringSpeed()) {
            this.pose.rotateYRad(this.speed * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * inputAngleFactor))) / 4.5f * TIME_PERTICK);
        } else {
            float wv = getMaxSafeSteeringSpeed() * getMaxSafeSteeringSpeed() * 2 * MathHelper.sin((float)(Math.toRadians(getMaxWhellAngle() * inputAngleFactor))) / 4.5f;
            this.pose.rotateYRad(wv / this.speed * TIME_PERTICK);
        }
    }

    public void updateSpeed() {
        if (this.inputBrake) {
            if (this.speed != 0) {
                float preSpeed = this.speed;
                this.speed += (this.speed >= 0) ? -getBrakeAcceleration() * TIME_PERTICK : getBrakeAcceleration() * TIME_PERTICK;
                this.speed = ((preSpeed > 0 && this.speed < 0) || (preSpeed < 0 && this.speed > 0)) ? 0 : this.speed;
            }
        } else {
            if (this.inputPowerFactor != 0 && (this.speed <= getMaxSafeSteeringSpeed() || this.inputAngleFactor == 0)) {
                this.speed += (this.inputPowerFactor >= 0) ? getMaxForwardAcceleration() * this.inputPowerFactor * TIME_PERTICK : getMaxBackwardAcceleration() * this.inputPowerFactor * TIME_PERTICK;
                this.speed = Math.max(-getMaxBackwardSpeed(), Math.min(getMaxForwardSpeed(), this.speed));
            } else {
                float preSpeed = this.speed;
                float acceleration = 0.01f;
                if (this.onGround) {
                    IBlockState state = this.world.getBlockState(this.getPosition().down());
                    if (this.inputAngleFactor == 0) {
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

    public Pose getPose() {
        return pose;
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
    public void setPlayerInput(float powerFactor, float angleFactor, boolean brake) {
        this.inputPowerFactor = powerFactor;
        this.inputAngleFactor = angleFactor;
        this.inputBrake = brake;
    }

    @SideOnly(Side.CLIENT)
    public void renderDebugAxis() {
        this.pose.renderDebugAxis();
        //依据DebugHUDHandler的数据 画出debug的轴线

        // 获取DebugHUDHandler的数据
        DebugHUDHandler debugHandler = DebugHUDHandler.INSTANCE;
        if (debugHandler == null) {
            return;
        }

        // 保存当前OpenGL状态
        GlStateManager.pushMatrix();
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.glLineWidth(2.0F);

        Tessellator tessellator = Tessellator.getInstance();
        float scale = 0.1f; // 缩放因子，使向量更容易看到

        // 绘制速度向量 (青色)
        if (debugHandler.lastSpeed != null && debugHandler.lastSpeed.length() > 0.01f) {
            Vector3f speedVec = new Vector3f(debugHandler.lastSpeed).mul(scale);
            tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            tessellator.getBuffer().pos(0, 0, 0).color(0, 255, 255, 255).endVertex(); // 青色
            tessellator.getBuffer().pos(speedVec.x, speedVec.y, speedVec.z).color(0, 255, 255, 255).endVertex();
            tessellator.draw();

            // 绘制速度向量标签
            renderVectorLabel("速度", speedVec, 0x00FFFF);
        }

        // 绘制驱动力向量 (绿色)
        if (debugHandler.lastDriveForce != null && debugHandler.lastDriveForce.length() > 0.01f) {
            Vector3f driveVec = new Vector3f(debugHandler.lastDriveForce).mul(scale);
            tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            tessellator.getBuffer().pos(0, 0, 0).color(0, 255, 0, 255).endVertex(); // 绿色
            tessellator.getBuffer().pos(driveVec.x, driveVec.y, driveVec.z).color(0, 255, 0, 255).endVertex();
            tessellator.draw();

            // 绘制驱动力向量标签
            renderVectorLabel("驱动力", driveVec, 0x00FF00);
        }

        // 绘制阻力向量 (红色)
        if (debugHandler.lastResistanceForce != null && debugHandler.lastResistanceForce.length() > 0.01f) {
            Vector3f resistanceVec = new Vector3f(debugHandler.lastResistanceForce).mul(scale);
            tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            tessellator.getBuffer().pos(0, 0, 0).color(255, 0, 0, 255).endVertex(); // 红色
            tessellator.getBuffer().pos(resistanceVec.x, resistanceVec.y, resistanceVec.z).color(255, 0, 0, 255).endVertex();
            tessellator.draw();

            // 绘制阻力向量标签
            renderVectorLabel("阻力", resistanceVec, 0xFF0000);
        }

        // 绘制合力向量 (黄色) - 驱动力 + 阻力
        if (debugHandler.lastDriveForce != null && debugHandler.lastResistanceForce != null) {
            Vector3f netForce = new Vector3f(debugHandler.lastDriveForce).add(debugHandler.lastResistanceForce);
            if (netForce.length() > 0.01f) {
                Vector3f netForceVec = new Vector3f(netForce).mul(scale);
                tessellator.getBuffer().begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                tessellator.getBuffer().pos(0, 0, 0).color(255, 255, 0, 255).endVertex(); // 黄色
                tessellator.getBuffer().pos(netForceVec.x, netForceVec.y, netForceVec.z).color(255, 255, 0, 255).endVertex();
                tessellator.draw();

                // 绘制合力向量标签
                renderVectorLabel("合力", netForceVec, 0xFFFF00);
            }
        }

        // 恢复OpenGL状态
        GlStateManager.glLineWidth(1.0F);
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @SideOnly(Side.CLIENT)
    private void renderVectorLabel(String label, Vector3f vector, int color) {
        // 在向量末端绘制一个小球体来表示向量终点
        GlStateManager.pushMatrix();
        GlStateManager.translate(vector.x, vector.y, vector.z);

        // 绘制小球体
        Tessellator tessellator = Tessellator.getInstance();
        tessellator.getBuffer().begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

        float radius = 0.05f;
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float)(i * 2 * Math.PI / segments);
            float angle2 = (float)((i + 1) * 2 * Math.PI / segments);

            // 绘制球体的一个扇形
            tessellator.getBuffer().pos(0, 0, 0).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255).endVertex();
            tessellator.getBuffer().pos(radius * Math.cos(angle1), 0, radius * Math.sin(angle1)).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255).endVertex();
            tessellator.getBuffer().pos(radius * Math.cos(angle2), 0, radius * Math.sin(angle2)).color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, 255).endVertex();
        }
        tessellator.draw();

        GlStateManager.popMatrix();
    }

    @Override
    public IType getType() {
        // TODO Auto-generated method stub
        return this.type;
    }
}
