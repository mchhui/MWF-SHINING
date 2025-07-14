package mchhui.sizvehicle.common.entity;

public interface IControllableVehicleGround {
    public void setPlayerInput(float powerFactor, float angleFactor, boolean brake, boolean shift);

    public float getInputPowerFactor();

    public float getInputAngleFactor();

    public boolean isInputBrake();

    public boolean isInputShift();

    public float getMaxForwardSpeed();

    public float getMaxBackwardSpeed();

    public float getMaxForwardAcceleration();

    public float getMaxBackwardAcceleration();

    public float getMaxWhellAngle();

    public float getBrakeAcceleration();

    public float getRelativeFrictionCoefficient();

    public float getRelativeFrictionCoefficientInSteering();

    public float getMaxSafeSteeringSpeed();
}
