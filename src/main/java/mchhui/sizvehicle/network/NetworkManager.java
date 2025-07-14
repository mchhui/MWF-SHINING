package mchhui.sizvehicle.network;

import mchhui.sizvehicle.ModSIZVehicle;
import mchhui.sizvehicle.network.client.PacketPlayerDriveInput;
import mchhui.sizvehicle.network.server.PacketVehicleState;
import mchhui.sizvehicle.network.server.PacketVehicleDebugState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 网络管理器 - 负责处理客户端和服务器之间的网络通信
 */
public class NetworkManager {
    
    private static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("sizvehicle");
    private static int packetId = 0;
    
    /**
     * 初始化网络管理器
     */
    public static void init() {
        
        // 注册车辆状态同步数据包
        registerPacket(PacketVehicleState.class, PacketVehicleState.Handler.class, Side.CLIENT);
        
        // 注册车辆调试状态数据包
        registerPacket(PacketVehicleDebugState.class, PacketVehicleDebugState.Handler.class, Side.CLIENT);
        
        // 注册玩家驾驶输入数据包
        registerPacket(PacketPlayerDriveInput.class, PacketPlayerDriveInput.Handler.class, Side.SERVER);
    }
    
    /**
     * 注册数据包
     */
    private static <REQ extends IMessage, REPLY extends IMessage> void registerPacket(
            Class<REQ> messageType, Class<? extends IMessageHandler<REQ, REPLY>> handlerClass, Side side) {
        INSTANCE.registerMessage(handlerClass, messageType, packetId++, side);
    }
    
    /**
     * 发送数据包到服务器
     */
    public static void sendToServer(IMessage message) {
        INSTANCE.sendToServer(message);
    }
    
    /**
     * 发送数据包到指定玩家
     */
    public static void sendToPlayer(IMessage message, EntityPlayerMP player) {
        INSTANCE.sendTo(message, player);
    }
    
    /**
     * 发送数据包到所有玩家
     */
    public static void sendToAll(IMessage message) {
        INSTANCE.sendToAll(message);
    }
    
    /**
     * 发送数据包到所有追踪指定实体的玩家
     */
    public static void sendToAllTracking(IMessage message, Entity entity) {
        INSTANCE.sendToAllTracking(message, entity);
    }
    
    /**
     * 获取网络包装器实例
     */
    public static SimpleNetworkWrapper getInstance() {
        return INSTANCE;
    }
} 