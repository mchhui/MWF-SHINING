package com.modularwarfare.client.compat;

import cloud.siz.atomic.api.render.AtomicExternalDraw;
import cloud.siz.atomic.api.render.ExternalDrawContext;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;
import java.util.List;

/**
 * Subscribe Atomic ExternalDraw layers for MWF gun mesh.
 * <p>
 * Shadow mid-fill (HE WO contract: world − origin):
 * <ul>
 *   <li>Entity-sun map ({@code cull ≤ 8}): only that living owner's held gun — never ITEMFRAME.</li>
 *   <li>Local FP: ignore stale TP world matrix; rebuild from posed biped after fillEntityMesh.</li>
 *   <li>CSM / custom light: world matrix (incl. frames) or skip if missing.</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
public final class MwfAtomicDrawBridge {

    /** Entity shadow halfExtent is typically ~2–4; CSM / custom cull is much larger. */
    private static final float ENTITY_SUN_MAP_CULL = 8f;

    private static boolean busRegistered;
    private static boolean gbufferReady;
    private static final MwfAtomicDrawBridge INSTANCE = new MwfAtomicDrawBridge();
    private static final FloatBuffer WORLD_MAT_UPLOAD = BufferUtils.createFloatBuffer(16);

    private MwfAtomicDrawBridge() {
    }

    public static void init() {
        if (!AtomicShaderCompat.isAtomicLoaded()) {
            return;
        }
        if (!busRegistered) {
            busRegistered = true;
            MinecraftForge.EVENT_BUS.register(INSTANCE);
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
        return AtomicShaderCompat.isAtomicLoaded();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        MwfAtomicGunPoseCache.onRenderTickStart();
        // Do NOT clear the local player's world matrix in FP: entity self-shadow is the TP
        // body silhouette (Atomic force-TP fill). Clearing forced a broken rebuild and
        // made FP gun shadows look 180°-flipped vs TP.
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
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

    @SubscribeEvent
    public void onShadowSunExternal(AtomicExternalDraw.AtomicExternalDrawEvent.ShadowSunExternal event) {
        submitShadowCasters(event.getContext());
    }

    @SubscribeEvent
    public void onShadowCustomExternal(AtomicExternalDraw.AtomicExternalDrawEvent.ShadowCustomExternal event) {
        submitShadowCasters(event.getContext());
    }

    private static void submitShadowCasters(ExternalDrawContext ctx) {
        tryEnableGBuffer();
        if (ctx == null || !ctx.isShadowPass() || !AtomicShaderCompat.isShadowDepthActive()) {
            return;
        }
        float ox = ctx.getOriginX();
        float oy = ctx.getOriginY();
        float oz = ctx.getOriginZ();
        float cull = ctx.getCullRadius() > 0f ? ctx.getCullRadius() : 64f;
        List<MwfAtomicGunPoseCache.Entry> casters = MwfAtomicGunPoseCache.entriesInCull(ox, oy, oz, cull);
        if (casters.isEmpty()) {
            return;
        }

        final boolean entitySunMap = cull <= ENTITY_SUN_MAP_CULL;
        final int mapOwnerId = entitySunMap ? resolveMapOwnerEntityId(ox, oy, oz, cull) : -1;

        Runnable rebind = ctx::rebindDepth;
        float partialTicks = ctx.getPartialTicks();
        AtomicShaderCompat.beginShadowDepthExternal(rebind, ctx.getLightViewProj(), ox, oy, oz);
        try {
            Minecraft mc = Minecraft.getMinecraft();
            for (MwfAtomicGunPoseCache.Entry e : casters) {
                if (entitySunMap) {
                    // Player self-shadow must not suck in nearby item-frame / loot guns.
                    if (e.isWorldProp()) {
                        continue;
                    }
                    if (mapOwnerId >= 0 && e.entityId != mapOwnerId) {
                        continue;
                    }
                }

                AtomicShaderCompat.beginShadowDepthDrawBatch();
                GlStateManager.pushMatrix();
                try {
                    // Prefer world matrix (TP color capture) for FP and TP so self-shadow matches.
                    // Rebuild only when matrix is missing; prepareScale keeps orientation correct.
                    if (e.worldMatrix != null) {
                        WORLD_MAT_UPLOAD.clear();
                        WORLD_MAT_UPLOAD.put(e.worldMatrix, 0, 16);
                        WORLD_MAT_UPLOAD.flip();
                        GL11.glMultMatrix(WORLD_MAT_UPLOAD);

                        EntityLivingBase holder = resolveLiving(mc, e.entityId);
                        ClientProxy.gunEnhancedRenderer.drawThirdGunForAtomicShadow(
                                e.stack, e.renderType, holder, e.sneak);
                    } else if (e.entityId >= 0 && tryDrawViaEntityRebuild(e, partialTicks)) {
                        // ok
                    }
                } catch (Throwable ignored) {
                } finally {
                    GlStateManager.popMatrix();
                    AtomicShaderCompat.beginShadowDepthDrawBatch();
                }
            }
            if (rebind != null) {
                rebind.run();
            }
        } finally {
            AtomicShaderCompat.endShadowDepthExternal();
        }
    }

    private static int resolveMapOwnerEntityId(float ox, float oy, float oz, float cull) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) {
            return -1;
        }
        EntityLivingBase best = null;
        double bestD = (double) cull * (double) cull;
        for (Entity ent : mc.world.loadedEntityList) {
            if (!(ent instanceof EntityLivingBase)) {
                continue;
            }
            double dx = ent.posX - ox;
            double dy = ent.posY + ent.getEyeHeight() * 0.5 - oy;
            double dz = ent.posZ - oz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 <= bestD) {
                bestD = d2;
                best = (EntityLivingBase) ent;
            }
        }
        return best != null ? best.getEntityId() : -1;
    }

    private static EntityLivingBase resolveLiving(Minecraft mc, int entityId) {
        if (entityId < 0 || mc.world == null) {
            return null;
        }
        Entity ent = mc.world.getEntityByID(entityId);
        return ent instanceof EntityLivingBase ? (EntityLivingBase) ent : null;
    }

    /**
     * After {@code fillEntityMesh → renderEntity}, biped arm angles are still set.
     * Rebuild entity→arm→gun under {@code T(-origin)} (batch already applied).
     */
    private static boolean tryDrawViaEntityRebuild(MwfAtomicGunPoseCache.Entry e, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase living = resolveLiving(mc, e.entityId);
        if (living == null) {
            return false;
        }
        RenderType renderType = null;
        for (RenderType t : RenderType.values()) {
            if (e.renderType.equals(t.serializedName)) {
                renderType = t;
                break;
            }
        }
        if (renderType == null) {
            return false;
        }
        RenderManager rm = mc.getRenderManager();
        Render<?> render = rm.getEntityRenderObject(living);
        if (!(render instanceof RenderLivingBase)) {
            return false;
        }
        RenderLivingBase<?> livingRender = (RenderLivingBase<?>) render;

        double ix = living.lastTickPosX + (living.posX - living.lastTickPosX) * (double) partialTicks;
        double iy = living.lastTickPosY + (living.posY - living.lastTickPosY) * (double) partialTicks;
        double iz = living.lastTickPosZ + (living.posZ - living.lastTickPosZ) * (double) partialTicks;
        float bodyYaw = living.prevRenderYawOffset
                + (living.renderYawOffset - living.prevRenderYawOffset) * partialTicks;

        GlStateManager.translate(ix, iy, iz);
        GlStateManager.rotate(180.0F - bodyYaw, 0.0F, 1.0F, 0.0F);
        // Match RenderLivingBase.prepareScale — without this the gun silhouette is mirrored/flipped.
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
        // Match RenderLayerHeldGun sneak offset (after prepareScale).
        if (living.isSneaking()) {
            GlStateManager.translate(0.0F, 0.2F, 0.0F);
        }
        ClientProxy.gunEnhancedRenderer.drawThirdGun(
                livingRender, renderType, living, e.stack, false, false);
        return true;
    }
}