package safx.client.render.particle;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBDrawInstanced;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.SAConfig;
import safx.client.particle.SAParticle;
import safx.client.render.SARenderHelper;
import safx.client.render.SARenderHelper.RenderType;

/**
 * GPU instanced draw path: one {@code glDrawArraysInstanced} per texture bucket (batched when needed).
 */
@SideOnly(Side.CLIENT)
public final class SAInstancedParticleRenderer {

	private static final int MAX_INSTANCES_PER_BATCH = 8192;

	/** Matches CPU writeQuad vertex order: v0..v3. */
	private static final float[] QUAD_CORNERS = {
			-1.0f, -1.0f,
			-1.0f,  1.0f,
			 1.0f,  1.0f,
			 1.0f, -1.0f
	};

	private static SAInstancedParticleRenderer instance;
	private final SAInstancedParticleShader shader = new SAInstancedParticleShader();
	private final FloatBuffer instanceBuffer = BufferUtils.createFloatBuffer(MAX_INSTANCES_PER_BATCH * SAInstancedParticleShader.INSTANCE_FLOATS);
	private final FloatBuffer mvpBuffer = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer projectionScratch = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer modelViewScratch = BufferUtils.createFloatBuffer(16);
	private final float[] projectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16];
	private int instanceVbo = -1;
	private int quadVbo = -1;
	private int instanceCapacity = 0;
	private boolean instancingSupported;
	private boolean supportChecked = false;

	public static SAInstancedParticleRenderer get() {
		if (instance == null) {
			instance = new SAInstancedParticleRenderer();
		}
		return instance;
	}

	public boolean isAvailable() {
		if (!SAConfig.cl_enableInstancedParticles) {
			return false;
		}
		if (!supportChecked) {
			ContextCapabilities caps = GLContext.getCapabilities();
			instancingSupported = caps.GL_ARB_draw_instanced && caps.GL_ARB_instanced_arrays;
			supportChecked = true;
			if (!instancingSupported) {
				safx.util.SALogger.logger_client.info("[SAFX] GL instancing not supported, using CPU particle path.");
			}
		}
		return instancingSupported && shader.ensureReady();
	}

	public int renderInstanced(List<SAParticle> particles, ResourceLocation texture, RenderType renderType,
			float partialTicks, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
		if (particles.isEmpty()) {
			return 0;
		}
		Minecraft mc = Minecraft.getMinecraft();
		TextureManager textureManager = mc.getTextureManager();
		textureManager.bindTexture(texture);
		ITextureObject particleTex = textureManager.getTexture(texture);
		if (particleTex == null) {
			return 0;
		}
		this.uploadMvp();
		this.ensureQuadVbo();
		int prevProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		boolean useLightmap = renderType == RenderType.ALPHA_SHADED;
		int totalPacked = 0;
		int index = 0;
		try {
			SARenderHelper.enableBlendMode(renderType);
			shader.bindUniforms(rotX, rotZ, rotYZ, rotXY, rotXZ, mvpBuffer, 0, useLightmap);
			this.bindDrawTextures(mc, particleTex.getGlTextureId(), useLightmap);
			while (index < particles.size()) {
				instanceBuffer.clear();
				int packed = 0;
				while (index < particles.size() && packed < MAX_INSTANCES_PER_BATCH) {
					if (instanceBuffer.remaining() < SAInstancedParticleShader.INSTANCE_FLOATS) {
						break;
					}
					SAParticle particle = particles.get(index++);
					if (particle.canUseInstancedRender() && particle.packInstanced(instanceBuffer, partialTicks)) {
						packed++;
					}
				}
				if (packed == 0) {
					break;
				}
				instanceBuffer.flip();
				this.ensureInstanceVbo(packed);
				GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, instanceVbo);
				GL15.glBufferData(GL15.GL_ARRAY_BUFFER, instanceBuffer, GL15.GL_STREAM_DRAW);
				this.bindVertexAttributes();
				ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, 4, packed);
				totalPacked += packed;
			}
			SARenderHelper.disableBlendMode(renderType);
		} finally {
			GL20.glUseProgram(prevProgram);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			for (int i = 0; i < 6; i++) {
				ARBInstancedArrays.glVertexAttribDivisorARB(i, 0);
				GL20.glDisableVertexAttribArray(i);
			}
		}
		return totalPacked;
	}

	private void ensureQuadVbo() {
		if (this.quadVbo > 0) {
			return;
		}
		this.quadVbo = GL15.glGenBuffers();
		FloatBuffer cornerBuffer = BufferUtils.createFloatBuffer(QUAD_CORNERS.length);
		cornerBuffer.put(QUAD_CORNERS);
		cornerBuffer.flip();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.quadVbo);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, cornerBuffer, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
	}

	private void ensureInstanceVbo(int instanceCount) {
		int bytes = Math.max(instanceCount, 256) * SAInstancedParticleShader.INSTANCE_BYTES;
		if (instanceVbo <= 0) {
			instanceVbo = GL15.glGenBuffers();
			instanceCapacity = bytes;
			return;
		}
		if (bytes > instanceCapacity) {
			instanceCapacity = bytes;
		}
	}

	private void bindDrawTextures(Minecraft mc, int particleTexId, boolean useLightmap) {
		GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, particleTexId);
		if (!useLightmap) {
			return;
		}
		mc.entityRenderer.enableLightmap();
		GL13.glActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		int lightmapTexId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		if (lightmapTexId <= 0) {
			mc.entityRenderer.enableLightmap();
			lightmapTexId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
		}
		if (lightmapTexId > 0) {
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightmapTexId);
		}
		GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	private void bindVertexAttributes() {
		int stride = SAInstancedParticleShader.INSTANCE_BYTES;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.quadVbo);
		GL20.glEnableVertexAttribArray(5);
		GL20.glVertexAttribPointer(5, 2, GL11.GL_FLOAT, false, 0, 0L);
		ARBInstancedArrays.glVertexAttribDivisorARB(5, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.instanceVbo);
		GL20.glEnableVertexAttribArray(0);
		GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, stride, 0L);
		ARBInstancedArrays.glVertexAttribDivisorARB(0, 1);
		GL20.glEnableVertexAttribArray(1);
		GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, stride, 16L);
		ARBInstancedArrays.glVertexAttribDivisorARB(1, 1);
		GL20.glEnableVertexAttribArray(2);
		GL20.glVertexAttribPointer(2, 4, GL11.GL_FLOAT, false, stride, 32L);
		ARBInstancedArrays.glVertexAttribDivisorARB(2, 1);
		GL20.glEnableVertexAttribArray(3);
		GL20.glVertexAttribPointer(3, 4, GL11.GL_FLOAT, false, stride, 48L);
		ARBInstancedArrays.glVertexAttribDivisorARB(3, 1);
		GL20.glEnableVertexAttribArray(4);
		GL20.glVertexAttribPointer(4, 4, GL11.GL_FLOAT, false, stride, 64L);
		ARBInstancedArrays.glVertexAttribDivisorARB(4, 1);
	}

	private void uploadMvp() {
		this.projectionScratch.clear();
		this.modelViewScratch.clear();
		GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, this.projectionScratch);
		GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.modelViewScratch);
		for (int i = 0; i < 16; i++) {
			this.projectionMatrix[i] = this.projectionScratch.get(i);
			this.modelViewMatrix[i] = this.modelViewScratch.get(i);
		}
		this.mvpBuffer.clear();
		this.multiplyMat4ColMajor(this.projectionMatrix, this.modelViewMatrix, this.mvpBuffer);
		this.mvpBuffer.position(0).limit(16);
	}

	private void multiplyMat4ColMajor(float[] projection, float[] modelView, FloatBuffer out) {
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				float sum = 0.0F;
				for (int k = 0; k < 4; k++) {
					sum += projection[row + k * 4] * modelView[k + col * 4];
				}
				out.put(row + col * 4, sum);
			}
		}
	}

	public void dispose() {
		shader.dispose();
		if (instanceVbo > 0) {
			GL15.glDeleteBuffers(instanceVbo);
			instanceVbo = -1;
		}
		if (quadVbo > 0) {
			GL15.glDeleteBuffers(quadVbo);
			quadVbo = -1;
		}
	}
}
