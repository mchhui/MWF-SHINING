package com.modularwarfare.client.patch.obfuscate;

import com.modularwarfare.client.ClientRenderHooks;
import com.mrcrayfish.obfuscate.client.event.ModelPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Loaded only via Class.forName when Obfuscate is present (see ClientProxy.startPatches).
 * Keep Obfuscate types out of always-registered classes like ClientRenderHooks.
 */
public class ObfuscateInteropImpl implements ObfuscateCompatInterop {
    public boolean fixApplied;

    public ObfuscateInteropImpl() {
        this.fixApplied = false;
    }

    @Override
    public boolean isModLoaded() {
        return true;
    }

    @Override
    public boolean isFixApplied() {
        return this.fixApplied;
    }

    @Override
    public void setFixed() {
        this.fixApplied = true;
    }

    @Override
    public void applyFix() {
        MinecraftForge.EVENT_BUS.register(this);
        setFixed();
    }

    /** Ride twist for ModelPlayer after Obfuscate SetupAngles (skips FakePlayerModel). */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSetupAnglesRideAim(ModelPlayerEvent.SetupAngles.Post event) {
        if (event.getModelPlayer().getClass().getName().contains("FakePlayerModel")) {
            return;
        }
        ClientRenderHooks.applyRideUpperBodyTwist(event.getModelPlayer(), event.getEntityPlayer());
    }
}
