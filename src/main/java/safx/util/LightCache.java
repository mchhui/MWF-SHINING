package safx.util;

import java.util.HashMap;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.SAConfig;

/**
 * Block-position light cache for ALPHA_SHADED particles.
 * Deduplicates {@code World.getCombinedLight} within a single render frame.
 */
@SideOnly(Side.CLIENT)
public final class LightCache {

	private static final ThreadLocal<BlockPos.MutableBlockPos> MUTABLE_POS =
			ThreadLocal.withInitial(() -> new BlockPos.MutableBlockPos(0, 0, 0));

	private static final HashMap<Long, Integer> CACHE = new HashMap<>(2048);

	private LightCache() {
	}

	public static void beginFrame() {
		if (!SAConfig.cl_enableLightCache) {
			return;
		}
		CACHE.clear();
	}

	public static int getPackedLight(World world, double x, double y, double z) {
		if (world == null) {
			return 0;
		}
		int xi = MathHelper.floor(x);
		int yi = MathHelper.floor(y);
		int zi = MathHelper.floor(z);
		if (!SAConfig.cl_enableLightCache) {
			return queryLight(world, xi, yi, zi);
		}
		BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
		pos.setPos(xi, yi, zi);
		long key = pos.toLong();
		Integer cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		int light = queryLight(world, xi, yi, zi);
		CACHE.put(key, light);
		return light;
	}

	private static int queryLight(World world, int x, int y, int z) {
		BlockPos.MutableBlockPos pos = MUTABLE_POS.get();
		pos.setPos(x, y, z);
		if (!world.isBlockLoaded(pos)) {
			return 0;
		}
		return world.getCombinedLight(pos, 0);
	}
}
