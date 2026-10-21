package mchhui.easyeffect;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientCustomPacketEvent;

public class EasyEffectClient {
    public static EasyEffectRenderer renderer=new EasyEffectRenderer();
    
    @SubscribeEvent
    public void onHandle(ClientCustomPacketEvent event) {
        PacketBuffer buf=new PacketBuffer(event.getPacket().payload());
        double x=buf.readDouble();
        double y=buf.readDouble();
        double z=buf.readDouble();
        double vx=buf.readDouble();
        double vy=buf.readDouble();
        double vz=buf.readDouble();
        double ax=buf.readDouble();
        double ay=buf.readDouble();
        double az=buf.readDouble();
        int delay=buf.readInt();
        int fps=buf.readInt();
        int length=buf.readInt();
        int unit=buf.readInt();
        double size=buf.readDouble();
        String name=buf.readString(Short.MAX_VALUE);
        renderer.objects.add(new EEObject(name,delay,fps,unit,length,x,y,z,vx,vy,vz,ax,ay,az,size,System.currentTimeMillis()));
        event.getPacket().payload().release();
    }
    
    @SubscribeEvent
    public void onRenderWolrd(RenderWorldLastEvent event) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        renderer.render(event.getPartialTicks());
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }
}
