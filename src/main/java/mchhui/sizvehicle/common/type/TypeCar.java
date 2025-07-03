package mchhui.sizvehicle.common.type;

public class TypeCar implements IType{
    //基础驾驶参数
    public float maxForwardSpeed;
    public float maxBackwardSpeed;
    public float maxForwardAcceleration;
    public float maxBackwardAcceleration;
    public float maxWhellAngle;
    public float brakeAcceleration;
    public float relativeFrictionCoefficient=1;
    public float relativeFrictionCoefficientInSteering=1.5f;
    
    /**
     * 俩个作用效果
     * 一、若速度过大，则转向时强制减速
     * 二、若速度过大，则转向时转向角减小（更难转弯）
     * */
    public float maxSafeSteeringSpeed;
}
