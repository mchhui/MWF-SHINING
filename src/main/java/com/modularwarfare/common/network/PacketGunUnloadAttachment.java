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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.HashMap;

public class PacketGunUnloadAttachment extends PacketBase {

    public String attachmentType;
    public boolean unloadAll;

    public PacketGunUnloadAttachment() {
    } // Don't delete

    public PacketGunUnloadAttachment(String attachmentType, boolean unloadAll) {
        this.attachmentType = attachmentType;
        this.unloadAll = unloadAll;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        ByteBufUtils.writeUTF8String(data, this.attachmentType);
        data.writeBoolean(unloadAll);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.attachmentType = ByteBufUtils.readUTF8String(data);
        this.unloadAll = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        if (entityPlayer.getHeldItemMainhand() != null) {
            if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemStack gunStack = entityPlayer.getHeldItemMainhand();
                GunType gunType = ((ItemGun) gunStack.getItem()).type;
                InventoryPlayer inventory = entityPlayer.inventory;
                WeaponAttachmentEvent.Unload event = new WeaponAttachmentEvent.Unload(entityPlayer, gunStack, AttachmentPresetEnum.getAttachment(attachmentType), unloadAll);
                if (MinecraftForge.EVENT_BUS.post(event)) {
                    return;
                }
                

                if (gunType.transformationRequirements != null) {
                    int currentState = getCurrentGunState(gunStack, gunType);
                    HashMap<AttachmentPresetEnum, String> requirements = gunType.transformationRequirements.get(currentState);
                    if (requirements != null && !requirements.isEmpty()) {
                        AttachmentPresetEnum targetAttachmentType = AttachmentPresetEnum.getAttachment(attachmentType);
                        
                        if (unloadAll) {
                            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                                ItemStack itemStack = GunType.getAttachment(gunStack, attachment);
                                if (itemStack != null && itemStack.getItem() != Items.AIR) {

                                    if (requirements.containsKey(attachment)) {
                                        ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                                        String requiredAttachment = requirements.get(attachment);
                                        if (itemAttachment.type.internalName.equals(requiredAttachment)) {

                                            String attachmentDisplayName = itemAttachment.type.displayName != null ? 
                                                    itemAttachment.type.displayName : requiredAttachment;
                                            String attachmentTypeKey = "mwf.dictionary." + attachment.typeName;
                                            
                                            TextComponentTranslation message = new TextComponentTranslation("mwf.transform.cannot_remove_required", 
                                                    new TextComponentTranslation(attachmentTypeKey), attachmentDisplayName);
                                            message.getStyle().setColor(net.minecraft.util.text.TextFormatting.RED);
                                            
                                            entityPlayer.sendMessage(message);
                                            return;
                                        }
                                    }
                                }
                            }
                        } else {
                            if (requirements.containsKey(targetAttachmentType)) {
                                ItemStack itemStack = GunType.getAttachment(gunStack, targetAttachmentType);
                                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                                    ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                                    String requiredAttachment = requirements.get(targetAttachmentType);
                                    if (itemAttachment.type.internalName.equals(requiredAttachment)) {

                                        String attachmentDisplayName = itemAttachment.type.displayName != null ? 
                                                itemAttachment.type.displayName : requiredAttachment;
                                        String attachmentTypeKey = "mwf.dictionary." + targetAttachmentType.typeName;
                                        
                                        TextComponentTranslation message = new TextComponentTranslation("mwf.transform.cannot_remove_required", 
                                                new TextComponentTranslation(attachmentTypeKey), attachmentDisplayName);
                                        message.getStyle().setColor(net.minecraft.util.text.TextFormatting.RED);
                                        
                                        entityPlayer.sendMessage(message);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (unloadAll) {
                    for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                        ItemStack itemStack = GunType.getAttachment(gunStack, attachment);
                        if (itemStack != null && itemStack.getItem() != Items.AIR) {
                            ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                            AttachmentType attachType = itemAttachment.type;
                            GunType.removeAttachment(gunStack, attachType.attachmentType);
                            inventory.addItemStackToInventory(itemStack);
                            ModularWarfare.NETWORK.sendTo(new PacketPlaySound(entityPlayer.getPosition(), "attachment.apply", 1f, 1f), entityPlayer);
                        }
                    }
                } else {
                    ItemStack itemStack = GunType.getAttachment(gunStack, AttachmentPresetEnum.getAttachment(attachmentType));
                    if (itemStack != null && itemStack.getItem() != Items.AIR) {
                        ItemAttachment itemAttachment = (ItemAttachment) itemStack.getItem();
                        AttachmentType attachType = itemAttachment.type;
                        GunType.removeAttachment(gunStack, attachType.attachmentType);
                        inventory.addItemStackToInventory(itemStack);
                        ModularWarfare.NETWORK.sendTo(new PacketPlaySound(entityPlayer.getPosition(), "attachment.apply", 1f, 1f), entityPlayer);
                    }
                }
            }
        }
    }
    
    /**
     * 获取当前枪械的变形状态
     */
    private int getCurrentGunState(ItemStack gunStack, GunType gunType) {
        if(gunStack.hasTagCompound()) {
            if(gunStack.getTagCompound().hasKey("currentState")) {
                return gunStack.getTagCompound().getInteger("currentState");
            }
        }
        
        String currentGunName = gunType.internalName;
        for(java.util.Map.Entry<Integer, String> entry : gunType.transformations.entrySet()) {
            if(entry.getValue().equals(currentGunName)) {
                return entry.getKey();
            }
        }
        
        return 0;
    }


    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {
        // UNUSED
    }

}