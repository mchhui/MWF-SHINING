package com.modularwarfare.client.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.io.InputStream;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.ARBShaderObjects.*;

public class Program {
    private final int program;


    public Program(ResourceLocation vertexShader) throws Exception {
        int vertShader = createShader(vertexShader, ARBVertexShader.GL_VERTEX_SHADER_ARB);

        program = glCreateProgramObjectARB();
        if (program == 0) {
            throw new Exception("glCreateProgramObjectARB failed");
        }

        glAttachObjectARB(program, vertShader);

        glLinkProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error linking: " + getLogInfo(program));
        }

        glValidateProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error validating: " + getLogInfo(program));
        }
    }

    public Program(ResourceLocation vertexShader, ResourceLocation fragmentShader) throws Exception {
        int vertShader = createShader(vertexShader, ARBVertexShader.GL_VERTEX_SHADER_ARB);
        int fragShader = createShader(fragmentShader, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        program = glCreateProgramObjectARB();
        if (program == 0) {
            throw new Exception("glCreateProgramObjectARB failed");
        }

        glAttachObjectARB(program, vertShader);
        glAttachObjectARB(program, fragShader);

        glLinkProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_LINK_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error linking: " + getLogInfo(program));
        }

        glValidateProgramARB(program);
        if (glGetObjectParameteriARB(program, GL_OBJECT_VALIDATE_STATUS_ARB) == GL11.GL_FALSE) {
            throw new Exception("Error validating: " + getLogInfo(program));
        }
    }

    private static String getLogInfo(int obj) {
        return glGetInfoLogARB(obj, glGetObjectParameteriARB(obj, GL_OBJECT_INFO_LOG_LENGTH_ARB));
    }

    private int createShader(ResourceLocation resourceLocation, int shaderType) throws Exception {
        int shader = 0;
        try {
            shader = glCreateShaderObjectARB(shaderType);

            if (shader == 0)
                throw new Exception("glCreateShaderObjectARB failed");

            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(resourceLocation);
            try (InputStream is = resource.getInputStream()) {
                glShaderSourceARB(shader, IOUtils.toString(is));
            }
            glCompileShaderARB(shader);

            if (glGetObjectParameteriARB(shader, GL_OBJECT_COMPILE_STATUS_ARB) == GL11.GL_FALSE)
                throw new RuntimeException("Error creating shader: " + getLogInfo(shader));

            return shader;
        } catch (Exception exc) {
            glDeleteObjectARB(shader);
            throw exc;
        }
    }

    public void use() {
        ARBShaderObjects.glUseProgramObjectARB(program);
    }

    public void stopUsing() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    public void delete() {
        ARBShaderObjects.glDeleteObjectARB(program);
    }

    public Uniform getUniformVariable(String name) {
        return new Uniform(ARBShaderObjects.glGetUniformLocationARB(program, name));
    }

    public class Uniform {
        private final int location;

        public Uniform(int location) {
            this.location = location;
        }

        public void set(boolean bool) {
            ARBShaderObjects.glUniform1iARB(location, bool ? GL11.GL_TRUE : GL11.GL_FALSE);
        }

        public void set(int integer) {
            ARBShaderObjects.glUniform1iARB(location, integer);
        }

        public void setVector(Vector2f vector) {
            ARBShaderObjects.glUniform2fARB(location, vector.x, vector.y);
        }

        public void setMatrix(FloatBuffer matrix) {
            ARBShaderObjects.glUniformMatrix4ARB(location, false, matrix);
        }
    }
}
