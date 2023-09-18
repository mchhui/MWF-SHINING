package mchhui.hegltf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class ShaderGltf {
    public static final int JOINTMATSBUFFERBINDING=0;
    public static final int VERTEXBUFFERBINDING=3;
    
    public static int shaderProgram = -1;
    public static int vshaderId = -1;

    public static void useShader() {

        if (shaderProgram != -1) {
            GL20.glUseProgram(shaderProgram);
            return;
        }

        String vshaderCode = readText();
        vshaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vshaderId, vshaderCode);
        GL20.glCompileShader(vshaderId);
        if (GL20.glGetShaderi(vshaderId, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetShaderInfoLog(vshaderId, Short.MAX_VALUE));
            throw new RuntimeException();
        }

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vshaderId);
        
        GL30.glTransformFeedbackVaryings(shaderProgram, new CharSequence[] {"outPos", "outNormal", "outTex"},
            GL30.GL_SEPARATE_ATTRIBS);
        GL20.glLinkProgram(shaderProgram);
        GL20.glDeleteShader(vshaderId);
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetProgramInfoLog(shaderProgram, Short.MAX_VALUE));
            throw new RuntimeException();
        }
    }
    
    @Nullable
    public static String readText() {
        return "#version 430\r\n"
            + "\r\n"
            + "layout (location = 0) in vec3 v_pos;\r\n"
            + "layout (location = 1) in vec2 v_tex;\r\n"
            + "layout (location = 2) in vec3 v_normal;\r\n"
            + "layout (location = 3) in vec4 v_joint;\r\n"
            + "layout (location = 4) in vec4 v_weight;\r\n"
            + "layout (location = 5) in float v_count;\r\n"
            + "\r\n"
            + "\r\n"
            + "layout (std430,binding = 0) buffer JointMatsBuffer {\r\n"
            + "    readonly mat4 joint_mats[];\r\n"
            + "};\r\n"
            + "\r\n"
            + "layout (std430,binding = 3) buffer VertexBuffer {\r\n"
            + "    writeonly float v_buffer[];\r\n"
            + "};\r\n"
            + "\r\n"
            + "out vec3 outPos;\r\n"
            + "out vec3 outNormal;\r\n"
            + "out vec2 outTex;\r\n"
            + "\r\n"
            + "void main() {\r\n"
            + "    mat4 skinMatrix = v_weight.x * joint_mats[floatBitsToUint(v_joint.x)]\r\n"
            + "    +v_weight.y * joint_mats[floatBitsToUint(v_joint.y)]\r\n"
            + "    +v_weight.z * joint_mats[floatBitsToUint(v_joint.z)]\r\n"
            + "    +v_weight.w * joint_mats[floatBitsToUint(v_joint.w)];\r\n"
            + "    mat3 matrix3d = mat3(skinMatrix);\r\n"
            + "\r\n"
            + "    outPos = (skinMatrix * vec4(v_pos, 1.0)).xyz;\r\n"
            + "    outNormal = matrix3d * v_normal;\r\n"
            + "    outTex = v_tex;\r\n"
            + "    uint v_offset=floatBitsToUint(v_count)*8;\r\n"
            + "    v_buffer[(v_offset+0)]=outPos.x;\r\n"
            + "    v_buffer[(v_offset+1)]=outPos.y;\r\n"
            + "    v_buffer[(v_offset+2)]=outPos.z;\r\n"
            + "    v_buffer[(v_offset+3)]=outNormal.x;\r\n"
            + "    v_buffer[(v_offset+4)]=outNormal.y;\r\n"
            + "    v_buffer[(v_offset+5)]=outNormal.z;\r\n"
            + "    v_buffer[(v_offset+6)]=outTex.x;\r\n"
            + "    v_buffer[(v_offset+7)]=outTex.y;\r\n"
            + "    \r\n"
            + "    gl_Position = vec4(outPos.x, outPos.y, outPos.z, 1);\r\n"
            + "}";
    }
}
