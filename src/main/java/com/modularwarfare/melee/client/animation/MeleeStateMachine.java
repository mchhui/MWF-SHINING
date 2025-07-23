package com.modularwarfare.melee.client.animation;

import java.util.ArrayList;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.network.PacketGunReload;
import com.modularwarfare.melee.client.RenderMelee;
import com.modularwarfare.melee.client.configs.AnimationMeleeType;
import com.modularwarfare.melee.client.configs.MeleeRenderConfig;
import com.modularwarfare.melee.common.melee.ItemMelee;
import com.modularwarfare.melee.common.melee.MeleeType;
import com.modularwarfare.melee.common.melee.MeleeType.AnimationInfo;
import com.modularwarfare.melee.comon.PacketAreaAttack;
import com.modularwarfare.melee.comon.PacketSwing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;

public class MeleeStateMachine {
	public static enum Phase {
		PRE, FIRST, SECOND, POST
	}

	public Phase attackPhase = Phase.PRE;
	public Phase lastAttackPhase = null;
	public boolean isHeavy = false;
	public boolean canDealDamage = false;
	public boolean bounced = false;

	public void reset() {
		attackPhase = Phase.POST;
		canDealDamage = false;
		this.isHeavy = false;
		this.lastAttackPhase = null;
		this.bounced = false;
	}

	public void onRenderTickUpdate(float partialTick) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		ItemStack stack = player.getHeldItemMainhand();
		Item item = stack.getItem();
		if (item instanceof ItemMelee) {
			MeleeType meleeType = ((ItemMelee) item).type;
			MeleeRenderConfig config = (MeleeRenderConfig) meleeType.enhancedModel.config;
			if (RenderMelee.controller == null || RenderMelee.controller.getConfig() != config) {
				RenderMelee.controller = new AnimationMeleeController(config, meleeType);
			}
			double modeChangeVal = RenderMelee.controller.config.meleeAnimations.get(getAttackAnimationType())
					.get(RenderMelee.controller.currentOrder).getSpeed(RenderMelee.controller.config.FPS) * partialTick;
			RenderMelee.controller.ATTACK += modeChangeVal;
			AnimationInfo ai = meleeType.getAnimationInfo(getAttackAnimationType(),
					RenderMelee.controller.currentOrder);
			if (RenderMelee.controller.ATTACK >= 1) {
				if (bounced) {
					RenderMelee.controller.ATTACK = 1;
					return;
				}
				switch (attackPhase) {
					case PRE:
						RenderMelee.controller.ATTACK = 0;
						attackPhase = Phase.FIRST;
						break;
					case FIRST:
						if (!bounced && canDealDamage) {
							canDealDamage = false;
							ArrayList<EntityLivingBase> result = RenderMelee.controller.areaCheck(true);
							if (result != null) {
								ArrayList<Integer> idList = new ArrayList();
								for (EntityLivingBase entity : result) {
									// entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 0);
									idList.add(entity.getEntityId());
								}
								ModularWarfare.NETWORK
										.sendToServer(new PacketAreaAttack(ai.damage, idList, ai.animationName));
							}
							player.swingArm(EnumHand.MAIN_HAND);
						}
						RenderMelee.controller.ATTACK = 0;
						attackPhase = Phase.SECOND;
						break;
					case SECOND:
						RenderMelee.controller.ATTACK = 0;
						attackPhase = Phase.POST;
						break;
					case POST:
						RenderMelee.controller.ATTACK = 1;
						RenderMelee.controller.nextResetDefault = true;
						break;
				}
				// System.out.println(attackPhase);
			}
			if (attackPhase != lastAttackPhase) {
				if (!isHeavy) {
					switch (attackPhase) {
						case PRE:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleePreAttack);
							break;
						case FIRST:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttack);
							break;
						case SECOND:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttackSecond);
							break;
						case POST:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleePostAttack);
							break;
					}
				} else {
					switch (attackPhase) {
						case PRE:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleePreAttackHeavy);
							break;
						case FIRST:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttackHeavy);
							break;
						case SECOND:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleeAttackHeavySecond);
							break;
						case POST:
							((ItemMelee) item).type.playClientSound(player, WeaponSoundType.MeleePostAttackHeavy);
							break;
					}
				}
			}
			lastAttackPhase = attackPhase;
		}
	}

	public AnimationMeleeType getAttackAnimationType() {
		return getAttackAnimationType(attackPhase);
	}

	public AnimationMeleeType getAttackAnimationType(Phase phase) {
		if (bounced) {
			if (!isHeavy) {
				return AnimationMeleeType.ATTACKBOUNCED;
			} else {
				return AnimationMeleeType.HEAVYATTACKBOUNCED;
			}
		}
		if (!isHeavy) {
			switch (phase) {
				case PRE:
					return AnimationMeleeType.PREATTACK;
				case FIRST:
					return AnimationMeleeType.ATTACK;
				case SECOND:
					return AnimationMeleeType.ATTACKSEC;
				case POST:
					return AnimationMeleeType.POSTATTACK;
			}
		} else {
			switch (phase) {
				case PRE:
					return AnimationMeleeType.PREHEAVYATTACK;
				case FIRST:
					return AnimationMeleeType.HEAVYATTACK;
				case SECOND:
					return AnimationMeleeType.HEAVYATTACKSEC;
				case POST:
					return AnimationMeleeType.POSTHEAVYATTACK;
			}
		}
		return null;
	}
}
