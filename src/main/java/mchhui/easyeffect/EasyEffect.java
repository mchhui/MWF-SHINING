package mchhui.easyeffect;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "easyeffect")
public class EasyEffect {
    public static FMLEventChannel channel;

    public static void sendEffect(EntityPlayerMP e, double x, double y, double z, double vx, double vy, double vz, double ax,
                                  double ay, double az, int delay, int fps, int length, int unit, double size, String path) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(vx);
        buf.writeDouble(vy);
        buf.writeDouble(vz);
        buf.writeDouble(ax);
        buf.writeDouble(ay);
        buf.writeDouble(az);
        buf.writeInt(delay);
        buf.writeInt(fps);
        buf.writeInt(length);
        buf.writeInt(unit);
        buf.writeDouble(size);
        buf.writeString(path);
        EasyEffect.channel.sendTo(new FMLProxyPacket(buf, "easyeffect"), e);
    }

    @EventHandler
    public void onInit(FMLInitializationEvent event) {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel("easyeffect");
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            EasyEffectClient client = new EasyEffectClient();
            MinecraftForge.EVENT_BUS.register(client);
            channel.register(client);
        }
    }

    @EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new EEComand());
    }
}
