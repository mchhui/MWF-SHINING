package mchhui.hegltf;

import com.modularwarfare.ModConfig;
import com.modularwarfare.client.compat.AtomicShaderCompat;
import com.modularwarfare.client.compat.TextureSamplingRegistry;
import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.client.objloader.api.model.ObjModelRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class DataMesh {
    public String material;
    public boolean skin;
    public boolean proxy;

    protected List<Float> geoList = new ArrayList<>();
    protected int geoCount;
    protected ByteBuffer geoBuffer;
    protected IntBuffer elementBuffer;
    protected int elementCount;
    public int unit;
    public int glDrawingMode = GL11.GL_TRIANGLES;
    private int displayList = -1;
    private int ssboVao = -1;
    private int vertexCount = 0;
    private boolean compiled = false;
    private boolean compiling = false;
    private boolean initSkinning = false;
    private boolean uploadQueued = false;
    private volatile boolean uploadCancelled = false;

    private int pos_vbo = -1;
    private int tex_vbo = -1;
    private int normal_vbo = -1;
    private int vbo = -1;
    private int ebo = -1;
    private int ssbo = -1;

    private static int batchBoundVao = 0;
    private static boolean batchActive = false;
    private static boolean batchTouchedSkinDraw = false;
    private static int batchDepth = 0;

    public static void beginBatch() {
        if (batchDepth++ > 0) {
            return;
        }
        batchActive = true;
        batchBoundVao = -1;
        batchTouchedSkinDraw = false;
    }

    public static void endBatch() {
        if (batchDepth <= 0) {
            return;
        }
        if (--batchDepth > 0) {
            return;
        }
        if (batchBoundVao > 0) {
            GL30.glBindVertexArray(0);
        }
        batchBoundVao = 0;
        batchActive = false;
        if (batchTouchedSkinDraw) {
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            batchTouchedSkinDraw = false;
        }
    }

    private static void bindBatchVao(int vao) {
        if (batchActive) {
            if (batchBoundVao != vao) {
                GL30.glBindVertexArray(vao);
                batchBoundVao = vao;
            }
        } else {
            GL30.glBindVertexArray(vao);
        }
    }

    private static void unbindBatchVao() {
        if (batchActive) {
            return;
        }
        GL30.glBindVertexArray(0);
    }

    public int getDrawVao() {
        if (!this.compiled) {
            return -1;
        }
        return this.skin ? this.ssboVao : this.displayList;
    }

    public boolean isCompiled() {
        return compiled;
    }

    public void requestUpload(String name, boolean priority, Runnable onComplete) {
        if (uploadCancelled) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        if (compiled || uploadQueued) {
            if (compiled && onComplete != null) {
                onComplete.run();
            }
            return;
        }
        uploadQueued = true;
        int bytes = estimateCpuBytes();
        int weight = GltfGpuUploadScheduler.estimateWeight(bytes);
        GltfGpuUploadScheduler.add("upload:" + name, weight, priority, () -> {
            try {
                if (!uploadCancelled && !compiled) {
                    compileVAOSliced();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    private int estimateCpuBytes() {
        int n = 0;
        if (geoBuffer != null) {
            n += geoBuffer.capacity();
        }
        if (elementBuffer != null) {
            n += elementBuffer.capacity() * 4;
        }
        if (geoList != null) {
            n += geoList.size() * 4;
        }
        return Math.max(n, 1024);
    }

    public void render() {
        if (!this.compiled) {
            if (!uploadQueued) {
                requestUpload("onDemand", true, null);
            }
            return;
        }
        boolean atomicGlow = ObjModelRenderer.glowTxtureMode
            && AtomicShaderCompat.prepareGlowMapEmissive(ObjModelRenderer.glowType, ObjModelRenderer.glowPath);
        this.callVAO();
        if (atomicGlow) {
            AtomicShaderCompat.clearEmissive();
            TextureSamplingRegistry.restoreDefaultTexUnit();
            return;
        }

        if (ObjModelRenderer.glowTxtureMode && !AtomicShaderCompat.isGBufferFillActive()
            && !AtomicShaderCompat.isShadowDepthActive()) {
            if (!ObjModelRenderer.customItemRenderer.bindTextureGlow(ObjModelRenderer.glowType, ObjModelRenderer.glowPath)) {
                return;
            }
            float x = OpenGlHelper.lastBrightnessX;
            float y = OpenGlHelper.lastBrightnessY;
            ObjModelRenderer.glowTxtureMode = false;
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            callVAO();
            GlStateManager.enableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            ObjModelRenderer.glowTxtureMode = true;
            ObjModelRenderer.customItemRenderer.bindTexture(ObjModelRenderer.glowType, ObjModelRenderer.glowPath);

            if (Minecraft.getMinecraft().currentScreen instanceof GuiGunModify) {
                GlStateManager.disableLighting();
            }
        }
    }

    private void compileVAOSliced() {
        if (this.compiling || this.compiled || this.uploadCancelled) {
            return;
        }
        if (this.unit == 3) {
            if (this.geoList == null || this.geoList.isEmpty()) {
                this.compiling = false;
                this.uploadQueued = false;
                return;
            }
        } else if (this.geoBuffer == null || this.elementBuffer == null) {
            this.compiling = false;
            this.uploadQueued = false;
            return;
        }
        this.compiling = true;
        this.ssboVao = GL30.glGenVertexArrays();
        this.displayList = GL30.glGenVertexArrays();

        int partSize = 65536;
        if (ModConfig.INSTANCE != null && ModConfig.INSTANCE.gltf != null) {
            partSize = Math.max(1024, ModConfig.INSTANCE.gltf.uploadPartSize);
        }

        if (this.unit == 3) {
            final List<Float> list = this.geoList;
            this.vertexCount = list.size() / this.unit;

            FloatBuffer pos_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
            FloatBuffer tex_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
            FloatBuffer normal_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);

            for (int i = 0, size = list.size(); i + 8 <= size; i += 8) {
                pos_floatBuffer.put(list.get(i));
                pos_floatBuffer.put(list.get(i + 1));
                pos_floatBuffer.put(list.get(i + 2));
                tex_floatBuffer.put(list.get(i + 3));
                tex_floatBuffer.put(list.get(i + 4));
                normal_floatBuffer.put(list.get(i + 5));
                normal_floatBuffer.put(list.get(i + 6));
                normal_floatBuffer.put(list.get(i + 7));
            }
            pos_floatBuffer.flip();
            tex_floatBuffer.flip();
            normal_floatBuffer.flip();

            GL30.glBindVertexArray(this.displayList);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            this.pos_vbo = GL15.glGenBuffers();
            this.tex_vbo = GL15.glGenBuffers();
            this.normal_vbo = GL15.glGenBuffers();

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pos_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, pos_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, tex_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tex_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normal_vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normal_floatBuffer, GL15.GL_STATIC_DRAW);
            GL11.glNormalPointer(GL11.GL_FLOAT, 0, 0);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            this.compiled = true;
            this.compiling = false;
        } else {
            this.vbo = GL15.glGenBuffers();
            this.ebo = GL15.glGenBuffers();
            this.geoBuffer.flip();
            this.elementBuffer.flip();

            int geoBytes = this.geoBuffer.remaining();
            int eleBytes = this.elementBuffer.remaining() * 4;

            GL30.glBindVertexArray(this.displayList);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, geoBytes, GL15.GL_STATIC_DRAW);
            uploadBufferSliced(GL15.GL_ARRAY_BUFFER, this.vbo, this.geoBuffer, partSize);
            int step = 17 * Float.BYTES;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, step, 0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, step, 3 * Float.BYTES);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, step, 5 * Float.BYTES);
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, step, 8 * Float.BYTES);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, step, 12 * Float.BYTES);
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, step, 16 * Float.BYTES);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, eleBytes, GL15.GL_STATIC_DRAW);
            ByteBuffer eleBytesBuf = BufferUtils.createByteBuffer(eleBytes);
            IntBuffer dup = this.elementBuffer.duplicate();
            while (dup.hasRemaining()) {
                eleBytesBuf.putInt(dup.get());
            }
            eleBytesBuf.flip();
            uploadBufferSliced(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo, eleBytesBuf, partSize);

            this.ssbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, this.ssbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, geoBytes, GL15.GL_DYNAMIC_COPY);
            this.geoBuffer.rewind();
            uploadBufferSliced(GL43.GL_SHADER_STORAGE_BUFFER, this.ssbo, this.geoBuffer, partSize);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            GL30.glBindVertexArray(this.ssboVao);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.ssbo);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 8 * Float.BYTES, 0);
            GL11.glNormalPointer(GL11.GL_FLOAT, 8 * Float.BYTES, 3 * Float.BYTES);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 8 * Float.BYTES, 6 * Float.BYTES);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.ebo);

            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
            GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0);

            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);

            this.skin = true;
            this.compiled = true;
            this.compiling = false;
        }

        dropCpuData();
    }

    private static void uploadBufferSliced(int target, int bufferId, ByteBuffer data, int partSize) {
        GL15.glBindBuffer(target, bufferId);
        int pos = data.position();
        int lim = data.limit();
        int offset = 0;
        while (pos < lim) {
            int chunk = Math.min(partSize, lim - pos);
            data.position(pos);
            data.limit(pos + chunk);
            ByteBuffer slice = data.slice();
            GL15.glBufferSubData(target, offset, slice);
            offset += chunk;
            pos += chunk;
        }
        data.position(0);
        data.limit(lim);
    }

    public void dropCpuData() {
        if (this.geoList != null) {
            this.geoList.clear();
            this.geoList = null;
        }
        if (this.geoBuffer != null) {
            try {
                if (((sun.nio.ch.DirectBuffer) this.geoBuffer).cleaner() != null) {
                    ((sun.nio.ch.DirectBuffer) this.geoBuffer).cleaner().clean();
                }
            } catch (Throwable ignored) {
            }
            this.geoBuffer = null;
        }
        if (this.elementBuffer != null) {
            try {
                if (((sun.nio.ch.DirectBuffer) this.elementBuffer).cleaner() != null) {
                    ((sun.nio.ch.DirectBuffer) this.elementBuffer).cleaner().clean();
                }
            } catch (Throwable ignored) {
            }
            this.elementBuffer = null;
        }
    }

    public void callSkinning() {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, this.ssbo);
            GL30.glBindVertexArray(this.displayList);
            GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
            this.initSkinning = true;
        }
    }

    private void callVAO() {
        if (!this.compiled) {
            return;
        }
        if (this.skin) {
            if (!this.initSkinning) {
                return;
            }
            bindBatchVao(this.ssboVao);
            batchTouchedSkinDraw = true;
            GL11.glDrawElements(this.glDrawingMode, this.elementCount, GL11.GL_UNSIGNED_INT, 0);
            unbindBatchVao();
            if (!batchActive) {
                GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
            }
        } else {
            bindBatchVao(this.displayList);
            GL11.glDrawArrays(this.glDrawingMode, 0, this.vertexCount);
            unbindBatchVao();
        }
    }

    public void delete() {
        uploadCancelled = true;
        try {
            if (org.lwjgl.opengl.GLContext.getCapabilities() != null) {
                if (displayList > 0) {
                    GL30.glDeleteVertexArrays(this.displayList);
                }
                if (ssboVao > 0) {
                    GL30.glDeleteVertexArrays(this.ssboVao);
                }
                if (this.pos_vbo != -1) {
                    GL15.glDeleteBuffers(this.pos_vbo);
                }
                if (this.tex_vbo != -1) {
                    GL15.glDeleteBuffers(this.tex_vbo);
                }
                if (this.normal_vbo != -1) {
                    GL15.glDeleteBuffers(this.normal_vbo);
                }
                if (this.vbo != -1) {
                    GL15.glDeleteBuffers(this.vbo);
                }
                if (this.ebo != -1) {
                    GL15.glDeleteBuffers(this.ebo);
                }
                if (this.ssbo != -1) {
                    GL15.glDeleteBuffers(this.ssbo);
                }
            }
        } catch (Throwable ignored) {
        }
        displayList = -1;
        ssboVao = -1;
        pos_vbo = -1;
        tex_vbo = -1;
        normal_vbo = -1;
        vbo = -1;
        ebo = -1;
        ssbo = -1;
        compiled = false;
        compiling = false;
        uploadQueued = false;
        initSkinning = false;
        dropCpuData();
    }
}

