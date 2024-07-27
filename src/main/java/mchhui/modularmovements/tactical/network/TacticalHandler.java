package mchhui.modularmovements.tactical.network;

import io.netty.buffer.Unpooled;
import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.network.EnumFeatures;
import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.client.ClientLitener;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class TacticalHandler {
    private static PlayerState state;

    public static void onHandle(ClientCustomPacketEvent event) {
        PacketBuffer buffer = (PacketBuffer) event.getPacket().payload();
        EnumPacketType type = buffer.readEnumValue(EnumPacketType.class);

        switch (type) {
            case STATE:
                int id = buffer.readInt();
                int code = buffer.readInt();
                if (id == Minecraft.getMinecraft().player.getEntityId()) {
                    break;
                }
                if (!ClientLitener.ohterPlayerStateMap.containsKey(id)) {
                    ClientLitener.ohterPlayerStateMap.put(id, new PlayerState());
                }
                state = ClientLitener.ohterPlayerStateMap.get(id);
                state.readCode(code);
                break;
            case SET_STATE:
                int client_code = buffer.readInt();
                ClientLitener.clientPlayerState.readCode(client_code);
                break;

            case MOD_CONFIG:
                boolean leanEnable = buffer.readBoolean();
                boolean sitEnable = buffer.readBoolean();
                boolean slideEnable = buffer.readBoolean();
                boolean crawlEnable = buffer.readBoolean();
                boolean withGunsOnly = buffer.readBoolean();
                float slideMaxForce = buffer.readFloat();

                ModularMovements.REMOTE_CONFIG.lean.enable = leanEnable;
                ModularMovements.REMOTE_CONFIG.sit.enable = sitEnable;
                ModularMovements.REMOTE_CONFIG.slide.enable = slideEnable;
                ModularMovements.REMOTE_CONFIG.crawl.enable = crawlEnable;
                ModularMovements.REMOTE_CONFIG.slide.maxForce = slideMaxForce;
                break;
        }
    }

    public static void onHandle(ServerCustomPacketEvent event) {
        PacketBuffer buffer = (PacketBuffer) event.getPacket().payload();
        EntityPlayer player = ((NetHandlerPlayServer) event.getHandler()).player;
        EnumPacketType type = buffer.readEnumValue(EnumPacketType.class);

        switch (type) {
        case STATE:
            int code = buffer.readInt();
            boolean flag = false;
            if (ServerListener.playerStateMap.containsKey(player.getEntityId())) {
                state = ServerListener.playerStateMap.get(player.getEntityId());
                if (code != state.writeCode()) {
                    flag = true;
                    state.readCode(code);
                    sendToClient(player, code);
                }
            }
            break;
        case NOFALL:
//            player.fallDistance = 0;
            break;
        case NOSTEP:
            int time = buffer.readInt();
            ServerListener.playerNotStepMap.put(player.getEntityId(), System.currentTimeMillis() + time);
            break;
        default:
            break;
        }
    }

    public static void sendToClient(EntityPlayer entityPlayer, int code) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.STATE);
        buffer.writeInt(entityPlayer.getEntityId());
        buffer.writeInt(code);
        ModularMovements.channel.sendToAll(new FMLProxyPacket(buffer, "modularmovements"));
    }

    public static void sendToServer(int code) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.STATE);
        buffer.writeInt(code);
        ModularMovements.channel.sendToServer(new FMLProxyPacket(buffer, "modularmovements"));
    }

    public static void sendStateSettng(EntityPlayerMP entityPlayer, int code) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.SET_STATE);
        buffer.writeInt(code);
        ModularMovements.channel.sendTo(new FMLProxyPacket(buffer, "modularmovements"), entityPlayer);
    }

    public static void sendNoFall() {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.NOFALL);
        ModularMovements.channel.sendToServer(new FMLProxyPacket(buffer, "modularmovements"));
    }

    public static void sendNoStep(int time) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.NOSTEP);
        buffer.writeInt(time);
        ModularMovements.channel.sendToServer(new FMLProxyPacket(buffer, "modularmovements"));
    }

    public static void sendClientConfig(EntityPlayerMP entityPlayer) {
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeEnumValue(EnumFeatures.Tactical);
        buffer.writeEnumValue(EnumPacketType.MOD_CONFIG);
        
        buffer.writeBoolean(ModularMovements.CONFIG.lean.enable);
        buffer.writeBoolean(ModularMovements.CONFIG.sit.enable);
        buffer.writeBoolean(ModularMovements.CONFIG.slide.enable);
        buffer.writeBoolean(ModularMovements.CONFIG.crawl.enable);
        
        buffer.writeBoolean(ModularMovements.CONFIG.lean.withGunsOnly);
        buffer.writeFloat(ModularMovements.CONFIG.slide.maxForce);
        
        ModularMovements.channel.sendTo(new FMLProxyPacket(buffer, "modularmovements"), entityPlayer);
    }

    public enum EnumPacketType {
        STATE, SET_STATE, NOFALL, NOSTEP, MOD_CONFIG
    }
}
