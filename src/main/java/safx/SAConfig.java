package safx;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = SagerFX.MODID)
public class SAConfig {
	public static Configuration config;
	
	public static boolean debug;

	public static boolean cl_enableDeathFX;
	public static boolean cl_enableDeathFX_Gore;
	public static int cl_sortPassesPerTick;
	public static boolean cl_enableLightCache;
	public static boolean cl_enableInstancedParticles;
	public static boolean cl_asyncParticleTick;
	public static int cl_asyncParticleTickMinCount;
	public static int cl_particleSortMode;
	public static int cl_particleSortLimit;
	public static boolean cl_asyncParticlePack;
	public static int cl_asyncParticlePackMinCount;
	public static int cl_collisionParticleLimit;
	public static boolean cl_enableParticleFrustumCull;

	public static final int SORT_NONE = 0;
	public static final int SORT_PARTIAL = 1;
	public static final int SORT_FULL = 2;
	/**
	 * CATEGORIES
	 */
	private static final String CATEGORY_ENABLING_ITEMS = "Disable Items";
	
	public static final String CLIENTSIDE = "Clientside";
	private static final String ID_CONFLICTS = "ID Conflicts";
	private static final String WORLDGEN="World Generation";
	private static final String DAMAGE_FACTORS="Damage Factors";
	private static final String ORE_DRILLS = "Ore Drills";
	

	public static void init(FMLPreInitializationEvent event){
		//Load the config file
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		
		initValues();
	}
	
	public static void initValues() {
		config.addCustomCategoryComment(CLIENTSIDE, "Clientside options, can be changed when playing on a server");
		
		debug = config.getBoolean("debug", config.CATEGORY_GENERAL, false, "Enable debug options and unfinished stuff, disable this for playing.");
		
		cl_enableDeathFX = config.getBoolean("EnableDeathEffects", CLIENTSIDE, true, "Enable Death Effects, pure clientside check.");
		cl_enableDeathFX_Gore = config.getBoolean("EnableGoreDeathEffect", CLIENTSIDE, true, "Enable the gore Death Effect, requires DeathEffects to be enabled, pure clientside check.");
		cl_sortPassesPerTick = config.getInt("ParticleDepthSortPasses", CLIENTSIDE, 0, 0, 20, "Deprecated: depth sorting is now done per texture bucket at render time. Leave at 0.");
		cl_enableLightCache = config.getBoolean("EnableParticleLightCache", CLIENTSIDE, true, "Cache block light lookups for ALPHA_SHADED particles (same block shares one query per frame). Clientside.");
		cl_enableInstancedParticles = config.getBoolean("EnableInstancedParticleRendering", CLIENTSIDE, true, "Use GPU instanced draw for camera-facing particles (requires GL_ARB_draw_instanced). Clientside.");
		cl_asyncParticleTick = config.getBoolean("EnableAsyncParticleTick", CLIENTSIDE, true, "Tick independent SAFX particles on worker threads (MadParticle-style). Stick/collision/streak stay on the client thread.");
		cl_asyncParticleTickMinCount = config.getInt("AsyncParticleTickMinCount", CLIENTSIDE, 64, 8, 4096, "Minimum live particles before async tick is used.");
		cl_particleSortMode = config.getInt("ParticleSortMode", CLIENTSIDE, 1, 0, 2, "Depth sort: 0=NONE, 1=PARTIAL (default, sort buckets up to ParticleSortLimit), 2=FULL.");
		cl_particleSortLimit = config.getInt("ParticleSortLimit", CLIENTSIDE, 2048, 64, 100000, "PARTIAL: skip depth sort when a bucket has more particles than this.");
		cl_asyncParticlePack = config.getBoolean("EnableAsyncParticlePack", CLIENTSIDE, true, "Fill instanced particle buffers on worker threads.");
		cl_asyncParticlePackMinCount = config.getInt("AsyncParticlePackMinCount", CLIENTSIDE, 512, 64, 65536, "Minimum particles in a bucket before async pack is used.");
		cl_collisionParticleLimit = config.getInt("CollisionParticleLimit", CLIENTSIDE, 64, 0, 65536, "Max live particles that run block-hit raytraces. 0=unlimited. Oldest over the cap keep rendering but skip collision.");
		cl_enableParticleFrustumCull = config.getBoolean("EnableParticleFrustumCull", CLIENTSIDE, true, "Skip packing/uploading particles whose world AABB is outside the camera frustum (HE-style vanilla Frustum). Clientside.");
		
		if(config.hasChanged()) {
			config.save();
		}
	}
	
	@SubscribeEvent
	public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event){
		if(event.getModID().equalsIgnoreCase(SagerFX.MODID))
		{
			initValues();
		}
	}

}
