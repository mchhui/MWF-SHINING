package mchhui.sizvehicle.network.server;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import io.netty.buffer.ByteBuf;
import mchhui.sizvehicle.client.handler.DebugHUDHandler;
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
    
    public PacketVehicleDebugState() {}
    
    public PacketVehicleDebugState(EntityCar vehicle) {
        this.entityID = vehicle.getEntityId();
        // 获取车辆的物理数据
        MassPoint massPoint = vehicle.getMassPoint();
        this.speed = new Vector3f(massPoint.getSpeed());
        this.driveForce = new Vector3f(massPoint.getLastDriveForce());
        this.resistanceForce = new Vector3f(massPoint.getLastResistanceForce());
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
                DebugHUDHandler debugHandler = DebugHUDHandler.INSTANCE;
                if (debugHandler != null) {
                    debugHandler.entityID = message.entityID;
                    debugHandler.speed = message.speed;
                    debugHandler.driveForce = message.driveForce;
                    debugHandler.resistanceForce = message.resistanceForce;
                }
            });
            
            return null; // 不需要回复
        }
    }
} 