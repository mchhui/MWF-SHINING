package com.modularwarfare.client.patch.obfuscate;

import com.mrcrayfish.obfuscate.client.event.ModelPlayerEvent;

import net.minecraft.client.model.ModelPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

public class ModelPlayerEventHelper {
    public static boolean postSetupAnglesPre(EntityPlayer player, ModelPlayer modelPlayer, float partialTicks) {
        return MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.SetupAngles.Pre(player, modelPlayer, partialTicks));
    }
    public static boolean postSetupAnglesPost(EntityPlayer player, ModelPlayer modelPlayer, float partialTicks) {
        return MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.SetupAngles.Post(player, modelPlayer, partialTicks));
    }
}
