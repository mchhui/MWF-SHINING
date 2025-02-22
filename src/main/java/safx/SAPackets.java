package safx;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import safx.init.ISAInitializer;

import safx.packets.PacketEntityDeathType;
import safx.packets.PacketSpawnParticle;
import safx.packets.PacketSpawnParticleOnEntity;

/**
 * Class for dealing with packets
*
*/
public class SAPackets implements ISAInitializer {
	public static SimpleNetworkWrapper network;
	
	public static EntityPlayer getPlayerFromContext(MessageContext ctx){
		EntityPlayer thePlayer = (ctx.side.isClient() ? SagerFX.proxy.getPlayerClient() : ctx.getServerHandler().player);
		return thePlayer;
	}

	public static TargetPoint targetPointAroundBlockPos(int dimension, BlockPos pos, double distance){
		return new TargetPoint(dimension, pos.getX()+0.5d, pos.getY()+0.5d, pos.getZ()+0.5d, distance);
	}
	
	public static TargetPoint targetPointAroundEnt(Entity ent, double distance){
		return new TargetPoint(ent.dimension, ent.posX, ent.posY, ent.posZ, distance);
	}
	
	public static TargetPoint targetPointAroundEnt(TileEntity ent, double distance){
		return new TargetPoint(ent.getWorld().provider.getDimension(), ent.getPos().getX(), ent.getPos().getY(), ent.getPos().getZ(), distance);
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent event) {
		
	}

	@Override
	public void init(FMLInitializationEvent event) {

		network = NetworkRegistry.INSTANCE.newSimpleChannel(SagerFX.MODID);
		int packetid=0;
		
		network.registerMessage(PacketSpawnParticle.Handler.class, PacketSpawnParticle.class, packetid++, Side.CLIENT);
		network.registerMessage(PacketSpawnParticleOnEntity.Handler.class, PacketSpawnParticleOnEntity.class, packetid++, Side.CLIENT);
		//network.registerMessage(PacketEntityDeathType.Handler.class, PacketEntityDeathType.class,  packetid++, Side.CLIENT);
		//network.registerMessage(PacketGunImpactFX.Handler.class, PacketGunImpactFX.class, packetid++, Side.CLIENT);
	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		
	}
}