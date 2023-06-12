package com.modularwarfare.script;

import net.minecraft.nbt.*;

public class NBTSearcher {
    public NBTBase search(NBTTagCompound tag, String key, int type) {
        String[] keys = key.split("\\.");
        if (keys.length == 0) {
            return null;
        }
        if (keys.length == 1) {
            NBTBase base = tag.getTag(keys[0]);
            if (base == null || base.getId() != type) {
                return null;
            }
            return base;
        }
        NBTTagCompound nbt;
        nbt = tag.getCompoundTag(keys[0]);
        for (int i = 1; i < keys.length - 1; i++) {
            nbt = nbt.getCompoundTag(keys[i]);
        }
        NBTBase base = nbt.getTag(keys[keys.length - 1]);
        if (base == null) {
            return null;
        }
        if (base.getId() == type) {
            return base;
        }
        return null;
    }

    public int searchInt(NBTTagCompound tag, String key) {
        NBTBase base = search(tag, key, 3);
        if (base instanceof NBTTagInt) {
            return ((NBTTagInt) base).getInt();
        }
        return 0;
    }


    public NBTTagCompound readJson(String json) throws NBTException {
        return JsonToNBT.getTagFromJson(json);
    }
}
