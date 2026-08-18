package com.modularwarfare.client.texture;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.modularwarfare.client.compat.TextureSamplingRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

/**
 * Registers immediately with a 1x1 placeholder; decodes and uploads full image asynchronously.
 * Used for effect / mask / overlay / flash textures that are not force-preloaded at startup.
 */
public class LazyLoadTexture extends AbstractTexture {
    private static final ExecutorService DECODE_POOL = Executors.newFixedThreadPool(2, new ThreadFactory() {
        private final AtomicInteger n = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "mwf-lazy-tex-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    private final ResourceLocation location;
    private final boolean linearSampling;
    private volatile boolean loadQueued;
    private volatile boolean ready;
    private volatile boolean failed;

    public LazyLoadTexture(ResourceLocation location, boolean linearSampling) {
        this.location = location;
        this.linearSampling = linearSampling;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isFailed() {
        return failed;
    }

    @Override
    public void loadTexture(IResourceManager resourceManager) throws IOException {
        int id = getGlTextureId();
        uploadPlaceholder(id);
        TextureSamplingRegistry.register(location, linearSampling);
        queueAsyncLoad();
    }

    private void queueAsyncLoad() {
        if (loadQueued || ready || failed) {
            return;
        }
        loadQueued = true;
        final ResourceLocation loc = this.location;
        final boolean linear = this.linearSampling;
        DECODE_POOL.execute(() -> {
            BufferedImage image = null;
            try {
                IResource res = Minecraft.getMinecraft().getResourceManager().getResource(loc);
                image = TextureUtil.readBufferedImage(res.getInputStream());
                if (image == null) {
                    throw new IOException("null image");
                }
                final int w = image.getWidth();
                final int h = image.getHeight();
                final int[] pixels = new int[w * h];
                image.getRGB(0, 0, w, h, pixels, 0, w);
                final BufferedImage imgFinal = image;
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    try {
                        if (glTextureId != -1) {
                            GlStateManager.bindTexture(glTextureId);
                            uploadRgba(glTextureId, w, h, pixels);
                            applyFilter(linear);
                            ready = true;
                        }
                    } catch (Throwable t) {
                        failed = true;
                        t.printStackTrace();
                    } finally {
                        imgFinal.flush();
                    }
                });
            } catch (Throwable t) {
                failed = true;
                if (image != null) {
                    image.flush();
                }
                // Keep placeholder; avoid caching MISSING via bindTexture.
            }
        });
    }

    private static void uploadPlaceholder(int texId) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(4);
        buf.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0);
        buf.flip();
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, 1, 1, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
    }

    private static void uploadRgba(int texId, int w, int h, int[] argb) {
        ByteBuffer buf = org.lwjgl.BufferUtils.createByteBuffer(w * h * 4);
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            buf.put((byte) ((p >> 16) & 0xFF));
            buf.put((byte) ((p >> 8) & 0xFF));
            buf.put((byte) (p & 0xFF));
            buf.put((byte) ((p >> 24) & 0xFF));
        }
        buf.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
    }

    private static void applyFilter(boolean linear) {
        int minMag = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, minMag);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, minMag);
    }
}
