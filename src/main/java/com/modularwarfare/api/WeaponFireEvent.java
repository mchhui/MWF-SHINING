package com.modularwarfare.api;

import java.util.List;

import com.modularwarfare.common.guns.manager.FireManager.FireData;
import com.modularwarfare.common.hitbox.hits.BulletHit;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public class WeaponFireEvent extends Event {
    public EntityLivingBase shooter;
    public FireData fireData;

    public WeaponFireEvent(EntityLivingBase shooter, FireData fireData) {
        this.shooter = shooter;
        this.fireData = fireData;
    }

    @Cancelable
    public static class Pre extends WeaponFireEvent {

        public Pre(EntityLivingBase shooter, FireData fireData) {
            super(shooter, fireData);
            // TODO Auto-generated constructor stub
        }

        public void setBaseDamage(float baseDamage) {
            this.fireData.baseDamage = baseDamage;
        }

        public void setHeadshotBonus(float headshotBonus) {
            this.fireData.headshotBonus = headshotBonus;
        }

        public void setWeaponRange(double weaponRange) {
            this.fireData.weaponRange = weaponRange;
        }

        public double getWeaponRange() {
            return this.fireData.weaponRange;
        }

        public float getBaseDamage() {
            return this.fireData.baseDamage;
        }

        public float getHeadshotBonus() {
            return this.fireData.headshotBonus;
        }

    }

    public static class BulletHitPipeline extends WeaponFireEvent {

        private final List<BulletHit> hits;

        public BulletHitPipeline(EntityLivingBase shooter, FireData fireData, List<BulletHit> hits) {
            super(shooter, fireData);
            this.hits = hits;
        }

        public List<BulletHit> getHits() {
            return this.hits;
        }
    }

    public static class Post extends WeaponFireEvent {

        public Post(EntityLivingBase shooter, FireData fireData) {
            super(shooter, fireData);
            // TODO Auto-generated constructor stub
        }

    }
}
