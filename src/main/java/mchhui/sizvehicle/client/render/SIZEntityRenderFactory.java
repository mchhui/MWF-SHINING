 package mchhui.sizvehicle.client.render;

import mchhui.sizvehicle.common.entity.EntityCar;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class SIZEntityRenderFactory implements IRenderFactory<EntityCar> {
    public static final SIZEntityRenderFactory FACTORY = new SIZEntityRenderFactory();

    @Override
    public Render<? super EntityCar> createRenderFor(RenderManager manager) {
        return new RenderSIZVehicle(manager);
    }
}