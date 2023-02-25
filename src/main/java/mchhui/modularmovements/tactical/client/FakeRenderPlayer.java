package mchhui.modularmovements.tactical.client;



import com.modularwarfare.client.model.layers.RenderLayerBackpack;
import com.modularwarfare.client.model.layers.RenderLayerBody;
import com.modularwarfare.client.model.layers.RenderLayerHeldGun;
import mchhui.modularmovements.ModularMovements;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeRenderPlayer extends RenderPlayer {

    public FakeRenderPlayer(RenderManager renderManager, boolean useSmallArms) {
        super(renderManager, useSmallArms);
        this.mainModel = new FakePlayerModel(0.0F, useSmallArms);
        for (int i = 0; i < this.layerRenderers.size(); i++) {
            if (this.layerRenderers.get(i).getClass() == LayerBipedArmor.class) {
                //must to i-- next time
                this.layerRenderers.remove(i);
                i--;
                break;
            }
        }
        if (ModularMovements.mwfEnable) {
            this.addLayer(new FakeLayerBipedArmor(this));
            this.addLayer(new RenderLayerBackpack(this, this.getMainModel().bipedBodyWear));
            this.addLayer(new RenderLayerBody(this, this.getMainModel().bipedBodyWear));
            this.addLayer(new RenderLayerHeldGun(this));
        }
    }

    public FakeRenderPlayer(RenderManager renderManager) {
        this(renderManager, false);
    }

    protected void applyRotations(AbstractClientPlayer entityLiving, float p_77043_2_, float rotationYaw,
            float partialTicks) {
        if (ClientLitener.applyRotations(this, entityLiving, p_77043_2_, rotationYaw, partialTicks)) {
            return;
        }
        super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
    }

}
