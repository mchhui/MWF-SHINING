package safx.client;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelCow;
import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.CommonProxy;
import safx.SagerFX;
import safx.SAConfig;

import safx.client.particle.DeathEffect;
import safx.client.particle.SAFX;
import safx.client.particle.SAParticleManager;
import safx.client.particle.SAParticleSystem;
import safx.client.particle.DeathEffect.GoreData;

import safx.deatheffects.EntityDeathUtils.DeathType;
import safx.debug.Keybinds;

import safx.util.EntityCondition;
import safx.util.MathUtil;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	
	public SAParticleManager particleManager = new SAParticleManager();
	
	public static Field Field_ItemRenderer_equippedProgressMainhand = ObfuscationReflectionHelper.findField(ItemRenderer.class, "field_187469_f"); //ReflectionHelper.findField(ItemRenderer.class, "equippedProgressMainHand", "field_187469_f");
	public static Field Field_ItemRenderer_equippedProgressOffhand =  ObfuscationReflectionHelper.findField(ItemRenderer.class, "field_187471_h");//ReflectionHelper.findField(ItemRenderer.class, "equippedProgressOffHand", "field_187471_h");
	public static Field Field_ItemRenderer_prevEquippedProgressMainhand = ObfuscationReflectionHelper.findField(ItemRenderer.class, "field_187470_g");//ReflectionHelper.findField(ItemRenderer.class, "prevEquippedProgressMainHand", "field_187470_g");
	public static Field Field_ItemRenderer_prevEquippedProgressOffhand = ObfuscationReflectionHelper.findField(ItemRenderer.class, "field_187472_i");//ReflectionHelper.findField(ItemRenderer.class, "prevEquippedProgressOffHand", "field_187472_i");
	
	protected static Field RenderPlayer_LayerRenderers = ObfuscationReflectionHelper.findField(RenderLivingBase.class, "field_177097_h");//ReflectionHelper.findField(RenderLivingBase.class, "layerRenderers", "field_177097_h");
	
	/**
	 * if local player has pressed the fire key this tick
	 */
	public boolean keyFirePressedMainhand;
	public boolean keyFirePressedOffhand;

	@SideOnly(Side.CLIENT)
	public float player_zoom=1.0f;
	
	//client side safeguard to prevent multiple reloadsounds overlapping caused by deyncs
	public long lastReloadsoundPlayed=0L;
	
	//local player First person muzzle flash timing
	protected long player_muzzleFlashtime_right=0;
	protected long player_muzzleFlashtime_total_right=0;
	protected long player_muzzleFlashtime_left=0;
	protected long player_muzzleFlashtime_total_left=0;
	
	public float PARTIAL_TICK_TIME;
	//local muzzle flash jitter offsets
	public float muzzleFlashJitterX = 0; //-1.0 to 1.0
	public float muzzleFlashJitterY = 0; //-1.0 to 1.0
	public float muzzleFlashJitterAngle = 0; //-1.0 to 1.0
	public float muzzleFlashJitterScale = 0; //-1.0 to 1.0
	
	public boolean hasStepassist=false;
	
	public boolean hasNightvision=false;

	private boolean isLeft(EnumHand handIn){
		if(this.getPlayerClient().getPrimaryHand()==EnumHandSide.RIGHT){
			return handIn == EnumHand.OFF_HAND;
		} else {
			return handIn == EnumHand.MAIN_HAND;
		}
	}
	
	private boolean isLeft(boolean left){
		if(this.getPlayerClient().getPrimaryHand()==EnumHandSide.RIGHT){
			return left;
		} else {
			return !left;
		}
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent event) {
		super.preInit(event);
		OBJLoader.INSTANCE.addDomain(SagerFX.MODID);
		SAFX.loadFXList();
	}

	@Override
	public void init(FMLInitializationEvent event) {
		super.init(event);
		
		if(SAConfig.debug) {
			Keybinds.init(); //Debuging Keybinds
		} 
	}
	
	@Override
	public void postInit(FMLPostInitializationEvent event) {
		super.postInit(event);
		DeathEffect.postInit();
	}

	public static ClientProxy get(){
		return (ClientProxy) SagerFX.proxy;
	}

	@Override
	public EntityPlayer getPlayerClient() {
		return Minecraft.getMinecraft().player;
	}
	
	@Override
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ){	
		List<SAParticleSystem> systems = SAFX.createFX(world, name, posX, posY, posZ, motionX, motionY, motionZ);
		if (systems!=null) {
			systems.forEach(s -> particleManager.addEffect(s));//Minecraft.getMinecraft().effectRenderer.addEffect(s));
		}
	}
	
	@Override
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float pitch, float yaw){	
		List<SAParticleSystem> systems = SAFX.createFX(world, name, posX, posY, posZ, motionX, motionY, motionZ);
		if (systems!=null) {
			for (SAParticleSystem s : systems) {
				s.rotationPitch = pitch;
				s.rotationYaw = yaw;
				particleManager.addEffect(s);
			}
		}
	}
	
	@Override
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float scale){	
		List<SAParticleSystem> systems = SAFX.createFX(world, name, posX, posY, posZ, motionX, motionY, motionZ);
		if (systems!=null) {
			for (SAParticleSystem s : systems) {
				s.scale = scale;
				particleManager.addEffect(s);
			}
		}
	}
	
	@Override
	public void createFXOnEntity(String name, Entity ent) {
		List<SAParticleSystem> systems = SAFX.createFXOnEntity(ent, name);
		if (systems!=null) {
			systems.forEach(s -> {
				s.condition=EntityCondition.ENTITY_ALIVE;
				particleManager.addEffect(s);
			}); //Minecraft.getMinecraft().effectRenderer.addEffect(s));
		}
	}
	
	@Override
	public void createFXOnEntity(String name, Entity ent, float scale) {
		List<SAParticleSystem> systems = SAFX.createFXOnEntity(ent, name);
		if (systems!=null) {
			for (SAParticleSystem s : systems) {
				s.scale = scale;
				s.condition=EntityCondition.ENTITY_ALIVE;
				particleManager.addEffect(s);
			}
		}
	}
	
	@Override
	public void createFXOnEntityWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, EntityCondition condition) {
		List<SAParticleSystem> systems = SAFX.createFXOnEntity(ent, name);
		if (systems!=null) {
			for (SAParticleSystem s : systems) {
				s.entityOffset = new Vec3d(offsetX, offsetY, offsetZ);
				s.attachToHead = attachToHead;
				s.condition = condition;
				particleManager.addEffect(s);
			}
		}
	}
	
	@Override
	public void createFXOnPlayerWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, float scale) {
		List<SAParticleSystem> systems = SAFX.createFXOnEntity(ent, name);
		if (systems!=null) {
			for (SAParticleSystem s : systems) {
				s.scale = scale;
				s.entityOffset = new Vec3d(offsetX, offsetY, offsetZ);
				s.attachToHead = attachToHead;
				//s.condition = condition;
				particleManager.addEffect(s);
			}
		}
	}
	
	@Override
	public void setHasStepassist(boolean value) {
		this.hasStepassist=value;
	}

	@Override
	public void setHasNightvision(boolean value) {
		this.hasNightvision=value;
	}

	@Override
	public boolean getHasStepassist() {
		return this.hasStepassist;
	}

	@Override
	public boolean getHasNightvision() {
		return this.hasNightvision;
	}

	/*@Override
	public void registerCapabilities() {
		super.registerCapabilities();
		CapabilityManager.INSTANCE.register(SADeathTypeCap.class, new SADeathTypeCapStorage(), () -> new SADeathTypeCap(null));
	}*/
	
	/**
	 * EntityDeathType
	**/
	/*
	public void setEntityDeathType(EntityLivingBase entity, DeathType deathtype){
		SADeathTypeCap cap = SADeathTypeCap.get(entity);
		cap.setDeathType(deathtype);
	}
	
	public DeathType getEntityDeathType(EntityLivingBase entity) {
		return SADeathTypeCap.get(entity).getDeathType();
	}
	
	@Deprecated
	public boolean hasDeathType(EntityLivingBase entity) {
		return SADeathTypeCap.get(entity).getDeathType() == DeathType.DEFAULT;
	}
	
	@Deprecated
	public void clearEntityDeathType(EntityLivingBase entity) {
		setEntityDeathType(entity, DeathType.DEFAULT);
	}*/
		
	@Override
	public boolean isClientPlayerAndIn1stPerson(EntityLivingBase ent) {
		return ent == this.getPlayerClient() && Minecraft.getMinecraft().gameSettings.thirdPersonView == 0;
	}

	public String resolvePlayerNameFromUUID(UUID uuid){
		String name = UsernameCache.getLastKnownUsername(uuid);
		if (name!=null){
			return name;
		} else {
			return "UNKNOW_PLAYERNAME";
		}
	}
	
	@Override
	public boolean clientInRangeSquared(double posX, double posZ, double distSq) {
		EntityPlayer localPly = this.getPlayerClient();
		
		MathUtil.Vec2 posPly = new MathUtil.Vec2(localPly.posX, localPly.posZ);
		MathUtil.Vec2 pos = new MathUtil.Vec2(posX, posZ);
		
		return posPly.getVecTo(pos).lenSquared() <= distSq;
	}
	
	protected static ResourceLocation[] getTextures(String name, String ...suffixes) {
		ResourceLocation[] tex = new ResourceLocation[suffixes.length+1];
		tex[0]=new ResourceLocation(SagerFX.MODID, name+".png");
		
		for(int i=1; i<=suffixes.length; i++) {
			tex[i] = new ResourceLocation(SagerFX.MODID, name+"_"+suffixes[i-1]+".png");
		}
		
		return tex;
		
	}
}
