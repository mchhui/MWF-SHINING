package mchhui.modularmovements;

import com.modularwarfare.ModularWarfare;
import mchhui.modularmovements.network.Handler;
import mchhui.modularmovements.tactical.client.ClientLitener;
import mchhui.modularmovements.tactical.client.MWFClientListener;
import mchhui.modularmovements.tactical.server.MWFServerListener;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = ModularMovements.MOD_ID)
public class ModularMovements {
    public static final String MOD_ID = "modularmovements";
    public static final String MOD_NAME = "ModularMovements";
    public static final String MOD_VERSION = "1.0.0f";
    public static boolean enableTactical = true;
    @SideOnly(Side.CLIENT)
    public static ClientLitener TacticalClientListener;
    public static ServerListener TacticalServerListener = new ServerListener();
    public static FMLEventChannel channel;
    public static boolean mwfEnable = false;
    @Mod.Instance("modularmovements")
    public static ModularMovements INSTANCE;

    public static File MOD_DIR;

    // Logger
    public static Logger LOGGER;

    public static ModularMovementsConfig CONFIG;


    @EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        // Creates directory if doesn't exist
        MOD_DIR = new File(event.getModConfigurationDirectory().getParentFile(), "ModularMovements");
        if (!MOD_DIR.exists()) {
            MOD_DIR.mkdir();
            LOGGER.info("Created ModularMovements folder.");
        }
        new ModularMovementsConfig(new File(MOD_DIR, "mod_config.json"));
    }

    @EventHandler
    public void onInit(FMLInitializationEvent event) {

        if (Loader.isModLoaded("modularwarfare")) {
            mwfEnable = true;
        }
        mwfEnable = true;
        ModularWarfare.isLoadedModularMovements = true;
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("modularmovements");
        channel.register(new Handler());
        if (enableTactical) {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                TacticalClientListener = new ClientLitener();
                MinecraftForge.EVENT_BUS.register(TacticalClientListener);
                if (mwfEnable) {
                    MinecraftForge.EVENT_BUS.register(new MWFClientListener());
                }
                TacticalClientListener.onFMLInit(event);
            }
            TacticalServerListener.onFMLInit(event);
            MinecraftForge.EVENT_BUS.register(TacticalServerListener);
            if (mwfEnable) {
                MinecraftForge.EVENT_BUS.register(new MWFServerListener());
            }
        }
    }

    @EventHandler
    public void onInitPost(FMLPostInitializationEvent event) {
        if (enableTactical) {
            if (FMLCommonHandler.instance().getSide().isClient()) {
                TacticalClientListener.onFMLInitPost(event);
            }
        }
    }
}
