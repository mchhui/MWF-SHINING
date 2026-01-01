package com.modularwarfare.common.network;

import java.util.ArrayList;

import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.ModularWarfare;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;

public class PacketAreaAttack extends PacketBase {
	public ArrayList<Integer> subjects;// entityID
	public float damage = 0;
	public String ani;
	public String hitAnimationName = ""; // 被攻击生物播放的动画名称

	public PacketAreaAttack() {
	}

	public PacketAreaAttack(float damage, ArrayList<Integer> list, String ani) {
		this.subjects = list;
		this.damage = damage;
		this.ani = ani;
	}
	
	public PacketAreaAttack(float damage, ArrayList<Integer> list, String ani, String hitAnimationName) {
		this.subjects = list;
		this.damage = damage;
		this.ani = ani;
		this.hitAnimationName = hitAnimationName != null ? hitAnimationName : "";
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		PacketBuffer buffer = new PacketBuffer(data);
		buffer.writeFloat(damage);
		buffer.writeString(ani);
		buffer.writeString(hitAnimationName);
		buffer.writeInt(subjects.size());
		for (Integer entityID : subjects) {
			buffer.writeInt(entityID);
		}
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		PacketBuffer buffer = new PacketBuffer(data);
		subjects = new ArrayList<Integer>();
		damage = buffer.readFloat();
		ani = buffer.readString(Short.MAX_VALUE);
		hitAnimationName = buffer.readString(Short.MAX_VALUE);
		int size = buffer.readInt();
		for (int i = 0; i < size; i++) {
			subjects.add(buffer.readInt());
		}
	}

	@Override
	public void handleServerSide(EntityPlayerMP playerEntity) {
		for (Integer subjectID : subjects) {
			Entity subject = playerEntity.world.getEntityByID(subjectID);
			if (subject != null) {
				if (subject.attackEntityFrom(DamageSource.causePlayerDamage(playerEntity), damage)) {
					Item item = playerEntity.getHeldItemMainhand().getItem();
					if (item instanceof ItemMelee) {
						((ItemMelee) item).type.playSound(playerEntity, WeaponSoundType.MeleeHit,
								playerEntity.getHeldItemMainhand(), null);
					}
					
					if (subject instanceof EntityLivingBase && hitAnimationName != null && !hitAnimationName.isEmpty()) {
						ModularWarfare.aniPlayer.playAni(subject.getUniqueID(), hitAnimationName, 1);
					}
				}
			}
		}
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) {

	}

}
