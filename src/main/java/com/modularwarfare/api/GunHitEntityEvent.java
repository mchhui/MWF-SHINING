package com.modularwarfare.api;

import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class GunHitEntityEvent extends Event {
    public final Entity shooter;
    public final Entity victim;
    public final String gunId;
    public final String hitbox;
    public final double hitX;
    public final double hitY;
    public final double hitZ;
    public float damage;

    public GunHitEntityEvent(Entity shooter, Entity victim, String gunId, String hitbox, double hitX, double hitY, double hitZ, float damage) {
        this.shooter = shooter;
        this.victim = victim;
        this.gunId = gunId;
        this.hitbox = hitbox;
        this.hitX = hitX;
        this.hitY = hitY;
        this.hitZ = hitZ;
        this.damage = damage;
    }

}
