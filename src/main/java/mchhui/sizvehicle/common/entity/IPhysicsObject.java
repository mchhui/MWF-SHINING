package mchhui.sizvehicle.common.entity;

import mchhui.sizvehicle.common.physics.MassPoint;
import mchhui.sizvehicle.common.physics.Pose;

public interface IPhysicsObject {
    public MassPoint getMassPoint();

    public Pose getPose();
}
