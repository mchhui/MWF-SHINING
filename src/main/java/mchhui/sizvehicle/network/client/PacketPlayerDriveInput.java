package mchhui.sizvehicle.network.client;

import io.netty.buffer.ByteBuf;
import mchhui.sizvehicle.common.entity.EntityCar;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 玩家驾驶输入数据包 - 客户端向服务器发送驾驶控制输入
 */
public class PacketPlayerDriveInput implements IMessage {

    public float powerFactor;
    public float angleFactor;
    public boolean brake;
    public boolean shift;

    public PacketPlayerDriveInput() {}

    public PacketPlayerDriveInput(float powerFactor, float angleFactor, boolean brake,boolean shift) {
        this.powerFactor = powerFactor;
        this.angleFactor = angleFactor;
        this.brake = brake;
        this.shift=shift;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        powerFactor = buf.readFloat();
        angleFactor = buf.readFloat();
        brake = buf.readBoolean();
        shift = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeFloat(powerFactor);
        buf.writeFloat(angleFactor);
        buf.writeBoolean(brake);
        buf.writeBoolean(shift);
    }

    /**
     * 玩家驾驶输入数据包处理器
     */
    public static class Handler implements IMessageHandler<PacketPlayerDriveInput, IMessage> {

        @Override
        public IMessage onMessage(PacketPlayerDriveInput message, MessageContext ctx) {
            // 在服务器主线程中处理
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> {
                // 检查玩家是否在驾驶车辆
                if (player.getRidingEntity() instanceof EntityCar) {
                    EntityCar vehicle = (EntityCar)player.getRidingEntity();
                    // 应用驾驶输入到车辆
                    vehicle.setPlayerInput(message.powerFactor, message.angleFactor, message.brake, message.shift);
                }
            });

            return null; // 不需要回复
        }
    }
}
