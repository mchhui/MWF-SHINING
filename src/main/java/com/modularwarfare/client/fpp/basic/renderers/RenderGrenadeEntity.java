package com.modularwarfare.client.fpp.basic.renderers;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.ClientProxy;
import com.modularwarfare.client.ClientRenderHooks;
import com.modularwarfare.client.model.ModelGrenade;
import com.modularwarfare.common.entity.grenades.EntityGrenade;
import com.modularwarfare.common.grenades.ItemGrenade;
import com.modularwarfare.common.grenades.GrenadeType;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.client.fpp.enhanced.configs.RenderType;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;

public class RenderGrenadeEntity extends Render<EntityGrenade> {

    public static final Factory FACTORY = new Factory();

    protected RenderGrenadeEntity(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.0F;
    }

    @Nullable
    @Override
    protected ResourceLocation getEntityTexture(EntityGrenade entity) {
        return null;
    }

    @Override
    public void doRender(EntityGrenade entityIn, double x, double y, double z, float entityYaw, float partialTicks) {
        if (ModularWarfare.grenadeTypes.containsKey(entityIn.getGrenadeName())) {
            ItemGrenade itemGrenade = ModularWarfare.grenadeTypes.get(entityIn.getGrenadeName());
            
            if(itemGrenade.type.animationType == WeaponAnimationType.ENHANCED) {
                if(ModularWarfare.DEV_ENV && renderManager.isDebugBoundingBox()) {
                    GlStateManager.pushMatrix();
                    GlStateManager.disableLighting();
                    GlStateManager.disableTexture2D();
                    GlStateManager.translate(x, y, z);
                    GlStateManager.color(1.0f, 0.0f, 0.0f, 1.0f);
                    GlStateManager.glBegin(GL11.GL_LINES);
                    float size = 0.1f;
                    GL11.glVertex3f(-size, 0, 0);
                    GL11.glVertex3f(size, 0, 0);
                    GL11.glVertex3f(0, -size, 0);
                    GL11.glVertex3f(0, size, 0);
                    GL11.glVertex3f(0, 0, -size);
                    GL11.glVertex3f(0, 0, size);
                    GlStateManager.glEnd();
                    GlStateManager.enableTexture2D();
                    GlStateManager.popMatrix();
                }

                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.translate((float) x, (float) y, (float) z);
                applyRotation(entityIn, partialTicks);
                GlStateManager.translate(0.15F, 0.1F, 0.0F);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                final float worldScale = 1F / 16F;
                ClientProxy.grenadeEnhancedRenderer.renderThirdPersonGrenade(null, RenderType.GRENADE, null, new ItemStack(itemGrenade), false);
            } else {
                GlStateManager.pushMatrix();
                GlStateManager.disableLighting();
                GlStateManager.translate((float) x + 0.15F, (float) y + 0.1F, (float) z);
                applyRotation(entityIn, partialTicks);
                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
                final float worldScale = 1F / 16F;
                ModelGrenade grenade = (ModelGrenade) (itemGrenade.type.model);
                ClientProxy.grenadeStaticRenderer.bindTexture("grenades", itemGrenade.type.internalName);
                grenade.renderPart("grenadeModel", worldScale);
            }

            GlStateManager.enableLighting();
            GlStateManager.popMatrix();
        }
    }

    private void applyRotation(EntityGrenade entityIn, float partialTicks) {
        if (entityIn == null || entityIn.getDataManager() == null) {
            return;
        }

        float prevTicksExisted = entityIn.ticksExisted - 1;
        float interpolatedTicks = prevTicksExisted + partialTicks;

        if (entityIn.onGround) {
            GlStateManager.rotate(90, 0, 0, 1);
        } else if (entityIn.isStuck()) {
            if (entityIn.getStuckTicks() == 0) {
                entityIn.setStuckTicks(entityIn.ticksExisted);
                float randomBase = (entityIn.getEntityId() * 7919) % 360;
                float stuckRotX = randomBase % 360;
                float stuckRotY = (randomBase * 1.5f) % 360;
                float stuckRotZ = (randomBase * 2.5f) % 360;
                entityIn.setStuckRotation(stuckRotX, stuckRotY, stuckRotZ);
            }
            GlStateManager.rotate(entityIn.getStuckRotX(), 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(entityIn.getStuckRotY(), 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(entityIn.getStuckRotZ(), 0.0F, 0.0F, 1.0F);
        } else {
            float rotX = interpolatedTicks * 10F;
            float rotY = interpolatedTicks * 8F;
            float rotZ = interpolatedTicks * 15F;
            GlStateManager.rotate(rotX, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(rotY, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(rotZ, 0.0F, 0.0F, 1.0F);
        }
    }

    public static class Factory implements IRenderFactory {
        public Render createRenderFor(RenderManager manager) {
            return new RenderGrenadeEntity(manager);
        }
    }
}
