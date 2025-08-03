package com.modularwarfare.api;

import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.hitbox.hits.BulletHit;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.List;

@Event.HasResult
@Deprecated
public class WeaponFireEvent extends WeaponEvent {

    public WeaponFireEvent(EntityLivingBase entityLivingBase, ItemStack stackWeapon, ItemGun itemWeapon) {
        super(entityLivingBase, stackWeapon, itemWeapon);
    }

    /**
     * WeaponFireEvent.PreClient is fired before the weapon actually fires. Canceling this event will stop the weapon firing.<br>
     * <br>
     * This event is {@link Cancelable}.<br>
     * This event does not use {@link HasResult}.<br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     */
    @Cancelable
    public static class PreClient extends WeaponFireEvent {
        private int weaponRange;

        public PreClient(EntityLivingBase entityLivingBase, ItemStack stackWeapon, ItemGun itemWeapon, int weaponRange) {
            super(entityLivingBase, stackWeapon, itemWeapon);
            this.weaponRange = weaponRange;
        }

        public int getWeaponRange() {
            return weaponRange;
        }

        public void setWeaponRange(int updatedRange) {
            this.weaponRange = updatedRange;
        }
    }

    /**
     * WeaponFireEvent.PreServer is fired before the weapon actually fires. Canceling this event will stop the weapon firing.<br>
     * <br>
     * This event is {@link Cancelable}.<br>
     * This event does not use {@link HasResult}.<br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     */
    @Cancelable
    public static class PreServer extends WeaponFireEvent {
        private int weaponRange;

        public PreServer(EntityLivingBase entityLivingBase, ItemStack stackWeapon, ItemGun itemWeapon, int weaponRange) {
            super(entityLivingBase, stackWeapon, itemWeapon);
            this.weaponRange = weaponRange;
        }

        public int getWeaponRange() {
            return weaponRange;
        }

        public void setWeaponRange(int updatedRange) {
            this.weaponRange = updatedRange;
        }
    }

    /**
     * WeaponFireEvent.Post is fired once the weapon has fired with a list of affected objects. These lists can be modified to change the outcome.<br>
     * <br>
     * This event is not {@link Cancelable}.<br>
     * This event does not use {@link HasResult}.<br>
     * This event is fired on the {@link MinecraftForge#EVENT_BUS}.<br>
     */
    public static class Post extends WeaponFireEvent {
        private List<BulletHit> hits;
        private int fireTickDelay;
        private float damage;

        public Post(EntityLivingBase entityLivingBase, ItemStack stackWeapon, ItemGun itemWeapon, List<BulletHit> hits) {
            super(entityLivingBase, stackWeapon, itemWeapon);
            this.hits = hits;

            GunType type = itemWeapon.type;

            damage = type.gunDamage;

            fireTickDelay = type.fireTickDelay;
        }

        public List<BulletHit> getHits() {
            return hits;
        }

        public void setHits(List<BulletHit> updatedList) {
            this.hits = updatedList;
        }

        public float getDamage() {
            return damage;
        }

        public void setDamage(float updatedDamage) {
            this.damage = updatedDamage;
        }

        public float getTickDelay() {
            return fireTickDelay;
        }
    }

}
