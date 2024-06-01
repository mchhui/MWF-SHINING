package mchhui.modularmovements.network;

import mchhui.modularmovements.tactical.network.TacticalHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;

public class Handler {
    @SubscribeEvent
    public void onHandle(ClientCustomPacketEvent event) {
        PacketBuffer buffer = (PacketBuffer) event.getPacket().payload();
        EnumFeatures type= buffer.readEnumValue(EnumFeatures.class);
        switch (type) {
        case Tactical:
            TacticalHandler.onHandle(event);
            break;
        default:
            break;
        }
        event.getPacket().payload().release();
    }

    @SubscribeEvent
    public void onHandle(ServerCustomPacketEvent event) {
        PacketBuffer buffer = (PacketBuffer) event.getPacket().payload();
        EnumFeatures type=buffer.readEnumValue(EnumFeatures.class);
        switch (type) {
        case Tactical:
            TacticalHandler.onHandle(event);
            break;
        default:
            break;
        }
        event.getPacket().payload().release();
    }
}
