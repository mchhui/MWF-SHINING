package com.modularwarfare.common.armor;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.MWArmorModel;
import com.modularwarfare.api.MWArmorType;
import com.modularwarfare.client.config.ArmorRenderConfig;
import com.modularwarfare.client.model.ModelCustomArmor;
import com.modularwarfare.common.type.BaseType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.HashMap;

public class ArmorType extends BaseType {
    public Integer durability;
    public double defense;
    public HashMap<MWArmorType, ArmorInfo> armorTypes;

    public ArmorType() {
        this.armorTypes = new HashMap<MWArmorType, ArmorInfo>();
    }

    public void initializeArmor(final String slot) {
        for (final MWArmorType armorType : this.armorTypes.keySet()) {
            if (armorType.name().toLowerCase().equalsIgnoreCase(slot)) {
                this.armorTypes.get(armorType).internalName = this.internalName + ((this.armorTypes.size() > 1) ? ("_" + slot) : "");
            }
            if (this.armorTypes.get(armorType).armorModels != null) {
                for (MWArmorModel model : MWArmorModel.values()) {
                    if (this.armorTypes.get(armorType).armorModels.contains(model)) {
                        this.armorTypes.get(armorType).showArmorModels.put(model, true);
                    } else {
                        this.armorTypes.get(armorType).showArmorModels.put(model, false);
                    }
                }
            }
        }
    }

    @Override
    public void loadExtraValues() {
        if (this.maxStackSize == null) {
            this.maxStackSize = 1;
        }
        this.loadBaseValues();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void reloadModel() {
        this.bipedModel = new ModelCustomArmor(ModularWarfare.getRenderConfig(this, ArmorRenderConfig.class), this);
    }

    @Override
    public String getAssetDir() {
        return "armor";
    }

    public static class ArmorInfo {
        public String displayName;
        public ArrayList<MWArmorModel> armorModels;
        public boolean hidePlayerModel=false;

        public transient HashMap<MWArmorModel, Boolean> showArmorModels = new HashMap<>();
        public transient String internalName;
    }
}
