package mchhui.sizvehicle.network.client;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import io.netty.buffer.ByteBuf;
import mchhui.sizvehicle.common.entity.EntityCar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 轮子位置信息包 - 客户端向服务器发送轮子位置信息
 */
public class PacketWheelPositions implements IMessage {
    
    private int entityID;
    private Matrix4f leftFrontOffset;
    private Matrix4f rightFrontOffset;
    private Matrix4f leftBackOffset;
    private Matrix4f rightBackOffset;
    
    public PacketWheelPositions() {}
    
    public PacketWheelPositions(int entityID, Matrix4f leftFrontOffset, Matrix4f rightFrontOffset, 
                               Matrix4f leftBackOffset, Matrix4f rightBackOffset) {
        this.entityID = entityID;
        this.leftFrontOffset = new Matrix4f(leftFrontOffset);
        this.rightFrontOffset = new Matrix4f(rightFrontOffset);
        this.leftBackOffset = new Matrix4f(leftBackOffset);
        this.rightBackOffset = new Matrix4f(rightBackOffset);
    }
    
    @Override
    public void fromBytes(ByteBuf buf) {
        entityID = buf.readInt();
        
        // 读取左前轮位置矩阵
        leftFrontOffset = new Matrix4f();
        float[] values = new float[16];
        for (int i = 0; i < 16; i++) {
            values[i] = buf.readFloat();
        }
        leftFrontOffset.set(values);
        
        // 读取右前轮位置矩阵
        rightFrontOffset = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            values[i] = buf.readFloat();
        }
        rightFrontOffset.set(values);
        
        // 读取左后轮位置矩阵
        leftBackOffset = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            values[i] = buf.readFloat();
        }
        leftBackOffset.set(values);
        
        // 读取右后轮位置矩阵
        rightBackOffset = new Matrix4f();
        for (int i = 0; i < 16; i++) {
            values[i] = buf.readFloat();
        }
        rightBackOffset.set(values);
    }
    
    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityID);
        
        // 写入左前轮位置矩阵
        float[] values = new float[16];
        leftFrontOffset.get(values);
        for (int i = 0; i < 16; i++) {
            buf.writeFloat(values[i]);
        }
        
        // 写入右前轮位置矩阵
        rightFrontOffset.get(values);
        for (int i = 0; i < 16; i++) {
            buf.writeFloat(values[i]);
        }
        
        // 写入左后轮位置矩阵
        leftBackOffset.get(values);
        for (int i = 0; i < 16; i++) {
            buf.writeFloat(values[i]);
        }
        
        // 写入右后轮位置矩阵
        rightBackOffset.get(values);
        for (int i = 0; i < 16; i++) {
            buf.writeFloat(values[i]);
        }
    }
    
    /**
     * 轮子位置信息包处理器
     */
    public static class Handler implements IMessageHandler<PacketWheelPositions, IMessage> {
        
        @Override
        public IMessage onMessage(PacketWheelPositions message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 查找对应的实体
                if (player.getServerWorld().getEntityByID(message.entityID) instanceof EntityCar) {
                    EntityCar vehicle = (EntityCar) player.getServerWorld().getEntityByID(message.entityID);
                    if (vehicle != null) {
                        // 设置轮子位置偏移量
                        vehicle.setWheelOffsets(message.leftFrontOffset, message.rightFrontOffset, 
                                              message.leftBackOffset, message.rightBackOffset);
                    }
                }
            });
            
            return null;
        }
    }
} 