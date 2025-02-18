package safx;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import safx.init.ISAInitializer;
import safx.util.EntityCondition;

@Mod.EventBusSubscriber
public abstract class CommonProxy implements ISAInitializer {

	@Override
	public void preInit(FMLPreInitializationEvent event) {

	}

	@Override
	public void init(FMLInitializationEvent event) {

	}

	@Override
	public void postInit(FMLPostInitializationEvent event) {
		
	}

	@SubscribeEvent
	public static void RecipeRegistryEvent(RegistryEvent.Register<IRecipe> event) {
		
	}
	
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		
	}

	@SubscribeEvent
	public static void registerItems(RegistryEvent.Register<Item> event) {

	}
	
	@SubscribeEvent
	public static void registerPotions(RegistryEvent.Register<Potion> event) {

	}
	
	@SubscribeEvent
    public static void registerPotionTypes(RegistryEvent.Register<PotionType> event)
    {

    }

	public EntityPlayer getPlayerClient() {
		return null;
	}
    
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
		
	}
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float pitch, float yaw) {
		
	}
	public void createFX(String name, World world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ, float scale) {
		
	}
	public void createFXOnEntity(String name, Entity ent) {

	}
	public void createFXOnEntity(String name, Entity ent, float scale) {

	}
	public void createFXOnEntityWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, EntityCondition condition) {};
	public void createFXOnPlayerWithOffset(String name, Entity ent, float offsetX, float offsetY, float offsetZ, boolean attachToHead, float scale) {};
	
    public void setHasStepassist(boolean value){};
    
    public void setHasNightvision(boolean value){};
    
    public void setFlySpeed(float value){};
    
    public boolean getHasStepassist(){
    	return false;
    };
    public boolean getHasNightvision(){
    	return false;
    };
    public boolean isClientPlayerAndIn1stPerson(EntityLivingBase ent) {
		return false;
	}
    public boolean clientInRangeSquared(double posX, double posZ, double distSq) {
		return false;
	}
    public void registerFluidModelsForFluidBlock(Block b) {};
    
    public void clearItemParticleSystemsHand(EntityLivingBase entity, EnumHand hand) {
	}
	public void handlePlayerGliding(EntityPlayer player) {
		//do nothing on server
	}
	
}
