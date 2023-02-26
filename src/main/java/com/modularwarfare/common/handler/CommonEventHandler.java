package com.modularwarfare.common.handler;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.entity.item.EntityItemLoot;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.network.PacketClientKillFeedEntry;
import com.modularwarfare.common.network.PacketExplosion;
import com.modularwarfare.common.network.PacketVerification;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.world.ModularWarfareWorldListener;
import net.minecraft.block.BlockContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class CommonEventHandler {

    public static HashMap<String, Long> playerTimeoutMap=new HashMap<>();
    
    private static int getRandomNumberInRange(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }
    
    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent event) {
        if(ModConfig.INSTANCE.general.modified_pack_server_kick||ModConfig.INSTANCE.general.directory_pack_server_kick) {
            playerTimeoutMap.put(event.player.getName(), System.currentTimeMillis());  
        }
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent event) {
        if (event.phase != Phase.END) {
            return;
        }
        long time=System.currentTimeMillis();
        ArrayList<String> list = new ArrayList<String>();
        playerTimeoutMap.forEach((name, t) -> {
            EntityPlayerMP player = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList()
                    .getPlayerByUsername(name);
            if(player==null) {
                list.add(name);
            }else {
                if(time>t+5000) {
                    ModularWarfare.NETWORK.sendTo(new PacketVerification(), player);  
                }
                if(time>t+10000) {
                    player.connection.disconnect(new TextComponentString("[ModularWarfare] Verification timeout."));
                    list.add(name);
                }
            }
        });
        list.forEach((name)->{
            playerTimeoutMap.remove(name);
        });
    }

    @SubscribeEvent
    public void onLivingDeath(final LivingDeathEvent event) {
        if (ModConfig.INSTANCE.killFeed.enableKillFeed) {
            final Entity entity = event.getEntity();
            if (entity instanceof EntityPlayer) {
                if (!entity.world.isRemote) {
                    if (event.getSource().isProjectile()) {
                        if (event.getSource().getTrueSource() instanceof EntityPlayer) {
                            ItemStack heldStack = ((EntityPlayer) event.getSource().getTrueSource()).getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
                            if (heldStack != null) {
                                if (heldStack.getItem() instanceof ItemGun) {
                                    final String text = getRandomMessage((event.getSource().getTrueSource()).getDisplayName().getFormattedText(), (event.getEntity()).getDisplayName().getFormattedText());
                                    ModularWarfare.NETWORK.sendToAll(new PacketClientKillFeedEntry(text, ModConfig.INSTANCE.killFeed.messageDuration, ((ItemGun) heldStack.getItem()).type.internalName));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String getRandomMessage(String killer, String victim) {
        if (ModConfig.INSTANCE.killFeed.messageList != null && ModConfig.INSTANCE.killFeed.messageList.size() > 0) {
            int r = getRandomNumberInRange(0, ModConfig.INSTANCE.killFeed.messageList.size() - 1);
            String choosen = ModConfig.INSTANCE.killFeed.messageList.get(r);
            choosen = choosen.replace("{killer}", killer).replace("{victim}", victim);
            choosen = choosen.replace("&", "§");
            return choosen;
        }
        return "";
    }

    @SubscribeEvent
    public void onLivingAttack(final LivingAttackEvent event) {
        if (!event.getEntityLiving().world.isRemote)
            return;
        final Entity entity = event.getEntity();
        if (entity.getEntityWorld().isRemote) {
            ModularWarfare.PROXY.addBlood(event.getEntityLiving(), 10, true);
        }
    }

    @SubscribeEvent
    public void onLivingHurt(final LivingHurtEvent event) {
        final Entity entity = event.getEntity();
        if (entity instanceof EntityItemLoot) {
            return;
        }
    }

    private static final ModularWarfareWorldListener WORLD_LISTENER = new ModularWarfareWorldListener();

    @SubscribeEvent
    public void onInitWorld(WorldEvent.Load event) {
        World world = event.getWorld();
        world.addEventListener(WORLD_LISTENER);
    }

    @SubscribeEvent
    public void onUnloadWorld(WorldEvent.Unload event) {
        World world = event.getWorld();
        world.removeEventListener(WORLD_LISTENER);
    }

    @SubscribeEvent
    public void onEntityInteractBlock(final PlayerInteractEvent.RightClickBlock event) {
        if (ModConfig.INSTANCE.guns.guns_interaction_hand) {
            if (event.getWorld().isRemote) {
                if (Minecraft.getMinecraft().player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) != null) {
                    if (Minecraft.getMinecraft().player.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).getItem() instanceof ItemGun) {
                        if (!(event.getWorld().getBlockState(event.getPos()).getBlock() instanceof BlockContainer)) {
                            event.setUseBlock(Event.Result.DENY);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void explosionEvent(ExplosionEvent e) {
        Vec3d pos = e.getExplosion().getPosition();
        ModularWarfare.NETWORK.sendToAll(new PacketExplosion(pos.x, pos.y, pos.z));
    }


    @SubscribeEvent
    public void onEntityInteract(final PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof EntityItemLoot) {
            if (!event.getWorld().isRemote && event.getTarget().onGround && !event.getEntityPlayer().isSpectator()) {
                final EntityItemLoot loot = (EntityItemLoot) event.getTarget();
                if (loot.getCustomAge() > 20) {
                    final ItemStack stack = loot.getItem();
                    if (stack.getItem() != Items.AIR && event.getTarget().onGround) {
                        loot.pickup(event.getEntityPlayer());
                    }
                }
            }
            event.setCanceled(true);
            event.setCancellationResult(EnumActionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(final EntityJoinWorldEvent event) {
        if (event.getWorld().isRemote) {
            return;
        }
        if (ModConfig.INSTANCE.drops.advanced_drops_models) {
            if (event.getEntity().getClass() == EntityItem.class) {
                final EntityItem item = (EntityItem) event.getEntity();
                if (!item.getItem().isEmpty()) {
                    if (item.getItem().getItem() instanceof BaseItem || ModConfig.INSTANCE.drops.advanced_drops_models_everything) {
                        final EntityItemLoot loot = new EntityItemLoot((EntityItem) event.getEntity());
                        event.getEntity().setDead();
                        loot.setInfinitePickupDelay();
                        event.setResult(Event.Result.DENY);
                        event.setCanceled(true);
                        event.getWorld().spawnEntity(loot);
                        return;
                    }
                }
            }
        }
    }

}
