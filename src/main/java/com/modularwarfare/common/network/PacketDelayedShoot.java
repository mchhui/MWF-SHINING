package com.modularwarfare.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.GlStateManager;
import java.util.ArrayList;
import java.util.List;

import com.modularwarfare.client.model.BeamRenderer;

public class PacketDelayedShoot extends PacketBase {

    public int entityId;
    public int targetEntityId;
    public double targetX, targetY, targetZ;
    public float offsetX, offsetY, offsetZ;
    public int delayTicks;
    public boolean isCoordinateShoot;

    public PacketDelayedShoot() {
    }

    public PacketDelayedShoot(int entityId, int targetEntityId, double targetX, double targetY, double targetZ, 
                             float offsetX, float offsetY, float offsetZ, int delayTicks, boolean isCoordinateShoot) {
        this.entityId = entityId;
        this.targetEntityId = targetEntityId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.delayTicks = delayTicks;
        this.isCoordinateShoot = isCoordinateShoot;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(this.entityId);
        data.writeInt(this.targetEntityId);
        data.writeDouble(this.targetX);
        data.writeDouble(this.targetY);
        data.writeDouble(this.targetZ);
        data.writeFloat(this.offsetX);
        data.writeFloat(this.offsetY);
        data.writeFloat(this.offsetZ);
        data.writeInt(this.delayTicks);
        data.writeBoolean(this.isCoordinateShoot);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        this.entityId = data.readInt();
        this.targetEntityId = data.readInt();
        this.targetX = data.readDouble();
        this.targetY = data.readDouble();
        this.targetZ = data.readDouble();
        this.offsetX = data.readFloat();
        this.offsetY = data.readFloat();
        this.offsetZ = data.readFloat();
        this.delayTicks = data.readInt();
        this.isCoordinateShoot = data.readBoolean();
    }

    @Override
    public void handleServerSide(EntityPlayerMP entityPlayer) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer entityPlayer) {
        RayRenderer.addRay(this);
    }
    
    @SideOnly(Side.CLIENT)
    public static class RayRenderer {
        private static final List<RayRenderData> activeRays = new ArrayList<>();
        private static final ResourceLocation RAY_TEXTURE = new ResourceLocation("modularwarfare:textures/particles/targetline.png");
        private static final float RAY_RADIUS = 0.01F;
        private static final float RAY_ALPHA = 0.35F;
        private static final float START_FADE = 1.5F;
        private static final float END_FADE = 1.5F;
        private static final float START_CLIP = 0.1F;
        private static final float END_CLIP = 0.35F;
        private static final Vec3d START_OFFSET = new Vec3d(0.0D, 0.0D, 0.0D);
        private static final Vec3d END_OFFSET = new Vec3d(0.0D, 0.0D, 0.0D);
        
        static {
            MinecraftForge.EVENT_BUS.register(RayRenderer.class);
        }
        
        private static class RayRenderData {
            public final PacketDelayedShoot packet;
            public int remainingRenderTicks;
            public long lastTickTime;
            
            public RayRenderData(PacketDelayedShoot packet) {
                this.packet = packet;
                this.remainingRenderTicks = packet.delayTicks;
                this.lastTickTime = System.currentTimeMillis();
            }
        }
        
        public static void addRay(PacketDelayedShoot packet) {
            activeRays.add(new RayRenderData(packet));
        }
        
        @SubscribeEvent
        public static void onRenderWorldLast(RenderWorldLastEvent event) {
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer player = mc.player;
            if (player == null) return;
            
            double playerX = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
            double playerY = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
            double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
            
            GlStateManager.pushMatrix();
            GlStateManager.translate(-playerX, -playerY, -playerZ);
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(false);
            GlStateManager.disableDepth();
            GlStateManager.disableCull();
            
            long currentTime = System.currentTimeMillis();
            
            activeRays.removeIf(rayData -> {
                if (rayData.remainingRenderTicks <= 0) {
                    return true;
                }
                
                if (currentTime - rayData.lastTickTime >= 50) {
                    rayData.remainingRenderTicks--;
                    rayData.lastTickTime = currentTime;
                }
                
                World world = mc.world;
                Entity shooter = world.getEntityByID(rayData.packet.entityId);
                if (!(shooter instanceof EntityLivingBase)) {
                    return true;
                }
                EntityLivingBase shooterEntity = (EntityLivingBase) shooter;
                
                float partialTicks = event.getPartialTicks();
                
                Vec3d startPos = shooterEntity.getPositionEyes(partialTicks).add(rayData.packet.offsetX, rayData.packet.offsetY, rayData.packet.offsetZ);
                
                Vec3d endPos;
                if (rayData.packet.isCoordinateShoot) {
                    endPos = new Vec3d(rayData.packet.targetX, rayData.packet.targetY, rayData.packet.targetZ);
                } else {
                    Entity target = world.getEntityByID(rayData.packet.targetEntityId);
                    if (!(target instanceof EntityLivingBase)) {
                        return true;
                    }
                    EntityLivingBase targetEntity = (EntityLivingBase) target;
                    endPos = targetEntity.getPositionEyes(partialTicks);
                }
                
                Vec3d clippedStart = startPos.add(START_OFFSET);
                Vec3d clippedEnd = endPos.add(END_OFFSET);
                Vec3d actualEndPos = checkRayBlocking(world, clippedStart, clippedEnd);

                Vec3d lineVec = actualEndPos.subtract(clippedStart);
                double len = lineVec.length();
                if (len < 0.001D) {
                    return false;
                }

                Vec3d dir = lineVec.scale(1.0D / len);
                double totalClip = START_CLIP + END_CLIP;
                if (len <= totalClip + 0.001D) {
                    return false;
                }

                Vec3d beamStart = clippedStart.add(dir.scale(START_CLIP));
                Vec3d beamEnd = actualEndPos.subtract(dir.scale(END_CLIP));

                BeamRenderer.renderTexturedCylinderBeam(
                        beamStart,
                        beamEnd,
                        RAY_RADIUS,
                        1.0F,
                        1.0F,
                        1.0F,
                        RAY_ALPHA,
                        RAY_TEXTURE,
                        START_FADE,
                        END_FADE,
                        12,
                        18,
                        1.25F
                );
                
                return false;
            });
            
            GlStateManager.enableDepth();
            GlStateManager.enableCull();
            GlStateManager.depthMask(true);
            GlStateManager.enableLighting();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
        
        private static Vec3d checkRayBlocking(World world, Vec3d start, Vec3d end) {
            net.minecraft.util.math.RayTraceResult result = world.rayTraceBlocks(start, end, false, true, false);
            if (result != null && result.typeOfHit == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
                return result.hitVec;
            }
            return end;
        }
    }
} 