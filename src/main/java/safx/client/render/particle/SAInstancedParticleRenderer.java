package safx.client.render.particle;

import java.nio.FloatBuffer;

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
import com.modularwarfare.client.compat.AtomicShaderCompat;
import safx.SAConfig;
import safx.client.particle.SAParticle;
import safx.client.particle.SAParticleArray;
import safx.client.particle.SAParticleParallelTick;
import safx.client.render.SARenderHelper;
import safx.client.render.SARenderHelper.RenderType;

/**
 * GPU instanced draw path: one {@code glDrawArraysInstanced} per texture bucket (batched when needed).
 */
@SideOnly(Side.CLIENT)
public final class SAInstancedParticleRenderer {

	private static final int MAX_INSTANCES_PER_BATCH = 131072;
	private static final int INITIAL_INSTANCES = 8192;

	private static final float[] QUAD_CORNERS = {
			-1.0f, -1.0f,
			-1.0f,  1.0f,
			 1.0f,  1.0f,
			 1.0f, -1.0f
	};

	private static SAInstancedParticleRenderer instance;
	private final SAInstancedParticleShader shader = new SAInstancedParticleShader();
	private FloatBuffer instanceBuffer = BufferUtils.createFloatBuffer(INITIAL_INSTANCES * SAInstancedParticleShader.INSTANCE_FLOATS);
	private float[] packScratch = new float[INITIAL_INSTANCES * SAInstancedParticleShader.INSTANCE_FLOATS];
	private SAParticle[] visibleScratch = new SAParticle[INITIAL_INSTANCES];
	private final FloatBuffer mvpBuffer = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer projectionScratch = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer modelViewScratch = BufferUtils.createFloatBuffer(16);
	private final float[] projectionMatrix = new float[16];
	private final float[] modelViewMatrix = new float[16];
	private final int[] instanceVbos = new int[] { -1, -1 };
	private final int[] instanceCapacityBytes = new int[] { 0, 0 };
	private int instanceVboSlot;
	private int quadVbo = -1;
	private boolean mvpUploaded;
	private boolean forwardLightsBound;
	private boolean instancingSupported;
	private boolean supportChecked = false;
	private int frameRestoreProgram = -1;

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

	public void beginFrame() {
		this.mvpUploaded = false;
		this.forwardLightsBound = false;
		this.frameRestoreProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
	}

	public void endFrame() {
		if (this.frameRestoreProgram >= 0) {
			GL20.glUseProgram(this.frameRestoreProgram);
			this.frameRestoreProgram = -1;
		}
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		for (int i = 0; i < 6; i++) {
			ARBInstancedArrays.glVertexAttribDivisorARB(i, 0);
			GL20.glDisableVertexAttribArray(i);
		}
	}

	public int renderInstanced(SAParticleArray particles, ResourceLocation texture, RenderType renderType,
			float partialTicks, float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ) {
		int count = particles.size();
		if (count <= 0) {
			return 0;
		}
		this.ensureMvp();
		this.ensureQuadVbo();
		boolean cull = SAConfig.cl_enableParticleFrustumCull;
		boolean additive = renderType == RenderType.ADDITIVE || renderType == RenderType.NO_Z_TEST_ADDITIVE;
		boolean useAtomicLighting = !additive && AtomicShaderCompat.isForwardLightingActive();
		boolean useLightmap = renderType == RenderType.ALPHA_SHADED && !useAtomicLighting;
		SAParticle[] data = particles.data();
		this.ensureVisibleScratch(count);
		int visible = 0;
		for (int i = 0; i < count; i++) {
			SAParticle particle = data[i];
			if (particle == null) {
				continue;
			}
			if (cull && !particle.isInCameraFrustum(partialTicks)) {
				continue;
			}
			this.visibleScratch[visible++] = particle;
		}
		if (visible <= 0) {
			return 0;
		}
		int totalPacked = 0;
		boolean drawSetup = false;
		Minecraft mc = Minecraft.getMinecraft();
		SAParticle.setPackForwardLit(useAtomicLighting);
		try {
			int offset = 0;
			while (offset < visible) {
				int batch = Math.min(visible - offset, MAX_INSTANCES_PER_BATCH);
				this.ensurePackScratch(batch * SAInstancedParticleShader.INSTANCE_FLOATS);
				this.ensureUploadBuffer(batch * SAInstancedParticleShader.INSTANCE_FLOATS);
				SAParticleParallelTick.packInstanced(this.visibleScratch, offset, batch, this.packScratch,
						partialTicks);
				if (!drawSetup) {
					drawSetup = this.beginDraw(mc, texture, renderType, rotX, rotZ, rotYZ, rotXY, rotXZ,
							useLightmap, useAtomicLighting);
					if (!drawSetup) {
						return 0;
					}
				}
				this.instanceBuffer.clear();
				this.instanceBuffer.put(this.packScratch, 0, batch * SAInstancedParticleShader.INSTANCE_FLOATS);
				this.instanceBuffer.flip();
				this.flushBatch(batch);
				totalPacked += batch;
				offset += batch;
			}
			if (drawSetup) {
				SARenderHelper.disableBlendMode(renderType);
			}
		} finally {
			SAParticle.setPackForwardLit(false);
			for (int i = 0; i < visible; i++) {
				this.visibleScratch[i] = null;
			}
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			for (int i = 0; i < 6; i++) {
				ARBInstancedArrays.glVertexAttribDivisorARB(i, 0);
				GL20.glDisableVertexAttribArray(i);
			}
		}
		return totalPacked;
	}

	private boolean beginDraw(Minecraft mc, ResourceLocation texture, RenderType renderType,
			float rotX, float rotZ, float rotYZ, float rotXY, float rotXZ,
			boolean useLightmap, boolean useAtomicLighting) {
		TextureManager textureManager = mc.getTextureManager();
		textureManager.bindTexture(texture);
		ITextureObject particleTex = textureManager.getTexture(texture);
		if (particleTex == null) {
			return false;
		}
		SARenderHelper.enableBlendMode(renderType);
		this.modelViewScratch.position(0).limit(16);
		this.mvpBuffer.position(0).limit(16);
		shader.bindUniforms(rotX, rotZ, rotYZ, rotXY, rotXZ, mvpBuffer, this.modelViewScratch, 0,
				useLightmap, useAtomicLighting);
		if (useAtomicLighting && !this.forwardLightsBound) {
			AtomicShaderCompat.applyForwardLighting(shader.getProgram());
			this.forwardLightsBound = true;
		}
		this.bindDrawTextures(mc, particleTex.getGlTextureId(), useLightmap);
		return true;
	}

	private void flushBatch(int packed) {
		this.uploadInstanceBuffer(packed);
		this.bindVertexAttributes();
		ARBDrawInstanced.glDrawArraysInstancedARB(GL11.GL_QUADS, 0, 4, packed);
	}

	private void ensureVisibleScratch(int count) {
		if (this.visibleScratch.length < count) {
			this.visibleScratch = new SAParticle[nextPow2(count)];
		}
	}

	private void ensurePackScratch(int floats) {
		if (this.packScratch.length < floats) {
			this.packScratch = new float[nextPow2(floats)];
		}
	}

	private void ensureUploadBuffer(int floats) {
		if (this.instanceBuffer.capacity() < floats) {
			this.instanceBuffer = BufferUtils.createFloatBuffer(nextPow2(floats));
		}
	}

	private void ensureMvp() {
		if (this.mvpUploaded) {
			return;
		}
		this.uploadMvp();
		this.mvpUploaded = true;
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

	private void uploadInstanceBuffer(int instanceCount) {
		int neededBytes = instanceCount * SAInstancedParticleShader.INSTANCE_BYTES;
		this.instanceVboSlot ^= 1;
		if (this.instanceVbos[this.instanceVboSlot] <= 0) {
			this.instanceVbos[this.instanceVboSlot] = GL15.glGenBuffers();
			this.instanceCapacityBytes[this.instanceVboSlot] = 0;
		}
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.instanceVbos[this.instanceVboSlot]);
		if (neededBytes > this.instanceCapacityBytes[this.instanceVboSlot]) {
			this.instanceCapacityBytes[this.instanceVboSlot] = Math.max(nextPow2(neededBytes),
					INITIAL_INSTANCES * SAInstancedParticleShader.INSTANCE_BYTES);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, (long) this.instanceCapacityBytes[this.instanceVboSlot],
					GL15.GL_STREAM_DRAW);
		}
		GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0L, this.instanceBuffer);
	}

	private static int nextPow2(int value) {
		int n = 1;
		while (n < value) {
			n <<= 1;
			if (n <= 0) {
				return value;
			}
		}
		return n;
	}

	private void bindDrawTextures(Minecraft mc, int particleTexId, boolean useLightmap) {
		GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, particleTexId);
		if (useLightmap) {
			mc.entityRenderer.enableLightmap();
			GL13.glActiveTexture(OpenGlHelper.defaultTexUnit);
		}
	}

	private void bindVertexAttributes() {
		int stride = SAInstancedParticleShader.INSTANCE_BYTES;
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.quadVbo);
		GL20.glEnableVertexAttribArray(5);
		GL20.glVertexAttribPointer(5, 2, GL11.GL_FLOAT, false, 0, 0L);
		ARBInstancedArrays.glVertexAttribDivisorARB(5, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.instanceVbos[this.instanceVboSlot]);
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
		this.modelViewScratch.position(0).limit(16);
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
		for (int i = 0; i < this.instanceVbos.length; i++) {
			if (this.instanceVbos[i] > 0) {
				GL15.glDeleteBuffers(this.instanceVbos[i]);
				this.instanceVbos[i] = -1;
			}
			this.instanceCapacityBytes[i] = 0;
		}
		if (quadVbo > 0) {
			GL15.glDeleteBuffers(quadVbo);
			quadVbo = -1;
		}
	}
}
