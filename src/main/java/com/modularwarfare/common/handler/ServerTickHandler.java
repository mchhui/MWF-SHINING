package com.modularwarfare.common.handler;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.handler.data.DataGunReloadEnhancedTask;
import com.modularwarfare.common.network.PacketAimingResponse;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ModularWarfare.MOD_ID)
public final class ServerTickHandler {

    public static ConcurrentHashMap<UUID, Integer> playerReloadCooldown = new ConcurrentHashMap<UUID, Integer>();
    public static ConcurrentHashMap<UUID, ItemStack> playerReloadItemStack = new ConcurrentHashMap<UUID, ItemStack>();

    public static ConcurrentHashMap<UUID, Integer> playerAimShootCooldown = new ConcurrentHashMap<UUID, Integer>();
    public static ConcurrentHashMap<UUID, Boolean> playerAimInstant = new ConcurrentHashMap<UUID, Boolean>();
    public static ConcurrentHashMap<UUID, DataGunReloadEnhancedTask> reloadEnhancedTask = new ConcurrentHashMap<UUID, DataGunReloadEnhancedTask>();

    private static long lastBackWeaponsSync = -1;

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent event) {
        if(event.side!=Side.SERVER||event.phase!=Phase.END) {
            return;
        }
        ItemStack stack = event.player.getHeldItem(EnumHand.MAIN_HAND);
        if (stack.getItem() instanceof ItemGun) {
            if(!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            stack.getTagCompound().setInteger("maxammo", 0);
            GunType gt=((ItemGun)stack.getItem()).type;
            if(gt.acceptedBullets!=null) {
                stack.getTagCompound().setInteger("maxammo", gt.internalAmmoStorage);
            }else {
                if (stack.getTagCompound() != null) {
                    ItemStack ammoStack = new ItemStack(stack.getTagCompound().getCompoundTag("ammo"));
                    if(ammoStack.getItem() instanceof ItemAmmo) {
                        stack.getTagCompound().setInteger("maxammo", ((ItemAmmo)ammoStack.getItem()).type.ammoCapacity);    
                    }
                }
            }
        }
        
        final boolean flag = (
            playerAimShootCooldown.containsKey(event.player.getUniqueID())
            || playerAimInstant.getOrDefault(event.player.getUniqueID(), false)
        );
        ModularWarfare.NETWORK.sendToAll(new PacketAimingResponse(event.player.getUniqueID(), flag));
    }
    
    @SubscribeEvent
    static void onServerTick(ServerTickEvent event) {
        long currentTime = System.currentTimeMillis();
        if (lastBackWeaponsSync == -1 || currentTime - lastBackWeaponsSync > 1000) {
            lastBackWeaponsSync = currentTime;
            //BackWeaponsManager.INSTANCE.collect().sync();
        }

        switch (event.phase) {
        case START:
            ModularWarfare.NETWORK.handleServerPackets();
            // Player shoot aim cooldown
            for (UUID playerEntityUniqueId : playerAimShootCooldown.keySet()) {
                int value = playerAimShootCooldown.get(playerEntityUniqueId) - 1;
                if (value <= 0) {
                    playerAimShootCooldown.remove(playerEntityUniqueId);
                } else {
                    playerAimShootCooldown.replace(playerEntityUniqueId, value);
                }
            }

            // Player reload cooldown
            for (UUID uuid : playerReloadCooldown.keySet()) {
                int value = playerReloadCooldown.get(uuid) - 1;
                if (value <= 0) {
                    playerReloadCooldown.remove(uuid);
                    playerReloadItemStack.get(uuid);
                } else {
                    playerReloadCooldown.replace(uuid, value);
                }
            }
            break;
        case END:
            ModularWarfare.PLAYER_HANDLER.serverTick();
            break;
        }
    }

    private ServerTickHandler() {
    }
}
