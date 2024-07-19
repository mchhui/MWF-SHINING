 package com.modularwarfare.common.playerstate;

import java.util.UUID;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.util.math.MathHelper;

public class PlayerState {
     //server
     public float ammoGunDamageAmplifier=1;
     public float bulletGunDamageAmplifier=1;
     public float speedAmplifier=1;
     public float bulletproofFactor=1;
     
     //client-sync
     public float recoilPitchFactor=1;
     public float recoilYawFactor=1;
     public float accuracyFactor=1;
     public float roundsPerMinFactor=1;
     
     //client-unsync
     public float devetionRoundsPerMinFactor=1;
     
     protected boolean dirty;
     protected UUID speedModifier=MathHelper.getRandomUUID(ThreadLocalRandom.current());
     
     public void makeDirty() {
         dirty=true;
     }
}
