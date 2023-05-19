package com.modularwarfare.client.shader;

import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ProjectionHelper {

    private IntBuffer viewport = GLAllocation.createDirectIntBuffer(16);

    private FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);

    private FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);

    private FloatBuffer objectCoords = GLAllocation.createDirectFloatBuffer(3);

    private FloatBuffer winCoords = GLAllocation.createDirectFloatBuffer(3);

    public void updateMatrices() {
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelview);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
    }

    public Vec3d unproject(float winX, float winY, float winZ) {
        GLU.gluUnProject(winX, winY, winZ, modelview, projection, viewport, objectCoords);

        float objectX = objectCoords.get(0);
        float objectY = objectCoords.get(1);
        float objectZ = objectCoords.get(2);

        return new Vec3d(objectX, objectY, objectZ);
    }

    public Vec3d project(float objX, float objY, float objZ) {

        int[] vp = new int[2];
        //vp=viewport.flip();
        viewport.rewind();
        int x = viewport.get(viewport.position() + 2);
        int y = viewport.get(viewport.position() + 3);
        viewport.rewind();


        GLU.gluProject(objX, objY, objZ, modelview, projection, viewport, winCoords);

        float winX = winCoords.get(0);
        float winY = winCoords.get(1);
        float winZ = winCoords.get(2);

        return new Vec3d(winX, winY, winZ);
    }
}