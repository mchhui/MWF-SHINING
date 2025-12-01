package mchhui.modularmovements.tactical.server;

import com.modularwarfare.api.PlayerSnapshotCreateEvent;
import mchhui.modularmovements.tactical.PlayerState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class MWFServerListener {
    @SubscribeEvent
    public void onPlayerSnapshotCreate(PlayerSnapshotCreateEvent.Pre event) {
        if (event.player instanceof EntityPlayerMP && !event.player.isDead) {
            if (ServerListener.playerStateMap.containsKey(event.player.getEntityId())) {
                PlayerState state = ServerListener.playerStateMap.get(event.player.getEntityId());
                if (state.probeOffset != 0) {
                    Vec3d vec3d = Vec3d.ZERO.add(state.probeOffset * -0.5, 0, 0)
                            .rotateYaw(-(event.player.rotationYaw * 3.14f / 180));
                    event.pos.x+=vec3d.x;
                    event.pos.z+=vec3d.z;
                }
            }
        }
    }
}
