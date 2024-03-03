package com.modularwarfare.common.backpacks;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.configs.BackpackRenderConfig;
import com.modularwarfare.client.model.ModelBackpack;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.objects.SoundEntry;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BackpackType extends BaseType {

    public int size = 16;

    public boolean allowSmallerBackpackStorage = false;
    public Integer maxWeaponStorage = null;
    public boolean isElytra=false;
    public boolean elytraStoppable=true;
    
    public boolean isJet=false;
    public boolean jetSneakHover=true;
    public boolean jetGroundDust=true;
    public float jetWorkForce=0.05f;
    //idle force is usually below zero
    public float jetIdleForce=-0.2f;
    public float jetMaxForce=0.4f;
    public float jetElytraBoost=2f;
    public int jetElytraBoostDuration=50;
    public int jetElytraBoostCoolTime=40;

    @Override
    public void loadExtraValues() {
        if (maxStackSize == null)
            maxStackSize = 1;
        loadBaseValues();
        try {
            for (ArrayList<SoundEntry> entryList : weaponSoundMap.values()) {
                for (SoundEntry soundEntry : entryList) {
                    if (soundEntry.soundName != null) {
                        ModularWarfare.PROXY.registerSound(soundEntry.soundName);
                        if (soundEntry.soundNameDistant != null)
                            ModularWarfare.PROXY.registerSound(soundEntry.soundNameDistant);
                    } else {
                        ModularWarfare.LOGGER
                            .error(String.format("Sound entry event '%s' has null soundName for type '%s'",
                                soundEntry.soundEvent, internalName));
                    }
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void reloadModel() {
        model = new ModelBackpack(ModularWarfare.getRenderConfig(this, BackpackRenderConfig.class), this);
    }

    @Override
    public String getAssetDir() {
        return "backpacks";
    }

    /***
     * Provider for the extraslots storage
     */
    public static class Provider implements ICapabilitySerializable<NBTBase> {
        final IItemHandlerModifiable items;

        public Provider(final BackpackType type) {
            this.items = new ItemStackHandler(type.size);
        }

        @Override
        public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
            return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
            return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? (T) this.items : null;
        }

        @Override
        public NBTBase serializeNBT() {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.writeNBT(this.items, null);
        }

        @Override
        public void deserializeNBT(final NBTBase nbt) {
            CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.readNBT(this.items, null, nbt);
        }
    }
}