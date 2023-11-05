 package com.modularwarfare.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WeaponExpShotEvent extends Event{
     public EntityPlayer player;
     public WeaponExpShotEvent(EntityPlayer player) {
         this.player=player;
     }
}
