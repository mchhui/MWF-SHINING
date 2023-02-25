package mchhui.modularmovements.tactical.client;

import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeLayerBipedArmor extends LayerBipedArmor {
    private final RenderLivingBase<?> renderer;
    public FakeLayerBipedArmor(RenderLivingBase<?> rendererIn) {
        super(rendererIn);
        renderer=rendererIn;
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void initArmor() {
        this.modelLeggings = new FakeModelBiped(0.5F);
        this.modelArmor = new FakeModelBiped(1.0F);
    }
}
