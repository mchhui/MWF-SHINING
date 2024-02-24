package com.modularwarfare.mixin;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.core.net.minecraft.entity.player.EntityLivingBase;
import com.modularwarfare.core.net.optifine.shaders.ShadersRender;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.Name("modularwarfare")
public class MixinCore implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        ArrayList<String> arrayList=new ArrayList<String>();
        arrayList.add(ShadersRender.class.getName());
        arrayList.add(EntityLivingBase.class.getName());
        try {
            if(Class.forName("mchhui.modularmovements.coremod.ModularMovementsPlugin") != null) {
                arrayList.add("mchhui.modularmovements.coremod.minecraft.EntityPlayerSP" );
                arrayList.add("mchhui.modularmovements.coremod.minecraft.Entity");
            }
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return arrayList.toArray(new String[arrayList.size()]);
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins." + ModularWarfare.MOD_ID + ".json");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
