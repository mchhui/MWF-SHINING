package safx.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped.ArmPose;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.FOVUpdateEvent;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.event.sound.SoundEvent.SoundSourceEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.BlockEvent.HarvestDropsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import safx.SAPackets;
import safx.SagerFX;

import safx.client.ClientProxy;

import safx.client.render.GLStateSnapshot;
import safx.client.render.entities.projectiles.DeathEffectEntityRenderer;
import safx.deatheffects.EntityDeathUtils;
import safx.deatheffects.EntityDeathUtils.DeathType;

import safx.packets.PacketEntityDeathType;

@Mod.EventBusSubscriber(modid = SagerFX.MODID)
public class SAEventHandler {
	
	/*@SideOnly(Side.CLIENT)
	@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=false)
	public static void onRenderLivingEventPre(RenderLivingEvent.Pre event){
		/*
		 * ENTITY DEATH EFFECTS
		 
		ClientProxy cp = ClientProxy.get();
		DeathType dt = cp.getEntityDeathType(event.getEntity());
		switch (dt) {
		case GORE:
			event.setCanceled(true);
			break;
		case DISMEMBER:
		case BIO:
		case LASER:
			//TODO
			event.setCanceled(true);
			DeathEffectEntityRenderer.doRender(event.getRenderer(), event.getEntity(), event.getX(), event.getY(), event.getZ(), 0f, dt);
			break;
		case DEFAULT:
		default:
			break;
		}
	}*/
	
	/*@SubscribeEvent(priority=EventPriority.NORMAL, receiveCanceled=false)
	public static void onLivingDeathEvent(LivingDeathEvent event){
		EntityLivingBase entity = event.getEntityLiving();
		if(!entity.world.isRemote){
			if (event.getSource() instanceof SADamageSource) {
				SADamageSource tgs = (SADamageSource)event.getSource();
				if (tgs.deathType != DeathType.DEFAULT) {
					if(Math.random()<tgs.goreChance) {
						if (EntityDeathUtils.hasSpecialDeathAnim(entity, tgs.deathType)) {
							//System.out.println("Send packet!");
							SAPackets.network.sendToAllAround(new PacketEntityDeathType(entity, tgs.deathType), SAPackets.targetPointAroundEnt(entity, 100.0f));
						}
					}
				}
			}
		}
	}*/
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public static void onRenderWorldLast(RenderWorldLastEvent event) {

//		GLStateSnapshot states = new GLStateSnapshot();
		//System.out.println("***********BEFORE**********");
		//states.printDebug();
		ClientProxy.get().particleManager.renderParticles(Minecraft.getMinecraft().getRenderViewEntity(), event.getPartialTicks());
//		states.restore();
		//System.out.println("<<<<<<<<<<<AFTER>>>>>>>>>>>>");
		//new GLStateSnapshot().printDebug();
		GlStateManager.disableBlend();
		GlStateManager.blendFunc(SourceFactor.ONE, DestFactor.ZERO);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		GlStateManager.enableCull();
		GlStateManager.disableLighting();
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0, 240f);
	}
	
	
	/*@SubscribeEvent
	public static void damageTest(LivingHurtEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			System.out.println("Attacking"+event.getEntityLiving()+" for "+event.getAmount() +" with "+event.getSource());
		}
	}*/
}
