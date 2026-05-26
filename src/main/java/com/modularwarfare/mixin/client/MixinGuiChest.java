package com.modularwarfare.mixin.client;

import com.modularwarfare.client.chest.ClientChestGuiSettings;
import com.modularwarfare.common.container.ContainerChestModified;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.gui.inventory.GuiChest;

/**
 * 原版 GuiChest 构造时会 new 第二个 ContainerChest，initGui 会把它设为 openContainer，
 * 覆盖 handleOpenWindow 已替换的容器。此处改为复用/创建 ContainerChestModified。
 */
@Mixin(GuiChest.class)
public class MixinGuiChest {

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "NEW",
                    target = "Lnet/minecraft/inventory/ContainerChest;<init>(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/inventory/IInventory;Lnet/minecraft/entity/player/EntityPlayer;)V"
            )
    )
    private Container mwf$redirectChestContainer(
            final IInventory upper,
            final IInventory lower,
            final EntityPlayer player
    ) {
        if (!ClientChestGuiSettings.isEnabled() || !ClientChestGuiSettings.shouldApply(lower)) {
            return new ContainerChest(upper, lower, player);
        }
        final Container open = player.openContainer;
        if (open instanceof ContainerChestModified) {
            return open;
        }
        if (ContainerChestModified.isVanillaChestContainer(open)) {
            return ContainerChestModified.fromVanillaChest((ContainerChest) open, player);
        }
        final int windowId = open != null ? open.windowId : 0;
        return ContainerChestModified.fromChestInventory(lower, player, windowId);
    }
}
