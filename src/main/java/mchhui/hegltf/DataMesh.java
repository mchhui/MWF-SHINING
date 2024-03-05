package mchhui.hegltf;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.lwjgl.BufferUtils;
import org.lwjgl.MemoryUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL42;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL45;

import com.modularwarfare.client.gui.GuiGunModify;
import com.modularwarfare.loader.api.model.ObjModelRenderer;
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
    private static final int SIZE_OF_FLOAT = 4;
    private static final int SIZE_OF_INT = 4;
    public String material;
    public boolean skin;
    public int unit;
    public int glDrawingMode = GL11.GL_TRIANGLES;
    protected List<Float> geoList = new ArrayList<Float>();
    protected int geoCount;
    protected ByteBuffer geoBuffer;
    protected IntBuffer elementBuffer;
    protected int elementCount;
    private int displayList = -1;
    private int ssboVao = -1;
    private int vertexCount = 0;
    private boolean compiled = false;
    private boolean compiling = false;
    private boolean initSkinning = false;

    // BUFFER OBJECT
    private int pos_vbo = -1;
    private int tex_vbo = -1;
    private int normal_vbo = -1;
    private int vbo = -1;
    private int ebo = -1;
    private int ssbo = -1;

    public void render() {
        if (!compiled) {
            try {
                compileVAO(1);
                return;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        /*
         * 如果需要 可加入纹理处理内容
         * */

        callVAO();

        if (ObjModelRenderer.glowTxtureMode) {
            if (!ObjModelRenderer.customItemRenderer.bindTextureGlow(ObjModelRenderer.glowType, ObjModelRenderer.glowPath)) {
                return;
            }
            float x = OpenGlHelper.lastBrightnessX;
            float y = OpenGlHelper.lastBrightnessY;
            ObjModelRenderer.glowTxtureMode = false;
            GlStateManager.depthMask(false);
            //GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            GlStateManager.disableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            callVAO();
            GlStateManager.enableLighting();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, x, y);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            //GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            ObjModelRenderer.glowTxtureMode = true;
            ObjModelRenderer.customItemRenderer.bindTexture(ObjModelRenderer.glowType, ObjModelRenderer.glowPath);

            //垃圾bug 迟早把这改装界面扬了
            if (Minecraft.getMinecraft().currentScreen instanceof GuiGunModify) {
                GlStateManager.disableLighting();
            }
        }
    }

    private void compileVAO(float scale) {
        if (compiling) {
            return;
        }
        compiling = true;
        this.ssboVao = GL30.glGenVertexArrays();
        this.displayList = GL30.glGenVertexArrays();

        if (unit == 3) {
            vertexCount = geoList.size() / unit;

            FloatBuffer pos_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);
            FloatBuffer tex_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 2);
            FloatBuffer normal_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 3);

            IntBuffer joint_intBuffer = BufferUtils.createIntBuffer(vertexCount * 4);
            FloatBuffer weight_floatBuffer = BufferUtils.createFloatBuffer(vertexCount * 4);

            int count = 0;
            for (int i = 0; i < geoList.size(); i++) {
                if (count == 8) {
                    count = 0;
                }
                if (count < 3) {
                    pos_floatBuffer.put(geoList.get(i));
                } else if (count < 5) {
                    tex_floatBuffer.put(geoList.get(i));
                } else if (count < 8) {
                    normal_floatBuffer.put(geoList.get(i));
                }
                count++;
            }
            pos_floatBuffer.flip();
            tex_floatBuffer.flip();
            normal_floatBuffer.flip();

            GL30.glBindVertexArray(displayList);
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            pos_vbo = GL15.glGenBuffers();
            tex_vbo = GL15.glGenBuffers();
            normal_vbo = GL15.glGenBuffers();

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
            vbo = GL15.glGenBuffers();
            ebo = GL15.glGenBuffers();
            geoBuffer.flip();
            elementBuffer.flip();
            GL30.glBindVertexArray(displayList);
            GL20.glEnableVertexAttribArray(0);
            GL20.glEnableVertexAttribArray(1);
            GL20.glEnableVertexAttribArray(2);
            GL20.glEnableVertexAttribArray(3);
            GL20.glEnableVertexAttribArray(4);
            GL20.glEnableVertexAttribArray(5);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, geoBuffer, GL15.GL_STATIC_DRAW);
            int step = 17 * SIZE_OF_FLOAT;
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, step, 0);
            GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, step, 3 * SIZE_OF_FLOAT);
            GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, step, 5 * SIZE_OF_FLOAT);
            // in fact it is u_int:
            GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, step, 8 * SIZE_OF_FLOAT);
            GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, step, 12 * SIZE_OF_FLOAT);
            // in fact it is u_int:
            GL20.glVertexAttribPointer(5, 1, GL11.GL_FLOAT, false, step, 16 * SIZE_OF_FLOAT);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, elementBuffer, GL15.GL_STATIC_DRAW);
            ssbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, ssbo);
            GL15.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, geoBuffer, GL15.GL_DYNAMIC_COPY);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

            GL30.glBindVertexArray(ssboVao);

            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, ssbo);
            GL11.glVertexPointer(3, GL11.GL_FLOAT, 8 * SIZE_OF_FLOAT, 0);
            GL11.glNormalPointer(GL11.GL_FLOAT, 8 * SIZE_OF_FLOAT, 3 * SIZE_OF_FLOAT);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, 8 * SIZE_OF_FLOAT, 6 * SIZE_OF_FLOAT);

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);

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
    }

    public void callSkinning() {
        if (!compiled) {
            return;
        }
        if (skin) {
            GL30.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, ShaderGltf.VERTEXBUFFERBINDING, ssbo);
            GL30.glBindVertexArray(displayList);
            GL11.glDrawElements(glDrawingMode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            GL42.glMemoryBarrier(GL43.GL_SHADER_STORAGE_BARRIER_BIT);
            initSkinning = true;
        }
    }

    private void callVAO() {
        if (!compiled) {
            return;
        }
        if (skin) {
            if (!initSkinning) {
                return;
            }
            GL30.glBindVertexArray(ssboVao);
            GL11.glDrawElements(glDrawingMode, elementCount, GL11.GL_UNSIGNED_INT, 0);
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);
        } else {
            GL30.glBindVertexArray(displayList);
            GL11.glDrawArrays(glDrawingMode, 0, vertexCount);
            GL30.glBindVertexArray(0);
        }
    }

    public void delete() {
        if(geoBuffer!=null) {
            if(((sun.misc.Cleaner)((sun.nio.ch.DirectBuffer)geoBuffer).cleaner())!=null) {
                ((sun.misc.Cleaner)((sun.nio.ch.DirectBuffer)geoBuffer).cleaner()).clean();  
            }
        }
        if(elementBuffer!=null) {
            if(((sun.misc.Cleaner)((sun.nio.ch.DirectBuffer)elementBuffer).cleaner())!=null) {
                ((sun.misc.Cleaner)((sun.nio.ch.DirectBuffer)elementBuffer).cleaner()).clean();  
            }
        }
        GL30.glDeleteVertexArrays(displayList);
        GL30.glDeleteVertexArrays(ssboVao);
        if (pos_vbo != -1) {
            GL15.glDeleteBuffers(pos_vbo);
        }
        if (tex_vbo != -1) {
            GL15.glDeleteBuffers(tex_vbo);
        }
        if (normal_vbo != -1) {
            GL15.glDeleteBuffers(normal_vbo);
        }
        if (vbo != -1) {
            GL15.glDeleteBuffers(vbo);
        }
        if (ebo != -1) {
            GL15.glDeleteBuffers(ebo);
        }
        if (ssbo != -1) {
            GL15.glDeleteBuffers(ssbo);
        }
    }
}
