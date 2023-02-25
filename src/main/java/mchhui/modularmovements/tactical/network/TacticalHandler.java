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
                boolean withGunsOnly = buffer.readBoolean();
                float slideMaxForce = buffer.readFloat();
                boolean blockView = buffer.readBoolean();
                float blockAngle = buffer.readFloat();
                float sitCooldown = buffer.readFloat();
                float crawlCooldown = buffer.readFloat();

                ModularMovements.CONFIG.lean.withGunsOnly = withGunsOnly;
                ModularMovements.CONFIG.slide.maxForce = slideMaxForce;
                ModularMovements.CONFIG.crawl.blockView = blockView;
                ModularMovements.CONFIG.crawl.blockAngle = blockAngle;
                ModularMovements.CONFIG.cooldown.sitCooldown = sitCooldown;
                ModularMovements.CONFIG.cooldown.crawlCooldown = crawlCooldown;
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
            player.fallDistance = 0;
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
        buffer.writeBoolean(ModularMovements.CONFIG.lean.withGunsOnly);
        buffer.writeFloat(ModularMovements.CONFIG.slide.maxForce);
        buffer.writeBoolean(ModularMovements.CONFIG.crawl.blockView);
        buffer.writeFloat(ModularMovements.CONFIG.crawl.blockAngle);
        buffer.writeFloat(ModularMovements.CONFIG.cooldown.sitCooldown);
        buffer.writeFloat(ModularMovements.CONFIG.cooldown.crawlCooldown);
        ModularMovements.channel.sendTo(new FMLProxyPacket(buffer, "modularmovements"), entityPlayer);
    }

    public enum EnumPacketType {
        STATE, SET_STATE, NOFALL, NOSTEP, MOD_CONFIG
    }
}
