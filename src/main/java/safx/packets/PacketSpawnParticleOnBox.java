package safx.packets;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import safx.client.ClientProxy;
import safx.util.SALogger;

public class PacketSpawnParticleOnBox implements IMessage {

	String name;
	int entityID;
	String boxName;
	long duration;
	float scale = 1.0f;
	boolean enableSmoothing = false;
	int smoothingSubdivisions = 3;
	
	public PacketSpawnParticleOnBox() {};
	
	public PacketSpawnParticleOnBox(String name, Entity ent, String boxName, long duration) {
		this(name, ent, boxName, duration, 1.0f, false, 3);
	}
	
	public PacketSpawnParticleOnBox(String name, Entity ent, String boxName, long duration, float scale) {
		this(name, ent, boxName, duration, scale, false, 3);
	}
	
	public PacketSpawnParticleOnBox(String name, Entity ent, String boxName, long duration, float scale, boolean enableSmoothing) {
		this(name, ent, boxName, duration, scale, enableSmoothing, 3);
	}
	
	public PacketSpawnParticleOnBox(String name, Entity ent, String boxName, long duration, float scale, boolean enableSmoothing, int smoothingSubdivisions) {
		super();
		this.name = name;
		this.entityID = ent.getEntityId();
		this.boxName = boxName;
		this.duration = duration;
		this.scale = scale;
		this.enableSmoothing = enableSmoothing;
		this.smoothingSubdivisions = smoothingSubdivisions;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		int len = buf.readShort();
		this.name = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();
		
		this.entityID = buf.readInt();
		
		int boxLen = buf.readShort();
		this.boxName = buf.readCharSequence(boxLen, StandardCharsets.UTF_8).toString();
		
		this.duration = buf.readLong();
		this.scale = buf.readFloat();
		this.enableSmoothing = buf.readBoolean();
		this.smoothingSubdivisions = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		CharSequence cs = this.name;
		buf.writeShort(cs.length());
		buf.writeCharSequence(name, StandardCharsets.UTF_8);
		
		buf.writeInt(entityID);
		
		CharSequence boxCs = this.boxName;
		buf.writeShort(boxCs.length());
		buf.writeCharSequence(boxName, StandardCharsets.UTF_8);
		
		buf.writeLong(this.duration);
		buf.writeFloat(this.scale);
		buf.writeBoolean(this.enableSmoothing);
		buf.writeInt(this.smoothingSubdivisions);
	}
	
	public static class Handler implements IMessageHandler<PacketSpawnParticleOnBox, IMessage> {
		@Override
		public IMessage onMessage(PacketSpawnParticleOnBox message, MessageContext ctx) {
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
			return null;
		}

		private void handle(PacketSpawnParticleOnBox m, MessageContext ctx) {
			Entity ent = Minecraft.getMinecraft().player.world.getEntityByID(m.entityID);
			if (ent != null) {
				ClientProxy.get().createFXOnBox(m.name, ent, m.boxName, m.duration, m.scale, m.enableSmoothing, m.smoothingSubdivisions);
			} else {
				SALogger.logger_client.warning("Got Packet for FX " + m.name + " on Box, but ent was null");
			}
		}
	}
}
