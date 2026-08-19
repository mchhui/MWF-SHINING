package safx.client.render.particle;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.util.SALogger;

/**
 * GLSL instanced particle shader (MadParticle-style VS + Atomic forward lighting FS).
 */
@SideOnly(Side.CLIENT)
public final class SAInstancedParticleShader {

	private static final int SHADER_REVISION = 7;

	public static final int INSTANCE_FLOATS = 20;
	public static final int INSTANCE_BYTES = INSTANCE_FLOATS * 4;

	private static final String VERT_SRC =
			"#version 330 compatibility\n" +
			"uniform float sa_RotX;\n" +
			"uniform float sa_RotZ;\n" +
			"uniform float sa_RotYZ;\n" +
			"uniform float sa_RotXY;\n" +
			"uniform float sa_RotXZ;\n" +
			"uniform mat4 sa_MVP;\n" +
			"uniform mat4 sa_ModelView;\n" +
			"layout(location = 0) in vec4 sa_Instance0;\n" +
			"layout(location = 1) in vec4 sa_Instance1;\n" +
			"layout(location = 2) in vec4 sa_Instance2;\n" +
			"layout(location = 3) in vec4 sa_Instance3;\n" +
			"layout(location = 4) in vec4 sa_Instance4;\n" +
			"layout(location = 5) in vec2 sa_Corner;\n" +
			"varying vec2 sa_TexCoord;\n" +
			"varying vec4 sa_VertexColor;\n" +
			"varying vec2 sa_LightmapCoord;\n" +
			"varying vec2 sa_LightVis;\n" +
			"varying vec3 sa_ViewPos;\n" +
			"vec3 saWallOffset(vec3 normal, float sx, float sy, float scaleX, float scaleY, float angle) {\n" +
			"  vec3 n = normal;\n" +
			"  float nLen = length(n);\n" +
			"  if (nLen > 1.0e-6) n /= nLen;\n" +
			"  vec3 h = (abs(n.y) > 0.9) ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0);\n" +
			"  vec3 t = cross(h, n);\n" +
			"  float tLen = length(t);\n" +
			"  if (tLen > 1.0e-6) t /= tLen;\n" +
			"  vec3 b = cross(n, t);\n" +
			"  float bLen = length(b);\n" +
			"  if (bLen > 1.0e-6) b /= bLen;\n" +
			"  float ca = cos(angle);\n" +
			"  float si = sin(angle);\n" +
			"  vec3 t2 = t * ca + b * si;\n" +
			"  vec3 b2 = b * ca - t * si;\n" +
			"  return t2 * (sx * scaleX) + b2 * (sy * scaleY);\n" +
			"}\n" +
			"void main() {\n" +
			"  vec3 pos = sa_Instance0.xyz;\n" +
			"  float mode = sa_Instance0.w;\n" +
			"  float scaleX = sa_Instance1.x;\n" +
			"  float scaleY = sa_Instance1.y;\n" +
			"  vec4 uv = vec4(sa_Instance1.z, sa_Instance1.w, sa_Instance2.x, sa_Instance2.y);\n" +
			"  sa_VertexColor = vec4(sa_Instance2.z, sa_Instance2.w, sa_Instance3.x, sa_Instance3.y);\n" +
			"  float lmSky = sa_Instance3.z;\n" +
			"  float lmBlock = sa_Instance3.w;\n" +
			"  sa_LightmapCoord = vec2(lmBlock + 8.0, lmSky + 8.0) * 0.00390625;\n" +
			"  sa_LightVis = clamp(vec2(lmSky, lmBlock) / 240.0, 0.0, 1.0);\n" +
			"  vec3 normal = sa_Instance4.xyz;\n" +
			"  float angle = sa_Instance4.w;\n" +
			"  float sx = sa_Corner.x;\n" +
			"  float sy = sa_Corner.y;\n" +
			"  vec3 offset;\n" +
			"  if (mode < 0.5) {\n" +
			"    offset.x = sa_RotX * scaleX * sx + sa_RotXY * scaleY * sy;\n" +
			"    offset.y = sa_RotZ * scaleY * sy;\n" +
			"    offset.z = sa_RotYZ * scaleX * sx + sa_RotXZ * scaleY * sy;\n" +
			"    if (abs(angle) > 0.001) {\n" +
			"      vec3 right = normalize(vec3(sa_RotX, 0.0, sa_RotYZ));\n" +
			"      vec3 up = vec3(sa_RotXY, sa_RotZ, sa_RotXZ);\n" +
			"      float ox = dot(offset, right);\n" +
			"      float oy = dot(offset, up);\n" +
			"      float ca = cos(angle);\n" +
			"      float si = sin(angle);\n" +
			"      offset = right * (ox * ca - oy * si) + up * (ox * si + oy * ca);\n" +
			"    }\n" +
			"  } else if (mode < 1.5) {\n" +
			"    float lx = sx * scaleX;\n" +
			"    float lz = sy * scaleY;\n" +
			"    float ca = cos(angle);\n" +
			"    float si = sin(angle);\n" +
			"    offset.x = lx * ca - lz * si;\n" +
			"    offset.y = 0.0;\n" +
			"    offset.z = lx * si + lz * ca;\n" +
			"  } else {\n" +
			"    offset = saWallOffset(normal, sx, sy, scaleX, scaleY, angle);\n" +
			"  }\n" +
			"  vec4 world = vec4(pos + offset, 1.0);\n" +
			"  sa_ViewPos = (sa_ModelView * world).xyz;\n" +
			"  gl_Position = sa_MVP * world;\n" +
			"  float texU = (sx > 0.0) ? uv.x : uv.y;\n" +
			"  float texV = (sy > 0.0) ? uv.z : uv.w;\n" +
			"  sa_TexCoord = vec2(texU, texV);\n" +
			"}\n";

	private static final String FRAG_SRC =
			"#version 330 compatibility\n" +
			"uniform sampler2D sa_Texture;\n" +
			"uniform sampler2D sa_Lightmap;\n" +
			"uniform int sa_UseLightmap;\n" +
			"uniform int sa_UseAtomicLighting;\n" +
			"uniform vec3 uAmbient;\n" +
			"uniform vec3 uSunColor;\n" +
			"uniform int uLightCount;\n" +
			"uniform int uLightKind[8];\n" +
			"uniform vec3 uLightPos[8];\n" +
			"uniform vec3 uLightColor[8];\n" +
			"uniform float uLightRange[8];\n" +
			"uniform vec3 uLightDir[8];\n" +
			"uniform float uLightCosInner[8];\n" +
			"uniform float uLightCosOuter[8];\n" +
			"uniform vec2 uLightHalfSize[8];\n" +
			"varying vec2 sa_TexCoord;\n" +
			"varying vec4 sa_VertexColor;\n" +
			"varying vec2 sa_LightmapCoord;\n" +
			"varying vec2 sa_LightVis;\n" +
			"varying vec3 sa_ViewPos;\n" +
			"float pointAtten(float dist, float range) {\n" +
			"  float attenuation = 1.0 / (1.0 + 0.22 * dist + 0.20 * dist * dist);\n" +
			"  float rangeFade = 1.0 - smoothstep(range * 0.75, range, dist);\n" +
			"  return attenuation * rangeFade;\n" +
			"}\n" +
			"vec3 evalCustomLight(int i, vec3 fragPos) {\n" +
			"  int kind = uLightKind[i];\n" +
			"  vec3 color = uLightColor[i];\n" +
			"  float range = max(uLightRange[i], 0.01);\n" +
			"  if (kind == 0) {\n" +
			"    vec3 toLight = uLightPos[i] - fragPos;\n" +
			"    float dist = length(toLight);\n" +
			"    if (dist >= range) return vec3(0.0);\n" +
			"    return color * pointAtten(dist, range);\n" +
			"  }\n" +
			"  if (kind == 1) {\n" +
			"    vec3 toLight = uLightPos[i] - fragPos;\n" +
			"    float dist = length(toLight);\n" +
			"    if (dist >= range) return vec3(0.0);\n" +
			"    vec3 L = toLight / max(dist, 1e-5);\n" +
			"    float cosTheta = dot(-L, normalize(uLightDir[i]));\n" +
			"    float spot = smoothstep(uLightCosOuter[i], uLightCosInner[i], cosTheta);\n" +
			"    return color * pointAtten(dist, range) * spot;\n" +
			"  }\n" +
			"  vec3 toCenter = uLightPos[i] - fragPos;\n" +
			"  float dist = length(toCenter);\n" +
			"  if (dist >= range) return vec3(0.0);\n" +
			"  vec3 N = normalize(uLightDir[i]);\n" +
			"  vec3 fromLight = -toCenter / max(dist, 1e-5);\n" +
			"  float facing = max(dot(fromLight, N), 0.0);\n" +
			"  vec3 toPlane = toCenter - N * dot(toCenter, N);\n" +
			"  vec2 halfSize = max(uLightHalfSize[i], vec2(0.01));\n" +
			"  vec3 up = abs(N.y) < 0.99 ? vec3(0.0, 1.0, 0.0) : vec3(1.0, 0.0, 0.0);\n" +
			"  vec3 T = normalize(cross(up, N));\n" +
			"  vec3 B = cross(N, T);\n" +
			"  float u = abs(dot(toPlane, T)) / halfSize.x;\n" +
			"  float v = abs(dot(toPlane, B)) / halfSize.y;\n" +
			"  float rect = (1.0 - smoothstep(1.0, 1.35, u)) * (1.0 - smoothstep(1.0, 1.35, v));\n" +
			"  return color * pointAtten(dist, range) * (0.35 + 0.65 * facing) * max(rect, 0.15);\n" +
			"}\n" +
			"void main() {\n" +
			"  vec4 tex = texture2D(sa_Texture, sa_TexCoord);\n" +
			"  vec4 color = tex * sa_VertexColor;\n" +
			"  if (color.a < 0.001) discard;\n" +
			"  if (sa_UseAtomicLighting != 0) {\n" +
			"    float skyVis = sa_LightVis.x;\n" +
			"    float blockVis = sa_LightVis.y;\n" +
			"    vec3 L_sky = uAmbient + uSunColor * skyVis;\n" +
			"    vec3 L_block = vec3(0.12, 0.11, 0.10) * blockVis;\n" +
			"    vec3 L_custom = vec3(0.0);\n" +
			"    for (int i = 0; i < 8; i++) {\n" +
			"      if (i >= uLightCount) break;\n" +
			"      L_custom += evalCustomLight(i, sa_ViewPos);\n" +
			"    }\n" +
			"    color.rgb *= (L_sky + L_block + L_custom);\n" +
			"  } else if (sa_UseLightmap != 0) {\n" +
			"    color *= texture2D(sa_Lightmap, sa_LightmapCoord);\n" +
			"  }\n" +
			"  gl_FragColor = color;\n" +
			"}\n";

	private int program = -1;
	private int uMvp = -1;
	private int uModelView = -1;
	private int uRotX = -1;
	private int uRotZ = -1;
	private int uRotYZ = -1;
	private int uRotXY = -1;
	private int uRotXZ = -1;
	private int uTexture = -1;
	private int uLightmap = -1;
	private int uUseLightmap = -1;
	private int uUseAtomicLighting = -1;
	private boolean initFailed = false;
	private int loadedRevision = -1;

	public boolean ensureReady() {
		if (program > 0 && this.loadedRevision == SHADER_REVISION) {
			return true;
		}
		if (program > 0) {
			this.dispose();
		}
		if (initFailed) {
			return false;
		}
		int vert = compile(GL20.GL_VERTEX_SHADER, VERT_SRC);
		if (vert <= 0) {
			initFailed = true;
			return false;
		}
		int frag = compile(GL20.GL_FRAGMENT_SHADER, FRAG_SRC);
		if (frag <= 0) {
			GL20.glDeleteShader(vert);
			initFailed = true;
			return false;
		}
		int prog = GL20.glCreateProgram();
		GL20.glAttachShader(prog, vert);
		GL20.glAttachShader(prog, frag);
		GL20.glBindAttribLocation(prog, 0, "sa_Instance0");
		GL20.glBindAttribLocation(prog, 1, "sa_Instance1");
		GL20.glBindAttribLocation(prog, 2, "sa_Instance2");
		GL20.glBindAttribLocation(prog, 3, "sa_Instance3");
		GL20.glBindAttribLocation(prog, 4, "sa_Instance4");
		GL20.glBindAttribLocation(prog, 5, "sa_Corner");
		GL20.glLinkProgram(prog);
		GL20.glDeleteShader(vert);
		GL20.glDeleteShader(frag);
		if (GL20.glGetProgrami(prog, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			SALogger.logger_client.warning("[SAFX] instanced particle shader link failed: "
					+ GL20.glGetProgramInfoLog(prog, 1024));
			GL20.glDeleteProgram(prog);
			initFailed = true;
			return false;
		}
		program = prog;
		uMvp = GL20.glGetUniformLocation(program, "sa_MVP");
		uModelView = GL20.glGetUniformLocation(program, "sa_ModelView");
		uRotX = GL20.glGetUniformLocation(program, "sa_RotX");
		uRotZ = GL20.glGetUniformLocation(program, "sa_RotZ");
		uRotYZ = GL20.glGetUniformLocation(program, "sa_RotYZ");
		uRotXY = GL20.glGetUniformLocation(program, "sa_RotXY");
		uRotXZ = GL20.glGetUniformLocation(program, "sa_RotXZ");
		uTexture = GL20.glGetUniformLocation(program, "sa_Texture");
		uLightmap = GL20.glGetUniformLocation(program, "sa_Lightmap");
		uUseLightmap = GL20.glGetUniformLocation(program, "sa_UseLightmap");
		uUseAtomicLighting = GL20.glGetUniformLocation(program, "sa_UseAtomicLighting");
		this.loadedRevision = SHADER_REVISION;
		SALogger.logger_client.info("[SAFX] instanced particle shader ready (program=" + program + ")");
		return true;
	}

	public int getProgram() {
		return program;
	}

	public void bindUniforms(float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ,
			java.nio.FloatBuffer mvp, java.nio.FloatBuffer modelView, int textureUnit,
			boolean useLightmap, boolean useAtomicLighting) {
		GL20.glUseProgram(program);
		if (mvp != null) {
			mvp.position(0);
		}
		if (modelView != null) {
			modelView.position(0);
		}
		if (uRotX >= 0) GL20.glUniform1f(uRotX, rotX);
		if (uRotZ >= 0) GL20.glUniform1f(uRotZ, rotZ);
		if (uRotYZ >= 0) GL20.glUniform1f(uRotYZ, rotYZ);
		if (uRotXY >= 0) GL20.glUniform1f(uRotXY, rotXY);
		if (uRotXZ >= 0) GL20.glUniform1f(uRotXZ, rotXZ);
		if (uMvp >= 0) GL20.glUniformMatrix4(uMvp, false, mvp);
		if (uModelView >= 0 && modelView != null) {
			GL20.glUniformMatrix4(uModelView, false, modelView);
		}
		if (uTexture >= 0) GL20.glUniform1i(uTexture, textureUnit);
		if (uLightmap >= 0) GL20.glUniform1i(uLightmap, OpenGlHelper.lightmapTexUnit - GL13.GL_TEXTURE0);
		if (uUseLightmap >= 0) GL20.glUniform1i(uUseLightmap, useLightmap ? 1 : 0);
		if (uUseAtomicLighting >= 0) GL20.glUniform1i(uUseAtomicLighting, useAtomicLighting ? 1 : 0);
	}

	public void dispose() {
		if (program > 0) {
			GL20.glDeleteProgram(program);
			program = -1;
		}
		this.loadedRevision = -1;
		initFailed = false;
	}

	private static int compile(int type, String src) {
		int id = GL20.glCreateShader(type);
		GL20.glShaderSource(id, src);
		GL20.glCompileShader(id);
		if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			SALogger.logger_client.warning("[SAFX] instanced particle shader compile failed: "
					+ GL20.glGetShaderInfoLog(id, 1024));
			GL20.glDeleteShader(id);
			return -1;
		}
		return id;
	}
}
