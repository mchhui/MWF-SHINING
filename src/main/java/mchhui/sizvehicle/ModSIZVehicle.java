package mchhui.sizvehicle;

import mchhui.sizvehicle.client.handler.ClientInputHandler;
import mchhui.sizvehicle.client.handler.DebugRenderHandler;
import mchhui.sizvehicle.client.handler.RenderPlayerHandler;
import mchhui.sizvehicle.client.render.SIZEntityRenderFactory;
import mchhui.sizvehicle.common.entity.EntityCar;
import mchhui.sizvehicle.network.NetworkManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = "sizvehicle")
public class ModSIZVehicle {

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        EntityRegistry.registerModEntity(new ResourceLocation("sizvehicle", "siz_vehicle"), EntityCar.class, "siz_vehicle", 1, this, 80, 3, true);
        NetworkManager.init();
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            RenderingRegistry.registerEntityRenderingHandler(EntityCar.class, SIZEntityRenderFactory.FACTORY);
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            MinecraftForge.EVENT_BUS.register(new ClientInputHandler());
            MinecraftForge.EVENT_BUS.register(new DebugRenderHandler());
            MinecraftForge.EVENT_BUS.register(new RenderPlayerHandler());
        }
    }

}
