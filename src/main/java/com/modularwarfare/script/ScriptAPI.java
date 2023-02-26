package com.modularwarfare.script;

import org.lwjgl.input.Keyboard;

import com.modularwarfare.common.guns.AmmoType;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponFireMode;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;

public class ScriptAPI {
    public Lang Lang = new Lang();
    public Stack Stack = new Stack();
    public Gun Gun = new Gun();
    public Ammo Ammo = new Ammo();
    public Input Input =new Input();

    public static class Lang {
        public String format(String key, Object... parms) {
            return I18n.format(key, parms);
        }
    }
    
    public static class Stack {
        public boolean hasNbt(ItemStack stack) {
            return stack.hasTagCompound();
        }

        public NBTTagCompound getNbt(ItemStack stack) {
            return stack.getTagCompound();
        }
        
        public ItemStack getStack(Item item) {
            return new ItemStack(item);
        }
        
        public String getDisplayName(ItemStack stack) {
            return stack.getDisplayName();
        }
        
        public boolean isEmpty(ItemStack stack) {
            return stack.isEmpty();
        }
    }

    public static class Gun {
        public boolean isGun(ItemStack stack) {
            return stack.getItem() instanceof ItemGun;
        }

        public boolean hasAmmoLoaded(ItemStack stack) {
            return ItemGun.hasAmmoLoaded(stack);
        }

        public ItemStack getAmmoStack(ItemStack gunStack) {
            if (hasAmmoLoaded(gunStack)) {
                ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
                return ammoStack;
            }
            return ItemStack.EMPTY;
        }
        
        public ItemBullet getUsedBulletItem(ItemStack stack) {
            if(ItemAmmo.getUsedBullet(stack)!=null) {
                return ItemAmmo.getUsedBullet(stack);
            }
            return null;
        }

        public WeaponFireMode getFireMode(ItemStack stack) {
            return GunType.getFireMode(stack);
        }

        public GunType getGunType(ItemStack stack) {
            return ((ItemGun) stack.getItem()).type;
        }
    }

    public static class Ammo {
        public boolean isAmmo(ItemStack stack) {
            return stack.getItem() instanceof ItemAmmo;
        }

        public AmmoType getAmmoType(ItemStack stack) {
            return ((ItemAmmo) stack.getItem()).type;
        }
    }

    public static class Input{
        public boolean isKeyHolding(int key) {
            return Keyboard.isKeyDown(key);
        }
    }
    
}
