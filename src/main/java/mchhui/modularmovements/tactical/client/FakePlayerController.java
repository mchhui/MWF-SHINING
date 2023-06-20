package mchhui.modularmovements.tactical.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakePlayerController extends PlayerControllerMP {

    public FakePlayerController(Minecraft mcIn, NetHandlerPlayClient netHandler) {
        super(mcIn, netHandler);
        // TODO Auto-generated constructor stub
    }

}
