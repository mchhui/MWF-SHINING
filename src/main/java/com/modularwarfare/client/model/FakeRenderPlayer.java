package com.modularwarfare.client.model;



import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.model.layers.RenderLayerBackpack;
import com.modularwarfare.client.model.layers.RenderLayerBody;
import com.modularwarfare.client.model.layers.RenderLayerHeldGun;
import com.modularwarfare.client.model.layers.RenderLayerHeldMelee;
import com.modularwarfare.client.model.layers.RenderLayerHeldGrenade;
import com.modularwarfare.client.model.layers.ResetHiddenModelLayer;
import com.mrcrayfish.obfuscate.client.event.ModelPlayerEvent;
import com.mrcrayfish.vehicle.client.render.layer.LayerHeldVehicle;

import mchhui.modularmovements.tactical.client.ClientListener;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.MWFRenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraftforge.common.MinecraftForge;
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
        //see com.modularwarfare.client.ClientProxy.setupLayers
        MWFRenderHelper helper = new MWFRenderHelper(this);
        this.addLayer(new FakeLayerBipedArmor(this));
        helper.getLayerRenderers().add(0, new ResetHiddenModelLayer(this));
        this.addLayer(new RenderLayerBackpack(this, this.getMainModel().bipedBodyWear));
        this.addLayer(new RenderLayerBody(this, this.getMainModel().bipedBodyWear));
        this.addLayer(new RenderLayerHeldGun(this));
        this.addLayer(new RenderLayerHeldMelee(this));
        this.addLayer(new RenderLayerHeldGrenade(this));
    }

    public FakeRenderPlayer(RenderManager renderManager) {
        this(renderManager, false);
    }

    protected void applyRotations(AbstractClientPlayer entityLiving, float p_77043_2_, float rotationYaw,
            float partialTicks) {
        if(ModularWarfare.isLoadedModularMovements) {
//            if (ClientListener.applyRotations(this, entityLiving, p_77043_2_, rotationYaw, partialTicks)) {
//                return;
//            }  
        }
//        System.out.println("test");
        super.applyRotations(entityLiving, p_77043_2_, rotationYaw, partialTicks);
        if(ModularWarfare.isLoadedObfuscate) {
            GlStateManager.rotate(180, 0, 0, 1);
            GlStateManager.scale(0.1, 0.1, 0.1);
            MinecraftForge.EVENT_BUS.post(new ModelPlayerEvent.Render.Pre(entityLiving, getMainModel(), partialTicks));
            GlStateManager.scale(10, 10, 10);
            GlStateManager.rotate(-180, 0, 0, 1);
        }
    }

}
