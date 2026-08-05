package com.modularwarfare.client.compat;

import cloud.siz.atomic.api.render.AtomicExternalDraw;
import cloud.siz.atomic.api.render.ExternalDrawContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Atomic G-buffer wiring for MWF.
 * <p>
 * Gun sky-light / self-shadow External casters were removed — reimplement from scratch later.
 * This bridge only enables fill detection and rebinds on {@code GBufferEntities}.
 */
@SideOnly(Side.CLIENT)
public final class MwfAtomicDrawBridge {

    private static boolean busRegistered;
    private static boolean gbufferReady;
    private static final MwfAtomicDrawBridge INSTANCE = new MwfAtomicDrawBridge();

    private MwfAtomicDrawBridge() {
    }

    public static void init() {
        if (!AtomicShaderCompat.isAtomicLoaded()) {
            return;
        }
        if (!busRegistered) {
            busRegistered = true;
            MinecraftForge.EVENT_BUS.register(INSTANCE);
            MinecraftForge.EVENT_BUS.register(ArmorTranslucentOverlay.INSTANCE);
        }
        tryEnableGBuffer();
    }

    private static void tryEnableGBuffer() {
        if (gbufferReady) {
            return;
        }
        if (!AtomicShaderCompat.isAvailable()) {
            return;
        }
        gbufferReady = true;
    }

    public static boolean isEnabled() {
        return gbufferReady;
    }

    public static boolean shouldSkipLegacyForwardDraw() {
        return AtomicShaderCompat.isPipelineEnabled();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            ArmorTranslucentOverlay.beginFrame();
            return;
        }
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tryEnableGBuffer();
    }

    @SubscribeEvent
    public void onGBufferEntities(AtomicExternalDraw.AtomicExternalDrawEvent.GBufferEntities event) {
        tryEnableGBuffer();
        if (!AtomicShaderCompat.isGBufferFillActive()) {
            return;
        }
        ExternalDrawContext ctx = event.getContext();
        if (ctx != null) {
            ctx.rebindFill();
        }
    }
}
