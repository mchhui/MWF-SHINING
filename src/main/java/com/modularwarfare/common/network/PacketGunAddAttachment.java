package com.modularwarfare.common.network;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.WeaponAttachmentEvent;
import com.modularwarfare.common.guns.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;

public class PacketGunAddAttachment extends PacketBase {

    public int slot;

    public PacketGunAddAttachment() {
    } // Don't delete

    public PacketGunAddAttachment(int slot) {
        this.slot = slot;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.slot);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.slot = data.readInt();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        if (entityPlayer.getHeldItemMainhand() != null) {
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemStack gunStack = entityPlayer.getHeldItemMainhand();
                ItemGun itemGun = (ItemGun) entityPlayer.getHeldItemMainhand().getItem();
                GunType gunType = itemGun.type;
                InventoryPlayer inventory = entityPlayer.inventory;
                ItemStack attachStack;
                if (slot == -999) {
                    attachStack = inventory.getItemStack();
                } else {
                    attachStack = inventory.getStackInSlot(slot);
                }
                WeaponAttachmentEvent.Load event = new WeaponAttachmentEvent.Load(entityPlayer, gunStack, attachStack);
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    return;
                }
                attachStack = event.attach;
                if (attachStack != null && !attachStack.isEmpty()) {
                    if (attachStack.getItem() instanceof ItemAttachment) {
                        if (!gunType.canAcceptAttachment(attachStack)) {
                            return;
                        }
                        ItemAttachment itemAttachment = (ItemAttachment) attachStack.getItem();
                        AttachmentType attachType = itemAttachment.type;
                        ItemStack existAttachment = GunType.getAttachment(gunStack, attachType.attachmentType);
                        if (existAttachment != null && existAttachment.getItem() != Items.AIR) {
                            ItemAttachment localItemAttachment = (ItemAttachment) existAttachment.getItem();
                            AttachmentType localAttachType = localItemAttachment.type;
                            GunType.removeAttachment(gunStack, localAttachType.attachmentType);
                            inventory.addItemStackToInventory(existAttachment);
                        }
                        ItemStack attachmentStack = new ItemStack(itemAttachment);
                        NBTTagCompound tag = new NBTTagCompound();
                        tag.setInteger("skinId", 0);
                        attachmentStack.setTagCompound(tag);
                        GunType.addAttachment(gunStack, attachType.attachmentType, attachmentStack);
                        if (slot == -999) {
                            inventory.getItemStack().shrink(1);
                        } else {
                            inventory.getStackInSlot(this.slot).shrink(1);
                        }
                        ModularWarfare.NETWORK.sendTo(new PacketPlaySound(entityPlayer.getPosition(), "attachment.apply", 1f, 1f), entityPlayer);
                    }
                    if (slot != -999 && (attachStack.getItem() instanceof ItemSpray)) {
                        ItemSpray spray = (ItemSpray) attachStack.getItem();
                        if (gunStack.getTagCompound() != null) {
                            for (int i = 0; i < gunType.modelSkins.length; i++) {
                                if (gunType.modelSkins[i].internalName.equalsIgnoreCase(spray.type.skinName)) {
                                    NBTTagCompound nbtTagCompound = gunStack.getTagCompound();
                                    nbtTagCompound.setInteger("skinId", i);
                                    gunStack.setTagCompound(nbtTagCompound);
                                    inventory.getStackInSlot(slot).damageItem(1, entityPlayer);
                                    if (inventory.getStackInSlot(slot).getMaxDamage() != 0 && inventory.getStackInSlot(slot).getItemDamage() == inventory.getStackInSlot(slot).getMaxDamage()) {
                                        inventory.removeStackFromSlot(slot);
                                    }
                                    ModularWarfare.NETWORK.sendTo(new PacketPlaySound(entityPlayer.getPosition(), "spray", 1f, 1f), entityPlayer);
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        // UNUSED
    }

}