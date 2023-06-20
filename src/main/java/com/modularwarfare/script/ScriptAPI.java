package com.modularwarfare.script;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.*;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;

public class ScriptAPI {
    public Lang Lang = new Lang();
    public Stack Stack = new Stack();
    public Gun Gun = new Gun();
    public Ammo Ammo = new Ammo();
    public Input Input = new Input();
    public Bullet Bullet = new Bullet();

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
            if (stack.getTagCompound() == null) {
                return new NBTTagCompound();
            }
            return stack.getTagCompound().copy();
        }

        public ItemStack getStack(int itemid) {
            return new ItemStack(Item.getItemById(itemid));
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

        public boolean isBulletGun(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return false;
            }
            if (((ItemGun) itemStack.getItem()).type.acceptedBullets != null
                    && ((ItemGun) itemStack.getItem()).type.acceptedBullets.length > 0) {
                return true;
            }
            return false;
        }

        public String getGunExtraLore(ItemStack stack) {
            if (!isGun(stack)) {
                return "";
            }
            return ((ItemGun) stack.getItem()).type.extraLore;
        }

        public ArrayList<String> getInstalledAttachments(ItemStack stack) {
            ArrayList<String> list = new ArrayList<>();
            if (!isGun(stack)) {
                return list;
            }
            for (AttachmentPresetEnum attachment : AttachmentPresetEnum.values()) {
                ItemStack itemStack = GunType.getAttachment(stack, attachment);
                if (itemStack != null && itemStack.getItem() != Items.AIR) {
                    AttachmentType attachmentType = ((ItemAttachment) itemStack.getItem()).type;
                    list.add(attachmentType.displayName);
                }
            }
            return list;
        }

        public int getAmmoStorage(ItemStack itemStack) {
            if (!isBulletGun(itemStack)) {
                return 0;
            }
            return ((ItemGun) itemStack.getItem()).type.internalAmmoStorage;
        }

        public int getUsedBulletItem(ItemStack stack) {
            if (!isGun(stack)) {
                return Item.getIdFromItem(Items.AIR);
            }
            if (ItemGun.getUsedBullet(stack, ((ItemGun) stack.getItem()).type) != null) {
                return Item.getIdFromItem(ItemGun.getUsedBullet(stack, ((ItemGun) stack.getItem()).type));
            }
            return Item.getIdFromItem(Items.AIR);
        }

        public float getGunBulletSpread(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun) itemStack.getItem()).type.bulletSpread;
        }

        public float getGunDamage(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun) itemStack.getItem()).type.gunDamage;
        }

        public float getGunNumBullets(ItemStack itemStack) {
            if (!isGun(itemStack)) {
                return 0;
            }
            return ((ItemGun) itemStack.getItem()).type.numBullets;
        }

        public WeaponFireMode getFireMode(ItemStack stack) {
            return GunType.getFireMode(stack);
        }

        public HashMap<String, ArrayList<String>> getAcceptedAttachment(ItemStack stack) {
            HashMap<String, ArrayList<String>> map = new HashMap<>();
            if (!isGun(stack)) {
                return map;
            }
            ((ItemGun) stack.getItem()).type.acceptedAttachments.forEach((k, v) -> {
                if (!map.containsKey(k.typeName)) {
                    map.put(k.typeName, new ArrayList<String>());
                }
                v.forEach((name) -> {
                    map.get(k.typeName).add(ModularWarfare.attachmentTypes.get(name).type.displayName);
                });
            });
            return map;
        }

        public ArrayList<String> getAcceptedAmmoOrBullet(ItemStack stack) {
            ArrayList<String> list = new ArrayList<String>();
            if (!isGun(stack)) {
                return list;
            }
            if (((ItemGun) stack.getItem()).type.acceptedAmmo != null) {
                for (String name : ((ItemGun) stack.getItem()).type.acceptedAmmo) {
                    list.add(ModularWarfare.ammoTypes.get(name).type.displayName);
                }
            }
            if (((ItemGun) stack.getItem()).type.acceptedBullets != null) {
                for (String name : ((ItemGun) stack.getItem()).type.acceptedBullets) {
                    list.add(ModularWarfare.bulletTypes.get(name).type.displayName);
                }
            }
            return list;
        }
    }

    public static class Ammo {

        public boolean isAmmo(ItemStack stack) {
            return stack.getItem() instanceof ItemAmmo;
        }

        public int getUsedBulletItem(ItemStack stack) {
            if (!isAmmo(stack)) {
                return Item.getIdFromItem(Items.AIR);
            }
            if (stack.getTagCompound() != null) {
                if (stack.getTagCompound().hasKey("bullet")) {
                    ItemStack usedBullet = new ItemStack(stack.getTagCompound().getCompoundTag("bullet"));
                    return Item.getIdFromItem(usedBullet.getItem());
                }
            }
            return Item.getIdFromItem(Items.AIR);
        }

        public ArrayList<String> getAcceptedBullet(ItemStack stack) {
            ArrayList<String> list = new ArrayList<String>();
            if (!isAmmo(stack)) {
                return list;
            }
            if (((ItemAmmo) stack.getItem()).type.subAmmo != null) {
                for (String name : ((ItemAmmo) stack.getItem()).type.subAmmo) {
                    list.add(ModularWarfare.bulletTypes.get(name).type.displayName);
                }
            }
            return list;
        }

        public int getAmmoCapacity(ItemStack stack) {
            return ((ItemAmmo) stack.getItem()).type.ammoCapacity;
        }

        public int getMagazineCount(ItemStack stack) {
            return ((ItemAmmo) stack.getItem()).type.magazineCount;
        }

    }

    public static class Bullet {
        public boolean isBullet(ItemStack stack) {
            return stack.getItem() instanceof ItemBullet;
        }

        public float getDamageFactor(ItemStack itemStack) {
            if (itemStack.getItem() instanceof ItemBullet) {
                return ((ItemBullet) itemStack.getItem()).type.bulletDamageFactor;
            }
            return 1;
        }

        public float getAccuracyFactor(ItemStack itemStack) {
            if (itemStack.getItem() instanceof ItemBullet) {
                return ((ItemBullet) itemStack.getItem()).type.bulletAccuracyFactor;
            }
            return 1;
        }
    }

    public static class Input {
        public boolean isKeyHolding(int key) {
            return Keyboard.isKeyDown(key);
        }
    }

}
