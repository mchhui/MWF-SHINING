package mchhui.modularmovements.tactical.network;

import io.netty.buffer.Unpooled;
import mchhui.modularmovements.ModularMovements;
import mchhui.modularmovements.network.EnumFeatures;
import mchhui.modularmovements.tactical.PlayerState;
import mchhui.modularmovements.tactical.client.ClientListener;
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
                if (!ClientListener.ohterPlayerStateMap.containsKey(id)) {
                    ClientListener.ohterPlayerStateMap.put(id, new PlayerState());
                }
                state = ClientListener.ohterPlayerStateMap.get(id);
                state.readCode(code);
                break;
            case SET_STATE:
                int client_code = buffer.readInt();
                ClientListener.clientPlayerState.readCode(client_code);
                break;

            case MOD_CONFIG:
                // Lean 配置
                boolean leanEnable = buffer.readBoolean();
                boolean withGunsOnly = buffer.readBoolean();
                
                // Sit 配置
                boolean sitEnable = buffer.readBoolean();
                int sitCooldownMs = buffer.readInt();
                
                // Slide 配置
                boolean slideEnable = buffer.readBoolean();
                float slideMaxForce = buffer.readFloat();
                
                // Crawl 配置
                boolean crawlEnable = buffer.readBoolean();
                int crawlCooldownMs = buffer.readInt();
                
                // Dodge 配置
                boolean dodgeEnable = buffer.readBoolean();
                int divingThresholdMs = buffer.readInt();
                int pounceCooldownMs = buffer.readInt();
                int pounceGroundCooldownMs = buffer.readInt();
                int rollCooldownMs = buffer.readInt();
                int rollSecondCooldownMs = buffer.readInt();

                // 应用配置
                ModularMovements.REMOTE_CONFIG.lean.enable = leanEnable;
                ModularMovements.REMOTE_CONFIG.lean.withGunsOnly = withGunsOnly;
                
                ModularMovements.REMOTE_CONFIG.sit.enable = sitEnable;
                ModularMovements.REMOTE_CONFIG.sit.cooldownMs = sitCooldownMs;
                
                ModularMovements.REMOTE_CONFIG.slide.enable = slideEnable;
                ModularMovements.REMOTE_CONFIG.slide.maxForce = slideMaxForce;
                
                ModularMovements.REMOTE_CONFIG.crawl.enable = crawlEnable;
                ModularMovements.REMOTE_CONFIG.crawl.cooldownMs = crawlCooldownMs;
                
                ModularMovements.REMOTE_CONFIG.dodge.enable = dodgeEnable;
                ModularMovements.REMOTE_CONFIG.dodge.divingThresholdMs = divingThresholdMs;
                ModularMovements.REMOTE_CONFIG.dodge.pounceCooldownMs = pounceCooldownMs;
                ModularMovements.REMOTE_CONFIG.dodge.pounceGroundCooldownMs = pounceGroundCooldownMs;
                ModularMovements.REMOTE_CONFIG.dodge.rollCooldownMs = rollCooldownMs;
                ModularMovements.REMOTE_CONFIG.dodge.rollSecondCooldownMs = rollSecondCooldownMs;
                
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
        
        // Lean 配置
        buffer.writeBoolean(ModularMovements.CONFIG.lean.enable);
        buffer.writeBoolean(ModularMovements.CONFIG.lean.withGunsOnly);
        
        // Sit 配置
        buffer.writeBoolean(ModularMovements.CONFIG.sit.enable);
        buffer.writeInt(ModularMovements.CONFIG.sit.cooldownMs);
        
        // Slide 配置
        buffer.writeBoolean(ModularMovements.CONFIG.slide.enable);
        buffer.writeFloat(ModularMovements.CONFIG.slide.maxForce);
        
        // Crawl 配置
        buffer.writeBoolean(ModularMovements.CONFIG.crawl.enable);
        buffer.writeInt(ModularMovements.CONFIG.crawl.cooldownMs);
        
        // Dodge 配置（新增）
        buffer.writeBoolean(ModularMovements.CONFIG.dodge.enable);
        buffer.writeInt(ModularMovements.CONFIG.dodge.divingThresholdMs);
        buffer.writeInt(ModularMovements.CONFIG.dodge.pounceCooldownMs);
        buffer.writeInt(ModularMovements.CONFIG.dodge.pounceGroundCooldownMs);
        buffer.writeInt(ModularMovements.CONFIG.dodge.rollCooldownMs);
        buffer.writeInt(ModularMovements.CONFIG.dodge.rollSecondCooldownMs);
        
        ModularMovements.channel.sendTo(new FMLProxyPacket(buffer, "modularmovements"), entityPlayer);
        
    }

    public enum EnumPacketType {
        STATE, SET_STATE, NOFALL, NOSTEP, MOD_CONFIG
    }
}
