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
		cl_sortPassesPerTick = config.getInt("ParticleDepthSortPasses", CLIENTSIDE, 10, 0, 20, "How many bubble sort passes should be performed each tick on particles. 0=off. Clientside");
		
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
