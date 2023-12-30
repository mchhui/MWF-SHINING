package mchhui.hebridge;

import com.modularwarfare.api.WeaponEnhancedReloadEvent;
import com.modularwarfare.api.WeaponExpShotEvent;
import com.modularwarfare.api.WeaponFireEvent;
import com.modularwarfare.api.WeaponReloadEvent;

import mchhui.he.api.ELMAPI;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class HEBridgeServer {
    @SubscribeEvent
    public void onReload(WeaponReloadEvent.Post event) {
        ELMAPI.playAni(event.getWeaponUser().getUniqueID(), "gun_reload", 1);
    }
    
    @SubscribeEvent
    public void onReload(WeaponExpShotEvent event) {
        ELMAPI.playAni(event.player.getUniqueID(), "gun_fire", 1);
    }
}
