package mchhui.sizvehicle.network.server;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.netty.buffer.ByteBuf;
import mchhui.sizvehicle.client.handler.DebugRenderHandler;
import mchhui.sizvehicle.common.entity.EntityCar;
import mchhui.sizvehicle.common.physics.MassPoint;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 车辆状态同步数据包 - 服务器向客户端同步车辆状态
 */
public class PacketVehicleDebugState implements IMessage {
    
    private int entityID;
    private Vector3f speed;
    private Vector3f driveForce;
    private Vector3f resistanceForce;
    private Vector3f debugPoint1;
    private Vector3f debugPoint2;
    private Vector3f debugPoint3;
    private Vector3f debugPoint4;
    
    public PacketVehicleDebugState() {}
    
    public PacketVehicleDebugState(EntityCar vehicle) {
        this.entityID = vehicle.getEntityId();
        // 获取车辆的物理数据
        MassPoint massPoint = vehicle.getMassPoint();
        this.speed = new Vector3f(massPoint.getSpeed());
        this.driveForce = new Vector3f(massPoint.getLastDriveForce());
        this.resistanceForce = new Vector3f(massPoint.getLastResistanceForce());
        // 获取 debugPoint 数据
        this.debugPoint1 = vehicle.debugPoint1;
        this.debugPoint2 = vehicle.debugPoint2;
        this.debugPoint3 = vehicle.debugPoint3;
        this.debugPoint4 = vehicle.debugPoint4;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        float speedX = buf.readFloat();
        float speedY = buf.readFloat();
        float speedZ = buf.readFloat();
        speed = new Vector3f(speedX, speedY, speedZ);
        
        float driveX = buf.readFloat();
        float driveY = buf.readFloat();
        float driveZ = buf.readFloat();
        driveForce = new Vector3f(driveX, driveY, driveZ);
        
        float resistX = buf.readFloat();
        float resistY = buf.readFloat();
        float resistZ = buf.readFloat();
        resistanceForce = new Vector3f(resistX, resistY, resistZ);
        
        float debugX = buf.readFloat();
        float debugY = buf.readFloat();
        float debugZ = buf.readFloat();
        debugPoint1 = new Vector3f(debugX, debugY, debugZ);
        
        float debug2X = buf.readFloat();
        float debug2Y = buf.readFloat();
        float debug2Z = buf.readFloat();
        debugPoint2 = new Vector3f(debug2X, debug2Y, debug2Z);
        
        float debug3X = buf.readFloat();
        float debug3Y = buf.readFloat();
        float debug3Z = buf.readFloat();
        debugPoint3 = new Vector3f(debug3X, debug3Y, debug3Z);
        
        float debug4X = buf.readFloat();
        float debug4Y = buf.readFloat();
        float debug4Z = buf.readFloat();
        debugPoint4 = new Vector3f(debug4X, debug4Y, debug4Z);
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeFloat(speed.x);
        buf.writeFloat(speed.y);
        buf.writeFloat(speed.z);
        buf.writeFloat(driveForce.x);
        buf.writeFloat(driveForce.y);
        buf.writeFloat(driveForce.z);
        buf.writeFloat(resistanceForce.x);
        buf.writeFloat(resistanceForce.y);
        buf.writeFloat(resistanceForce.z);
        buf.writeFloat(debugPoint1.x);
        buf.writeFloat(debugPoint1.y);
        buf.writeFloat(debugPoint1.z);
        buf.writeFloat(debugPoint2.x);
        buf.writeFloat(debugPoint2.y);
        buf.writeFloat(debugPoint2.z);
        buf.writeFloat(debugPoint3.x);
        buf.writeFloat(debugPoint3.y);
        buf.writeFloat(debugPoint3.z);
        buf.writeFloat(debugPoint4.x);
        buf.writeFloat(debugPoint4.y);
        buf.writeFloat(debugPoint4.z);
    }
    
    /**
     * 车辆状态同步数据包处理器
     */
    public static class Handler implements IMessageHandler<PacketVehicleDebugState, IMessage> {
        
        @Override
        public IMessage onMessage(PacketVehicleDebugState message, MessageContext ctx) {
            // 在客户端主线程中处理
            Minecraft.getMinecraft().addScheduledTask(() -> {
                // 更新DebugHUDHandler的数据
                DebugRenderHandler debugHandler = DebugRenderHandler.INSTANCE;
                if (debugHandler != null) {
                    debugHandler.entityID = message.entityID;
                    debugHandler.speed = message.speed;
                    debugHandler.driveForce = message.driveForce;
                    debugHandler.resistanceForce = message.resistanceForce;
                    debugHandler.debugPoint1 = message.debugPoint1;
                    debugHandler.debugPoint2 = message.debugPoint2;
                    debugHandler.debugPoint3 = message.debugPoint3;
                    debugHandler.debugPoint4 = message.debugPoint4;
                }
            });
            
            return null; // 不需要回复
        }
    }
} 