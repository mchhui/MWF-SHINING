package mchhui.modularmovements.tactical.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FakeTutorial extends Tutorial{

    public FakeTutorial(Minecraft minecraft) {
        super(minecraft);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    public void handleMouse(MouseHelper mouseHelper) {
        super.handleMouse(mouseHelper);
        ClientLitener.onMouseMove(mouseHelper);
    }

}
