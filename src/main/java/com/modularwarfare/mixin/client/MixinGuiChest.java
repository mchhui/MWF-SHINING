package com.modularwarfare.mixin.client;

import com.modularwarfare.client.chest.ClientChestGuiSettings;
import com.modularwarfare.common.container.ContainerChestModified;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * GuiChest is {@code super(new ContainerChest(...))} — redirect NEW is unreliable under Mixin 0.8 /
 * MCP. Modify the Container argument passed to {@link net.minecraft.client.gui.inventory.GuiContainer}
 * instead so custom chest layout is not overwritten on initGui.
 */
@Mixin(GuiChest.class)
public class MixinGuiChest {

    @ModifyArg(
            method = "<init>(Lnet/minecraft/inventory/IInventory;Lnet/minecraft/inventory/IInventory;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/inventory/GuiContainer;<init>(Lnet/minecraft/inventory/Container;)V"
            ),
            index = 0,
            require = 0
    )
    private Container mwf$replaceChestContainer(final Container original) {
        if (!(original instanceof ContainerChest)) {
            return original;
        }
        final ContainerChest vanilla = (ContainerChest) original;
        final IInventory lower = vanilla.getLowerChestInventory();
        if (!ClientChestGuiSettings.isEnabled() || !ClientChestGuiSettings.shouldApply(lower)) {
            return original;
        }
        final EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return original;
        }
        final Container open = player.openContainer;
        if (open instanceof ContainerChestModified) {
            return open;
        }
        if (ContainerChestModified.isVanillaChestContainer(open)) {
            return ContainerChestModified.fromVanillaChest((ContainerChest) open, player);
        }
        final int windowId = open != null ? open.windowId : vanilla.windowId;
        return ContainerChestModified.fromChestInventory(lower, player, windowId);
    }
}
