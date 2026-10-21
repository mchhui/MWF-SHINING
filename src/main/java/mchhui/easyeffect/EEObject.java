package mchhui.easyeffect;

import java.util.HashMap;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

public class EEObject {
    public static HashMap<String,ResourceLocation> glowTextrues=new HashMap<String, ResourceLocation>();
    public static HashMap<String,ResourceLocation>textrues=new HashMap<String, ResourceLocation>();
    
    public String name;
    public int fps;
    public int unit;
    private int unit2;
    public int length;
    public double x;
    public double y;
    public double z;
    public double vx;
    public double vy;
    public double vz;
    public double ax;
    public double ay;
    public double az;
    public double size;
    public long beginTime;
    
    
    
    public EEObject(String name, int delay, int fps, int unit, int length, double x, double y, double z, double vx, double vy,
            double vz, double ax, double ay, double az, double size,
            long beginTime) {
        super();
        this.name = name;
        this.fps = fps;
        this.unit = unit;
        this.unit2 = unit*unit;
        this.length = length;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.ax = ax;
        this.ay = ay;
        this.az = az;
        this.size = size;
        this.beginTime = beginTime+delay;
    }

    public void render(EasyEffectRenderer render,long time,float partialTicks) {
        float timeSec=(time-beginTime)/1000f;
        if(timeSec<0) {
            return;
        }
        
        GlStateManager.pushMatrix();
        
        float renderingX=(float) (x+vx*timeSec+0.5f*ax*timeSec*timeSec);
        float renderingY=(float) (y+vy*timeSec+0.5f*ay*timeSec*timeSec);
        float renderingZ=(float) (z+vz*timeSec+0.5f*az*timeSec*timeSec);
        
        GlStateManager.color(1, 1, 1,1);
        
        GlStateManager.translate(renderingX-render.viewEntityRenderingPosX, renderingY-render.viewEntityRenderingPosY, renderingZ-render.viewEntityRenderingPosZ);
        GlStateManager.scale(size, size, size);
        GlStateManager.rotate(render.viewEntityRenderingYaw+90, 0, -1, 0);
        GlStateManager.rotate(render.viewEntityRenderingPitch, 0, 0, 1);
        
        if(textrues.containsKey(name)) {
            Minecraft.getMinecraft().renderEngine.bindTexture(textrues.get(name));
        }else {
            textrues.put(name, new ResourceLocation(name));
            if(Minecraft.getMinecraft().renderEngine.getTexture(new ResourceLocation(name.replaceFirst("(?s)[.](?!.*?[.])", "_e.")))!=null) {
                glowTextrues.put(name, new ResourceLocation(name.replaceFirst("(?s)[.](?!.*?[.])", "_e.")));
            }
        }

        
        int frame=(int)(timeSec*fps);
        if(frame>=unit2) {
            frame=unit2-1;
        }
        
        float offsetX=(int)(frame%unit);
        float offsetY=(int)(frame/unit);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(0, 0.5, 0.5).tex((0+offsetX)/unit, (0+offsetY)/unit).endVertex();
        bufferbuilder.pos(0, -0.5, 0.5).tex((0+offsetX)/unit, (1+offsetY)/unit).endVertex();
        bufferbuilder.pos(0, -0.5, -0.5).tex((1+offsetX)/unit, (1+offsetY)/unit).endVertex();
        bufferbuilder.pos(0, 0.5, -0.5).tex((1+offsetX)/unit, (0+offsetY)/unit).endVertex();
        tessellator.draw();
        
        
        if(glowTextrues.containsKey(name)) {
            GlStateManager.disableLighting();
            float lastBrightnessX = OpenGlHelper.lastBrightnessX;
            float lastBrightnessY = OpenGlHelper.lastBrightnessY;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
            GlStateManager.depthMask(false);
            GlStateManager.enableBlend();
            GlStateManager.depthFunc(GL11.GL_EQUAL);
            Minecraft.getMinecraft().renderEngine.bindTexture(glowTextrues.get(name));
            bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferbuilder.pos(0, 0.5, 0.5).tex((0+offsetX)/unit, (0+offsetY)/unit).endVertex();
            bufferbuilder.pos(0, -0.5, 0.5).tex((0+offsetX)/unit, (1+offsetY)/unit).endVertex();
            bufferbuilder.pos(0, -0.5, -0.5).tex((1+offsetX)/unit, (1+offsetY)/unit).endVertex();
            bufferbuilder.pos(0, 0.5, -0.5).tex((1+offsetX)/unit, (0+offsetY)/unit).endVertex();
            tessellator.draw();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
            GlStateManager.enableLighting();
        }
        
        
        GlStateManager.popMatrix();
    }
    
    public boolean isShutdown(long time) {
        return time-beginTime>=length/20f*1000;
        //return false;
    }
}
