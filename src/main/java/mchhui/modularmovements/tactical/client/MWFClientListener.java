package mchhui.modularmovements.tactical.client;

import com.modularwarfare.api.GunBobbingEvent;
import com.modularwarfare.api.PlayerSnapshotCreateEvent;
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
    

    @SubscribeEvent
    public void onPlayerSnapshotCreate(PlayerSnapshotCreateEvent.Pre event) {
        if (event.player instanceof EntityPlayer && !event.player.isDead) {
            if (ClientListener.ohterPlayerStateMap.containsKey(event.player.getEntityId())) {
                PlayerState state = ClientListener.ohterPlayerStateMap.get(event.player.getEntityId());
                if (state.probeOffset != 0) {
                    Vec3d vec3d = Vec3d.ZERO.add(state.probeOffset * -0.5, 0, 0)
                            .rotateYaw(-(event.player.rotationYaw * 3.14f / 180));
                    event.pos.x+=vec3d.x;
                    event.pos.z+=vec3d.z;
                }
                if(state.isSitting){
                    event.pos.y -= 0.8f;
                }
                if(state.isCrawling){
                    event.pos.y -= 1.5f;
                }

            }
        }
    }
}
