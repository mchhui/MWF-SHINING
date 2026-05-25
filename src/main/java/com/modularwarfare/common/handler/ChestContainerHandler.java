package com.modularwarfare.common.handler;

import com.modularwarfare.ModConfig;
import com.modularwarfare.common.container.ContainerChestModified;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ChestContainerHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onContainerOpen(final PlayerContainerEvent.Open event) {
        tryReplaceChestContainer(event.getEntityPlayer());
    }

    /** 部分环境下 Open 事件未触发时的兜底 */
    @SubscribeEvent
    public void onPlayerTick(final TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !ModConfig.INSTANCE.general.customChestGui) {
            return;
        }
        final EntityPlayer player = event.player;
        if (player.world.isRemote) {
            return;
        }
        tryReplaceChestContainer(player);
    }

    private static void tryReplaceChestContainer(final EntityPlayer player) {
        if (!ModConfig.INSTANCE.general.customChestGui) {
            return;
        }
        final Container container = player.openContainer;
        if (ContainerChestModified.isVanillaChestContainer(container)) {
            player.openContainer = ContainerChestModified.fromVanillaChest((ContainerChest) container, player);
        }
    }
}
