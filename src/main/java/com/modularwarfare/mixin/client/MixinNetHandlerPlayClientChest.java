package com.modularwarfare.mixin.client;

import com.modularwarfare.client.chest.ClientChestGuiSettings;
import com.modularwarfare.common.container.ContainerChestModified;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.play.server.SPacketOpenWindow;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraft.network.play.server.SPacketWindowItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 OpenWindow / WindowItems / SetSlot 处理前把客户端箱子容器换成 54 格布局，
 * 避免小箱时服务端槽位 54+ 与客户端原版 27+ 布局错位（主背包第一行显示成快捷栏）。
 */
@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClientChest {

    @Inject(method = "handleOpenWindow", at = @At("RETURN"))
    private void mwf$replaceChestContainerOnOpen(final SPacketOpenWindow packet, final CallbackInfo ci) {
        this.tryReplaceOpenChestContainer();
    }

    @Inject(method = "handleWindowItems", at = @At("HEAD"))
    private void mwf$replaceChestContainerBeforeWindowItems(final SPacketWindowItems packet, final CallbackInfo ci) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.openContainer == null) {
            return;
        }
        if (packet.getWindowId() != mc.player.openContainer.windowId) {
            return;
        }
        this.tryReplaceOpenChestContainer();
    }

    @Inject(method = "handleSetSlot", at = @At("HEAD"))
    private void mwf$replaceChestContainerBeforeSetSlot(final SPacketSetSlot packet, final CallbackInfo ci) {
        final Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.player.openContainer == null) {
            return;
        }
        if (packet.getWindowId() != mc.player.openContainer.windowId) {
            return;
        }
        this.tryReplaceOpenChestContainer();
    }

    private void tryReplaceOpenChestContainer() {
        if (!ClientChestGuiSettings.isEnabled()) {
            return;
        }
        final EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }
        final Container container = player.openContainer;
        if (ContainerChestModified.isVanillaChestContainer(container)) {
            final IInventory chestInventory = ((ContainerChest) container).getLowerChestInventory();
            if (!ClientChestGuiSettings.shouldApply(chestInventory)) {
                return;
            }
            player.openContainer = ContainerChestModified.fromVanillaChest((ContainerChest) container, player);
        }
    }
}
