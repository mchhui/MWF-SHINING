 package mchhui.hebridge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class HEBridge {
    public static void init() {
        if(FMLCommonHandler.instance().getSide()==Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new HEBridgeClient());  
        }
        MinecraftForge.EVENT_BUS.register(new HEBridgeServer());
    }
}
