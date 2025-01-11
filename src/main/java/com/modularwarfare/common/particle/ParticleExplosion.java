package com.modularwarfare.common.particle;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.common.guns.BulletType;
import com.modularwarfare.common.guns.ItemBullet;
import com.modularwarfare.utility.RenderHelperMW;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

public class ParticleExplosion extends Particle {

    public float lastSwing = 0;
    private float alpha_rubble = 0;
    private String customModelPath;
    private String customTexturePath;
    private float lastParticleAge = 0;
    private float fadeInProgress = 0.0f;
    private static final float FADE_IN_SPEED = 0.15f;
    private static final float SCALE_START = 0.01f;
    private static final float SCALE_SPEED = 0.025f; 
    private static final float ELASTIC_BASE_STRENGTH = 8f; // 弹性强度
    private static final float ELASTIC_BASE_DAMPING = 0.75f; // 弹性基础阻尼
    private static final float FADE_OUT_SPEED = 2.5f;
    private static final float EXPAND_SPEED = 4.5f;
    private static final int DEFAULT_PARTICLE_MAX_AGE = 110; // 默认粒子生命周期
    private float currentScale = SCALE_START;

    public ParticleExplosion(World par1World, double par2, double par4, double par6) {
        this(par1World, par2, par4, par6, null, null);
    }

    public ParticleExplosion(World par1World, double par2, double par4, double par6, String modelPath, String texturePath) {
        this(par1World, par2, par4, par6, modelPath, texturePath, null);
    }

    public ParticleExplosion(World par1World, double par2, double par4, double par6, String modelPath, String texturePath, BulletType bulletType) {
        super(par1World, par2, par4, par6, 0.0D, 0.0D, 0.0D);
        Random rand = new Random();
        this.motionX *= 0.800000011920929D;
        this.motionY = 0;
        this.motionZ *= 0.800000011920929D;
        this.particleRed = this.particleGreen = this.particleBlue = 1.0F;
        this.particleScale *= this.rand.nextFloat() * 2.0F + 0.2F;
        
        // 从bulletType中读取particleMaxAge，如果没有则使用默认值
        this.particleMaxAge = bulletType != null && bulletType.particleMaxAge > 0 ? 
                             bulletType.particleMaxAge : DEFAULT_PARTICLE_MAX_AGE;
                             
        this.customModelPath = modelPath;
        this.customTexturePath = texturePath;
        this.fadeInProgress = 0.0f;
        this.currentScale = SCALE_START;
    }

    @Override
    public int getBrightnessForRender(float par1) {
        int i = world != null ? super.getBrightnessForRender(par1) : 0;
        short short1 = 240;
        int j = i >> 16 & 255;
        return short1 | j << 16;
    }

    public int getFXLayer() {
        return 3;
    }

    @Override
    public void renderParticle(BufferBuilder buffer, Entity entityIn, float partialTicks, float par3, float par4, float par5, float par6, float par7){
        GL11.glPushMatrix();
        GL11.glPushAttrib(8192);

        RenderHelper.disableStandardItemLighting();

        float lifeRatio = this.particleAge / (float)this.particleMaxAge;

        if (fadeInProgress < 1.0f) {
            fadeInProgress = Math.min(1.0f, fadeInProgress + (FADE_IN_SPEED * partialTicks));
            
            if (fadeInProgress < SCALE_START) {
                currentScale = SCALE_START;
            } else {
                float progress = (fadeInProgress - SCALE_START) / (1.0f - SCALE_START);
                float c4 = (2.0f * (float)Math.PI) / 3.0f;
                
                float t = progress;
                currentScale = SCALE_START + (1.0f - SCALE_START) * (1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t));
                
                if (progress > 0.8f) {
                    float bounce = (float) (Math.sin(progress * Math.PI * 3) * 0.05f);
                    currentScale += bounce;
                }
            }
        }

        if (lifeRatio > 0.6f) {
            float expandProgress = (lifeRatio - 0.6f) / 0.4f;
            float expandFactor = 1.0f + (expandProgress * expandProgress * EXPAND_SPEED);
            currentScale *= expandFactor;
        }

        if (this.lastSwing != RenderParameters.SMOOTH_SWING) {
            this.lastSwing = RenderParameters.SMOOTH_SWING;
            EntityPlayer player = Minecraft.getMinecraft().player;
            double d = Math.random();
            double d2 = Math.random();
            double dist = player.getDistance(this.posX, this.posY, this.posZ);

            if(dist <= 16) {
                float distFactor = (float) (1/dist);
                player.rotationPitch += distFactor * (d < .5 ? this.alpha_rubble : -this.alpha_rubble) * 5;
                player.rotationYaw += distFactor * (d2 < .5 ? this.alpha_rubble : -this.alpha_rubble) * 5;
            }

            if (this.particleAge < 6) {
                this.alpha_rubble += 0.2;
            } else {
                if (lifeRatio > 0.7f) {
                    this.alpha_rubble -= FADE_OUT_SPEED * 2;
                } else {
                    this.alpha_rubble -= FADE_OUT_SPEED;
                }
            }
        }

        if (this.alpha_rubble < 0) {
            this.alpha_rubble = 0;
            if (this.particleAge > this.particleMaxAge * 0.7) {
                this.setExpired();
            }
        }

        double interpolatedX = this.prevPosX + (this.posX - this.prevPosX) * partialTicks;
        double interpolatedY = this.prevPosY + (this.posY - this.prevPosY) * partialTicks;
        double interpolatedZ = this.prevPosZ + (this.posZ - this.prevPosZ) * partialTicks;
        float interpolatedAge = this.lastParticleAge + (this.particleAge - this.lastParticleAge) * partialTicks;

        ResourceLocation texture = this.customTexturePath != null ? 
            new ResourceLocation(this.customTexturePath) : 
            new ResourceLocation(ModularWarfare.MOD_ID, "textures/particles/explosion.png");


        float finalAlpha = this.alpha_rubble * fadeInProgress;


        int baseSize = 80;

        int width = (int)((baseSize + (interpolatedAge * baseSize)) * currentScale);
        int height = (int)((baseSize + (interpolatedAge * 50)) * currentScale);

        RenderHelperMW.renderSmoke(
                texture,
                this.customModelPath, 
                interpolatedX,
                interpolatedY + (interpolatedAge / 100),
                interpolatedZ,
                partialTicks,
                width, height,
                "0xFFFFFF",
                finalAlpha);

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        if (world != null) {
            this.lastParticleAge = this.particleAge;

            if (this.particleAge == 1) {
                this.world.playSound(this.posX, this.posY, this.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 20.0F, 0.9F + this.rand.nextFloat() * 0.15F, true);
            }

            this.prevPosX = this.posX;
            this.prevPosY = this.posY;
            this.prevPosZ = this.posZ;

            if (this.particleAge++ >= this.particleMaxAge) {
                this.setExpired();
            }

            this.motionX *= 0.9990000128746033D;
            this.motionY *= 0.9990000128746033D;
            this.motionZ *= 0.9990000128746033D;

            this.move(this.motionX / 40, this.motionY, this.motionZ / 40);

            if (this.onGround) {
                this.motionX *= 0.699999988079071D;
                this.motionZ *= 0.699999988079071D;
            }
        }
    }


    @SideOnly(Side.CLIENT)
    public static class Factory implements IParticleFactory {
        public Particle createParticle(final int particleID, final World worldIn, final double xCoordIn, final double yCoordIn, final double zCoordIn, final double xSpeedIn, final double ySpeedIn, final double zSpeedIn, final int... p_178902_15_) {
            return new ParticleExplosion(worldIn, xCoordIn, yCoordIn, zCoordIn);
        }
    }
}
