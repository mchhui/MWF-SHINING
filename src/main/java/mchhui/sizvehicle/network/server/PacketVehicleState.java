package mchhui.sizvehicle.network.server;

import org.joml.Quaternionf;

import io.netty.buffer.ByteBuf;
import mchhui.sizvehicle.common.entity.EntityCar;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 车辆状态同步数据包 - 服务器向客户端同步车辆状态
 */
public class PacketVehicleState implements IMessage {
    
    private int entityID;
    private Quaternionf pose;
    private float inputAngleFactor;
    private boolean inputBrake;
    private boolean inputShift;
    private float speed;
    private float whellProcess;
    
    public PacketVehicleState() {}
    
    public PacketVehicleState(EntityCar vehicle) {
        this.entityID = vehicle.getEntityId();
        this.pose = new Quaternionf(vehicle.getPose().getQuaternion());
        this.inputAngleFactor=vehicle.getInputAngleFactor();
        this.inputBrake=vehicle.isInputBrake();
        this.inputShift=vehicle.isInputShift();
        this.speed=vehicle.speed;
        this.whellProcess=vehicle.whellProcess;
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float w = buf.readFloat();
        pose = new Quaternionf(x, y, z, w);
        inputAngleFactor = buf.readFloat();
        inputBrake = buf.readBoolean();
        inputShift = buf.readBoolean();
        speed = buf.readFloat();
        whellProcess = buf.readFloat();
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        buf.writeFloat(pose.x);
        buf.writeFloat(pose.y);
        buf.writeFloat(pose.z);
        buf.writeFloat(pose.w);
        buf.writeFloat(inputAngleFactor);
        buf.writeBoolean(inputBrake);
        buf.writeBoolean(inputShift);
        buf.writeFloat(speed);
        buf.writeFloat(whellProcess);
    }
    
    /**
     * 车辆状态同步数据包处理器
     */
    public static class Handler implements IMessageHandler<PacketVehicleState, IMessage> {
        
        @Override
        public IMessage onMessage(PacketVehicleState message, MessageContext ctx) {
            // 在客户端主线程中处理
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if(Minecraft.getMinecraft().world.getEntityByID(message.entityID) instanceof EntityCar) {
                    EntityCar vehicle = (EntityCar) Minecraft.getMinecraft().world.getEntityByID(message.entityID);
                    if(vehicle!=null) {
                        // 使用插值方法设置服务器状态
                        vehicle.setServerState(message.speed, message.pose);
                        // 仍然需要同步输入状态用于渲染
                        vehicle.setPlayerInput(0, message.inputAngleFactor, message.inputBrake, message.inputShift);
                        // 同步轮胎旋转状态
                        vehicle.lastWhellProcess = vehicle.whellProcess;
                        vehicle.whellProcess = message.whellProcess;
                    }
                }
            });
            
            return null;
        }
    }
} 