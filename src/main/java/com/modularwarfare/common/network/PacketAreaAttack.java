package com.modularwarfare.common.network;

import java.util.ArrayList;

import com.modularwarfare.client.fpp.enhanced.animation.melee.MeleeStateMachine.Phase;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.melee.ItemMelee;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mchhui.he.api.ELMAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;

public class PacketAreaAttack extends PacketBase {
	public ArrayList<Integer> subjects;// entityID
	public float damage = 0;
	public String ani;

	public PacketAreaAttack() {
	}

	public PacketAreaAttack(float damage, ArrayList<Integer> list, String ani) {
		this.subjects = list;
		this.damage = damage;
		this.ani = ani;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		PacketBuffer buffer = new PacketBuffer(data);
		buffer.writeFloat(damage);
		buffer.writeString(ani);
		buffer.writeInt(subjects.size());
		for (Integer entityID : subjects) {
			buffer.writeInt(entityID);
		}
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
		PacketBuffer buffer = new PacketBuffer(data);
		subjects = new ArrayList();
		damage = buffer.readFloat();
		ani = buffer.readString(Short.MAX_VALUE);
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
				}
			}
		}
		// ELMAPI.playAni(playerEntity.getUniqueID(), ani, 1);
	}

	@Override
	public void handleClientSide(EntityPlayer clientPlayer) {

	}

}
