package mchhui.sizvehicle.network;

import net.minecraft.entity.player.EntityPlayerMP;
import mchhui.sizvehicle.common.entity.EntityCar;
import mchhui.sizvehicle.network.server.PacketVehicleDebugState;
import mchhui.sizvehicle.network.server.PacketVehiclePose;

public class ServerSIZVehicle {
    public static void boardCastVehiclePose(EntityCar vehicle) {
        // 创建车辆状态同步数据包
        PacketVehiclePose packet = new PacketVehiclePose(vehicle);

        // 向所有追踪该车辆的玩家发送数据包
        NetworkManager.sendToAllTracking(packet, vehicle);
    }

    public static void sendDebugVehicleState(EntityPlayerMP player) {
        if (player.getRidingEntity() instanceof EntityCar) {
            NetworkManager.sendToPlayer(new PacketVehicleDebugState((EntityCar)player.getRidingEntity()), player);
        }
    }
}
