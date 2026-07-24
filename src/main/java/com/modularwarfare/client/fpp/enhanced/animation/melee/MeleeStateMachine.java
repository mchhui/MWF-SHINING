package com.modularwarfare.client.fpp.enhanced.animation.melee;

import java.util.ArrayList;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.enhanced.AnimationMeleeType;
import com.modularwarfare.client.fpp.enhanced.AnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.MeleeRenderConfig;
import com.modularwarfare.client.fpp.enhanced.renderers.RenderMelee;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponSoundType;
import com.modularwarfare.common.melee.ItemMelee;
import com.modularwarfare.common.melee.MeleeType;
import com.modularwarfare.common.melee.MeleeType.AnimationInfo;
import com.modularwarfare.common.network.PacketAreaAttack;
import com.modularwarfare.common.network.PacketGunReload;
import com.modularwarfare.common.network.PacketSwing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;

public class MeleeStateMachine {
	public static enum Phase {
		PRE, FIRST, SECOND, POST
	}

	public Phase attackPhase = Phase.PRE;
	public Phase lastAttackPhase = null;
	public boolean isHeavy = false;
	public boolean canDealDamage = false;
	public boolean bounced = false;
	/** Per-phase attack sounds; earlier phases keep playing when later ones start. */
	private final ISound[] phaseSounds = new ISound[Phase.values().length];

	public void reset() {
		// Only stop current + not-yet-played phase sounds; leave already-played ones alone.
		stopCurrentAndUnplayedPhaseSounds();
		attackPhase = Phase.POST;
		canDealDamage = false;
		this.isHeavy = false;
		this.lastAttackPhase = null;
		this.bounced = false;
	}

	public void stopAllPhaseSounds() {
		for (Phase phase : Phase.values()) {
			stopPhaseSound(phase);
		}
	}

	public void stopCurrentAndUnplayedPhaseSounds() {
		if (attackPhase == null) {
			return;
		}
		Phase[] phases = Phase.values();
		for (int i = attackPhase.ordinal(); i < phases.length; i++) {
			stopPhaseSound(phases[i]);
		}
	}

	public void stopPhaseSound(Phase phase) {
		if (phase == null) {
			return;
		}
		int index = phase.ordinal();
		if (phaseSounds[index] != null) {
			Minecraft.getMinecraft().getSoundHandler().stopSound(phaseSounds[index]);
			phaseSounds[index] = null;
		}
	}

	private void playAttackPhaseSound(EntityPlayer player, MeleeType type, WeaponSoundType soundType) {
		// Do not stop earlier phase sounds — only replace this phase's tracked instance.
		int order = RenderMelee.controller != null ? RenderMelee.controller.currentOrder : 0;
		SoundEvent se = type.getSound(player, soundType, order);
		if (se != null) {
			ISound sound = PositionedSoundRecord.getRecord(se, 1, 1);
			Minecraft.getMinecraft().getSoundHandler().playSound(sound);
			phaseSounds[attackPhase.ordinal()] = sound;
		}
	}

	public void onRenderTickUpdate(float partialTick) {
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayerSP player = mc.player;
		if(player==null) {
		    return;
		}
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
										.sendToServer(new PacketAreaAttack(ai.damage, idList, ai.animationName, ai.hitAnimationName));
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
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleePreAttack);
							break;
						case FIRST:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleeAttack);
							break;
						case SECOND:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleeAttackSecond);
							break;
						case POST:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleePostAttack);
							break;
					}
				} else {
					switch (attackPhase) {
						case PRE:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleePreAttackHeavy);
							break;
						case FIRST:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleeAttackHeavy);
							break;
						case SECOND:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleeAttackHeavySecond);
							break;
						case POST:
							playAttackPhaseSound(player, meleeType, WeaponSoundType.MeleePostAttackHeavy);
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
