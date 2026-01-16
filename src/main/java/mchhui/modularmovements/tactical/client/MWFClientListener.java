package mchhui.modularmovements.tactical.client;

import com.modularwarfare.api.GunBobbingEvent;
import mchhui.modularmovements.tactical.PlayerState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MWFClientListener {

    @SubscribeEvent
    public void onGunBobbing(GunBobbingEvent event) {
        if (ClientListener.clientPlayerSitMoveAmplifier > 0) {
            event.bobbing = 0;
        }
    }
    
}
