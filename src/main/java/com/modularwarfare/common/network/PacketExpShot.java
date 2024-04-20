package com.modularwarfare.common.network;

import com.modularwarfare.ModConfig;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.WeaponExpShotEvent;
import com.modularwarfare.common.guns.*;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.network.PacketOtherPlayerAnimation.AnimationType;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mchhui.easyeffect.EasyEffect;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IThreadListener;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.ByteBufUtils;

public class PacketExpShot extends PacketBase {

    public int entityId;
    public String internalname;

    public PacketExpShot() {
    }

    public PacketExpShot(int entityId, String internalname) {
        this.entityId = entityId;
        this.internalname = internalname;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.entityId);
        ByteBufUtils.writeUTF8String(data, this.internalname);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.entityId = data.readInt();
        this.internalname = ByteBufUtils.readUTF8String(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
        IThreadListener mainThread = (WorldServer) entityPlayer.world;
        mainThread.addScheduledTask(new Runnable() {
            public void run() {
                if (entityPlayer.ping > 100 * 20) {
                    entityPlayer.sendMessage(new TextComponentString(TextFormatting.GRAY + "[" + TextFormatting.RED + "ModularWarfare" + TextFormatting.GRAY + "] Your ping is too high, shot not registered."));
                    return;
                }
                if (entityPlayer != null) {
                    if (entityPlayer.getHeldItemMainhand() != null) {
                        if (entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                            if (ModularWarfare.gunTypes.get(internalname) != null) {
                                ItemGun itemGun = ModularWarfare.gunTypes.get(internalname);
                                WeaponFireMode fireMode = itemGun.type.getFireMode(entityPlayer.getHeldItemMainhand());
                                int shotCount = fireMode == WeaponFireMode.BURST ? entityPlayer.getHeldItemMainhand().getTagCompound().getInteger("shotsremaining") > 0 ? entityPlayer.getHeldItemMainhand().getTagCompound().getInteger("shotsremaining") : itemGun.type.numBurstRounds : 1;

                                // Burst Stuff
                                if (fireMode == WeaponFireMode.BURST) {
                                    shotCount = shotCount - 1;
                                    entityPlayer.getHeldItemMainhand().getTagCompound().setInteger("shotsremaining", shotCount);
                                }

                                itemGun.consumeShot(entityPlayer.getHeldItemMainhand());
                                entityPlayer.sendSlotContents(entityPlayer.inventoryContainer,
                                        entityPlayer.inventoryContainer.inventorySlots.size() - 1 - 9 + entityPlayer.inventory.currentItem, entityPlayer.getHeldItemMainhand());

                                // Sound
                                if (GunType.getAttachment(entityPlayer.getHeldItemMainhand(), AttachmentPresetEnum.Barrel) != null) {
                                    ItemStack barrel=GunType.getAttachment(entityPlayer.getHeldItemMainhand(), AttachmentPresetEnum.Barrel);
                                    if(((ItemAttachment)barrel.getItem()).type.barrel.isSuppressor) {
                                        itemGun.type.playSound(entityPlayer, WeaponSoundType.FireSuppressed, entityPlayer.getHeldItemMainhand(), entityPlayer);  
                                    }else {
                                        itemGun.type.playSound(entityPlayer, WeaponSoundType.Fire, entityPlayer.getHeldItemMainhand(), entityPlayer);
                                    }
                                } else {
                                    itemGun.type.playSound(entityPlayer, WeaponSoundType.Fire, entityPlayer.getHeldItemMainhand(), entityPlayer);
                                }
                                
                                //Hands upwards when shooting
                                if (ServerTickHandler.playerAimShootCooldown.get(entityPlayer.getName()) == null) {
                                    ModularWarfare.NETWORK.sendToAll(new PacketAimingReponse(entityPlayer.getName(), true));
                                }
                                ServerTickHandler.playerAimShootCooldown.put(entityPlayer.getName(), 60);
                                
                                //Animation
                                MinecraftForge.EVENT_BUS.post(new WeaponExpShotEvent(entityPlayer));
                                ModularWarfare.NETWORK.sendToAll(new PacketOtherPlayerAnimation(entityPlayer.getName(), AnimationType.FIRE, internalname, itemGun.type.fireTickDelay, false));
                                Vec3d posSmoke =entityPlayer.getPositionEyes(0);
                                if(ModularWarfare.isLoadedModularMovements) {
                                    posSmoke=ServerListener.onGetPositionEyes(entityPlayer, 0, posSmoke);
                                }
                                posSmoke=posSmoke.add(entityPlayer.getLookVec().scale(0.8f));
                                Vec3d crossVec=new Vec3d(1, 0, 0).rotateYaw(-(float)Math.toRadians(entityPlayer.rotationYaw)).rotatePitch((float)Math.toRadians(entityPlayer.rotationPitch));
                                Vec3d offsetVec;
                                for(int i=0;i<5;i++) {
                                    double rand=Math.random()-0.5f;
                                    offsetVec=crossVec.scale((rand/Math.abs(rand)* 0.5f)).add(entityPlayer.getLookVec().scale(0.9f));
                                    EasyEffect.sendEffect(entityPlayer, posSmoke.x,
                                            posSmoke.y - 0.1f, posSmoke.z,
                                            offsetVec.x/(i+1), 1.2f, offsetVec.z/(i+1), 0.5f, -1f, 0.5f, 200/(i+1), (int)(10+20*Math.random()), 20, 5,
                                            (Math.random()*0.3f+0.2f), "modularwarfare:textures/particles/fire_smoke.png");    
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void handleClientSide(EntityPlayer entityPlayer) {

    }

}
