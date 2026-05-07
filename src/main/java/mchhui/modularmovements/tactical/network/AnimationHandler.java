package mchhui.modularmovements.tactical.network;

import java.util.UUID;

import com.modularwarfare.ModularWarfare;

import io.netty.buffer.Unpooled;
import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.network.EnumFeatures;
import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.client.ClientListener;
import mchhui.modularmovements.tactical.network.TacticalHandler.EnumPacketType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class AnimationHandler {
    public static void onHandle(ServerCustomPacketEvent event) {
        PacketBuffer buffer = (PacketBuffer) event.getPacket().payload();
        UUID uid=buffer.readUniqueId();
        String str=buffer.readString(Short.MAX_VALUE);
        int type=buffer.readVarInt();
        FMLCommonHandler.instance().getMinecraftServerInstance().addScheduledTask(()->{
            if (ModularWarfare.aniPlayer != null) {
                ModularWarfare.aniPlayer.playAni(uid, str, type);
            }
        });
    }
    
    public static void sendAnimation(UUID uid,String ani,int type) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Animation);
        buffer.writeUniqueId(uid);
        buffer.writeString(ani);
        buffer.writeVarInt(type);
        ModularMovements.channel.sendToServer(new FMLProxyPacket(buffer, "modularmovements"));
    }
}
