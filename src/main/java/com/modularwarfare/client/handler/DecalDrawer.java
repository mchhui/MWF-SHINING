package com.modularwarfare.client.handler;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import com.modularwarfare.ModularWarfare;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

public class DecalDrawer {
    public static class BulletHole {
        public int aliveTime;
        public double posX;
        public double posY;
        public double posZ;
        public double aX;
        public double aY;
        public double aZ;
        public RayTraceResult result;
        public EnumFacing direction;

        public BulletHole(int aliveTime, double posX, double posY, double posZ, double aX, double aY, double aZ) {
            this.aliveTime = aliveTime;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.aX = aX;
            this.aY = aY;
            this.aZ = aZ;
        }
    }

    public static ArrayList<BulletHole> bulletHoles = new ArrayList<DecalDrawer.BulletHole>();
    public static int bulletHoleVao = -1;

    @SubscribeEvent
    public void onClientTick(ClientTickEvent event) {
        if (event.phase == Phase.END) {
            return;
        }
        if (Minecraft.getMinecraft().world == null) {
            bulletHoles.clear();
            return;
        }
        for (int i = 0; i < bulletHoles.size(); i++) {
            if (--bulletHoles.get(i).aliveTime == 0) {
                bulletHoles.remove(i);
                i--;
            } else {
                BulletHole hole = bulletHoles.get(i);
                if (hole.result == null) {
                    hole.result = Minecraft.getMinecraft().world.rayTraceBlocks(new Vec3d(hole.posX, hole.posY, hole.posZ), new Vec3d(hole.posX + hole.aX, hole.posY + hole.aY, hole.posZ + hole.aZ));
                    if (hole.result == null || hole.result.hitVec == null) {
                        bulletHoles.remove(i);
                        i--;
                        continue;
                    } else {
                        hole.posX = hole.result.hitVec.x - hole.aX * 0.01;
                        hole.posY = hole.result.hitVec.y - hole.aY * 0.01;
                        hole.posZ = hole.result.hitVec.z - hole.aZ * 0.01;
                        hole.direction = hole.result.sideHit;
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (Minecraft.getMinecraft().player == null) {
            return;
        }
        if (bulletHoleVao == -1) {
            // 创建带UV的正方形（弹孔贴花）VAO
            float size = 1f; // 1像素为0.0625（16px纹理），可自行调整大小
            float[] vertices = {
                // x, y, z, u, v
                -size, -size, 0f, 0f, 0f, // 左下
                size, -size, 0f, 1f, 0f, // 右下
                size, size, 0f, 1f, 1f, // 右上
                -size, size, 0f, 0f, 1f // 左上
            };
            int vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            int vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
            buffer.put(vertices).flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 5 * 4, 0); // 每顶点前3个float
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 5 * 4, 3 * 4); // 后2个float为uv
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL30.glBindVertexArray(0);
            bulletHoleVao = vao;
        }

        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.depthMask(false);
        String location = ModularWarfare.MOD_ID + ":textures/entity/bullethole/bullethole0.png";
        Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation(location));
        GL30.glBindVertexArray(bulletHoleVao);
        EntityPlayer player = Minecraft.getMinecraft().player;
        for (int i = 0; i < bulletHoles.size(); i++) {
            BulletHole hole = bulletHoles.get(i);
            if (hole.result != null) {
                GlStateManager.pushMatrix();
                double x = player.prevPosX + (player.posX - player.prevPosX) * event.getPartialTicks();
                double y = player.prevPosY + (player.posY - player.prevPosY) * event.getPartialTicks();
                double z = player.prevPosZ + (player.posZ - player.prevPosZ) * event.getPartialTicks();
                x = hole.posX - x;
                y = hole.posY - y;
                z = hole.posZ - z;
                // 按理讲最好算个光照 不过纯黑贴图不搞应该也无所谓
                float alpha = (hole.aliveTime > 100 ? 100 : hole.aliveTime) / 100f;
                GlStateManager.color(1, 1, 1, alpha);
                GlStateManager.translate(x, y, z);
                switch (hole.direction) {
                    case DOWN:
                        GlStateManager.rotate(90, 1, 0, 0);
                        break;
                    case UP:
                        GlStateManager.rotate(-90, 1, 0, 0);
                        break;
                    case NORTH:
                        GlStateManager.rotate(180, 0, 1, 0);
                        break;
                    case SOUTH:
                        GlStateManager.rotate(0, 0, 1, 0);
                        break;
                    case EAST:
                        GlStateManager.rotate(90, 0, 1, 0);
                        break;
                    case WEST:
                        GlStateManager.rotate(-90, 0, 1, 0);
                        break;
                    default:
                        break;
                }
                GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
                GlStateManager.popMatrix();
            }
        }
        GL30.glBindVertexArray(0);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }
}
