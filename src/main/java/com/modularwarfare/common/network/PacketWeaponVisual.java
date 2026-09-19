package com.modularwarfare.common.network;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Server-to-client only. The server intentionally ignores client visual commands. */
public class PacketWeaponVisual extends PacketBase {
    public static final int PLAY = 0, STOP = 1, SHOW = 2, HIDE = 3, RESET_PARTS = 4, FRAMES = 5;
    public float fps;
    public boolean hold;
    public double loopStart = -1, loopEnd = -1;
    public int entityId, operation, duration;
    public UUID uuid;
    public String weapon, name;
    public double start, end;
    public float speed;
    public boolean reload, fire;
    public PacketWeaponVisual() {}
    public PacketWeaponVisual(int entityId, UUID uuid, String weapon, int operation, String name,
            double start, double end, float speed, boolean reload, boolean fire, int duration) {
        this.entityId = entityId; this.uuid = uuid; this.weapon = weapon; this.operation = operation;
        this.name = name; this.start = start; this.end = end; this.speed = speed;
        this.reload = reload; this.fire = fire; this.duration = duration;
    }
    @Override public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer b = new PacketBuffer(data);
        b.writeInt(entityId); b.writeUniqueId(uuid); b.writeString(weapon); b.writeByte(operation);
        b.writeString(name); b.writeDouble(start); b.writeDouble(end); b.writeFloat(speed);
        b.writeBoolean(reload); b.writeBoolean(fire); b.writeInt(duration);
        if (operation == FRAMES) { b.writeFloat(fps); b.writeBoolean(hold); b.writeDouble(loopStart); b.writeDouble(loopEnd); }
    }
    @Override public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        PacketBuffer b = new PacketBuffer(data);
        entityId = b.readInt(); uuid = b.readUniqueId(); weapon = b.readString(256); operation = b.readUnsignedByte();
        name = b.readString(128); start = b.readDouble(); end = b.readDouble(); speed = b.readFloat();
        reload = b.readBoolean(); fire = b.readBoolean(); duration = b.readInt();
        if (operation == FRAMES) { fps = b.readFloat(); hold = b.readBoolean(); loopStart = b.readDouble(); loopEnd = b.readDouble(); }
    }
    @Override public void handleServerSide(EntityPlayerMP player) {}
    @Override @SideOnly(Side.CLIENT) public void handleClientSide(EntityPlayer player) {
        if (player == null || player.world == null) return;
        net.minecraft.entity.Entity entity = player.world.getEntityByID(entityId);
        if (!(entity instanceof net.minecraft.entity.EntityLivingBase) || !uuid.equals(entity.getUniqueID())) return;
        net.minecraft.entity.EntityLivingBase holder = (net.minecraft.entity.EntityLivingBase) entity;
        net.minecraft.item.ItemStack stack = holder.getHeldItemMainhand();
        if (stack.isEmpty() || !(stack.getItem() instanceof com.modularwarfare.common.guns.ItemGun)
                || !weapon.equals(((com.modularwarfare.common.guns.ItemGun) stack.getItem()).type.internalName)) return;
        switch (operation) {
            case FRAMES: com.modularwarfare.api.ClientWeaponVisualAPI.playFrames(holder, start, end, fps, speed, hold, loopStart, loopEnd, reload, fire); break;
            case PLAY: com.modularwarfare.api.ClientWeaponVisualAPI.playAnimation(holder, name, start, end, speed, reload, fire); break;
            case STOP: com.modularwarfare.api.ClientWeaponVisualAPI.stopAnimation(holder); break;
            case SHOW: case HIDE: com.modularwarfare.api.ClientWeaponVisualAPI.setPartVisible(holder, name, operation == SHOW, duration); break;
            case RESET_PARTS: com.modularwarfare.api.ClientWeaponVisualAPI.resetParts(holder); break;
            default: break;
        }
    }
}
