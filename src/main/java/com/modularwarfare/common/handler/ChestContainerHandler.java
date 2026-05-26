package com.modularwarfare.common.handler;

import com.modularwarfare.ModConfig;
import com.modularwarfare.common.container.chest.ChestGuiFilterMatcher;
import com.modularwarfare.common.container.ContainerChestModified;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ChestContainerHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerOpen(final PlayerContainerEvent.Open event) {
        tryReplaceChestContainer(event.getEntityPlayer());
    }

    @SubscribeEvent
    public void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModConfig.INSTANCE.customChestGui.enable) {
            return;
        }
        final EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }
        tryReplaceChestContainer(player);
    }

    private static void tryReplaceChestContainer(final EntityPlayer player) {
        if (!ModConfig.INSTANCE.customChestGui.enable) {
            return;
        }
        final Container container = player.openContainer;
        if (!ContainerChestModified.isVanillaChestContainer(container)) {
            return;
        }
        final IInventory chestInventory = ((ContainerChest) container).getLowerChestInventory();
        if (!ChestGuiFilterMatcher.shouldApply(ModConfig.INSTANCE.customChestGui, chestInventory)) {
            return;
        }
        final ContainerChestModified modified =
                ContainerChestModified.fromVanillaChest((ContainerChest) container, player);
        player.openContainer = modified;
        if (player instanceof EntityPlayerMP) {
            ((EntityPlayerMP) player).sendContainerToPlayer(modified);
        }
    }
}
