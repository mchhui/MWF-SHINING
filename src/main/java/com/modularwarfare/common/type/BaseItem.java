package com.modularwarfare.common.type;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.script.ScriptHost;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class BaseItem extends Item {

    public BaseType baseType;
    public boolean render3d = true;
    public ResourceLocation tooltipScript;

    public BaseItem(BaseType type) {
        setTranslationKey(type.internalName);
        setRegistryName(type.internalName);
        setCreativeTab(ModularWarfare.MODS_TABS.get(type.contentPack));

        this.baseType = type;
        if(type.maxStackSize != null) {
            this.setMaxStackSize(type.maxStackSize);
        } else {
            this.setMaxStackSize(1);
        }
        this.canRepair = false;
        tooltipScript=new ResourceLocation(ModularWarfare.MOD_ID,"script/"+baseType.toolipScript+".js");
    }

    public void setType(BaseType type) {

    }

    public String generateLoreLine(String prefix, String value) {
        String baseDisplayLine = "%b%s: %g%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%g", TextFormatting.GRAY.toString());
        return String.format(baseDisplayLine, prefix, value);
    }

    public String generateLoreHeader(String prefix) {
        String baseDisplayLine = "%b%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        return String.format(baseDisplayLine, prefix);
    }

    public String generateLoreListEntry(String prefix, String value) {
        String baseDisplayLine = " - %s %g%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%g", TextFormatting.GRAY.toString());
        return String.format(baseDisplayLine, value, prefix);
    }

    public String generateLoreLineAlt(String prefix, String current, String max) {
        String baseDisplayLine = "%b%s: %g%s%dg/%g%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%g", TextFormatting.GRAY.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%dg", TextFormatting.DARK_GRAY.toString());
        return String.format(baseDisplayLine, prefix, current, max);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if(tooltipScript!=null) {
            ScriptHost.INSTANCE.callScript(tooltipScript, stack, tooltip,"updateTooltip");  
        }
    }

}
