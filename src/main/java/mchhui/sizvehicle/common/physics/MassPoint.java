package mchhui.sizvehicle.common.physics;

import org.joml.Vector3f;

/**
 * @author Hueihuea
 * 
 * @apiNote 质点类，用于模拟物理系统中的质点对象。
 *          该类实现了基本的牛顿力学计算，包含质量、速度和力的属性，
 *          提供完整的物理计算功能。使用 {@link #update(float)} 方法
 *          进行物理步进计算，支持力的累积应用和速度的实时更新。
 */
public class MassPoint {
    /** 速度阈值，用于处理速度分量接近0的情况 */
    private static final float VELOCITY_THRESHOLD = 0.001f;

    //kg
    private float mass = 1;
    //m/s
    private Vector3f speed = new Vector3f(0, 0, 0);
    //N 可以反向加速
    private Vector3f driveForce = new Vector3f(0, 0, 0);
    //N 不能反向加速
    private Vector3f resistanceForce = new Vector3f(0, 0, 0);

    private Vector3f lastDriveForce = new Vector3f(0, 0, 0);
    private Vector3f lastResistanceForce = new Vector3f(0, 0, 0);

    public MassPoint() {
        // TODO Auto-generated constructor stub
    }

    public MassPoint(float mass) {
        this.mass = mass;
    }

    public float getMass() {
        return mass;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public Vector3f getSpeed() {
        return new Vector3f(speed);
    }
    
    public Vector3f getSpeed(Vector3f dest) {
        return dest.set(speed);
    }

    public Vector3f getLastDriveForce() {
        return lastDriveForce;
    }

    public Vector3f getLastResistanceForce() {
        return lastResistanceForce;
    }

    public void setSpeed(float x, float y, float z) {
        this.speed.set(x, y, z);
    }

    public void addSpeed(float x, float y, float z) {
        this.speed.add(x, y, z);
    }

    public void setDriveForce(float x, float y, float z) {
        this.driveForce.set(x, y, z);
    }

    public void addDriveForce(float x, float y, float z) {
        this.driveForce.add(x, y, z);
    }

    public void setResistanceForce(float x, float y, float z) {
        this.resistanceForce.set(x, y, z);
    }

    public void addResistanceForce(float x, float y, float z) {
        this.resistanceForce.add(x, y, z);
    }

    //计算质点物理步
    public void update(float deltaTime) {
        //f=ma;
        //deltaV=v+at;
        this.addSpeed(driveForce.x / mass * deltaTime, driveForce.y / mass * deltaTime, driveForce.z / mass * deltaTime);
        float vx = this.speed.x;
        float vy = this.speed.y;
        float vz = this.speed.z;
        this.addSpeed(resistanceForce.x / mass * deltaTime, resistanceForce.y / mass * deltaTime, resistanceForce.z / mass * deltaTime);
        vx = ((vx > 0 && speed.x < 0) || (vx < 0 && speed.x > 0)||(vx==0)) ? 0 : this.speed.x;
        vy = ((vy > 0 && speed.y < 0) || (vy < 0 && speed.y > 0)||(vy==0)) ? 0 : this.speed.y;
        vz = ((vz > 0 && speed.z < 0) || (vz < 0 && speed.z > 0)||(vz==0)) ? 0 : this.speed.z;
        //使用setter 而不是直接设置 这样子语义更明确
        this.setSpeed(vx, vy, vz);
        // 更高性能地处理速度分量接近0
        float x = speed.x, y = speed.y, z = speed.z;
        speed.x = (x > -VELOCITY_THRESHOLD && x < VELOCITY_THRESHOLD) ? 0 : x;
        speed.y = (y > -VELOCITY_THRESHOLD && y < VELOCITY_THRESHOLD) ? 0 : y;
        speed.z = (z > -VELOCITY_THRESHOLD && z < VELOCITY_THRESHOLD) ? 0 : z;
        lastDriveForce.set(driveForce);
        lastResistanceForce.set(resistanceForce);
        this.setDriveForce(0, 0, 0);
        this.setResistanceForce(0, 0, 0);
    }
}
