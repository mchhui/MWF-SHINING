 package mchhui.sizvehicle.client.handler;

import mchhui.sizvehicle.common.entity.EntitySIZVehicle;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RenderPlayerHandler {
    @SubscribeEvent 
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
         if(event.getEntityPlayer().getRidingEntity() instanceof EntitySIZVehicle) {
             event.setCanceled(true);
         }
     }
}
