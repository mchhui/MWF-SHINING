package safx;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import safx.init.ISAInitializer;

@Mod(modid = SagerFX.MODID, version = SagerFX.VERSION, name=SagerFX.NAME, acceptedMinecraftVersions=SagerFX.MCVERSION, /*guiFactory=SagerFX.GUI_FACTORY, updateJSON=SagerFX.UPDATEURL,*/ dependencies=SagerFX.DEPENDENCIES)
public class SagerFX
{
    public static final String MODID = "safx";
    public static final String MCVERSION = "1.12.2";
    public static final String VERSION = "1.0.0.0";
    public static final String NAME = "SagerFX";
    public static final String FORGE_BUILD = "14.23.5.2860";
    public static final String DEPENDENCIES = "required:forge@["+FORGE_BUILD+",)";
    
    @Mod.Instance
    public static SagerFX instance;
    
    @SidedProxy(clientSide = "safx.client.ClientProxy", serverSide = "safx.server.ServerProxy")
    public static CommonProxy proxy;
    
    public SAPackets packets = new SAPackets();
 
    //Mod integration
    public boolean FTBLIB_ENABLED=false;
    public boolean CHISEL_ENABLED=false;
    
    protected ISAInitializer[] initializers = {
    	packets
    };
	
	public static int modEntityID=-1;
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	for (ISAInitializer init : initializers){
    		init.init(event);
    	}
    	proxy.init(event);
    }
    
    @EventHandler
    public void preinit(FMLPreInitializationEvent event)
    {
    	SAConfig.init(event);
    	for (ISAInitializer init : initializers){
    		init.preInit(event);
    	}
    	proxy.preInit(event);
    }
    
    @EventHandler
    public void postinit(FMLPostInitializationEvent event)
    {
    	for (ISAInitializer init : initializers){
    		init.postInit(event);
    	}
    	proxy.postInit(event);
    }
}