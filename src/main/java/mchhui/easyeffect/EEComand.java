package mchhui.easyeffect;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class EEComand extends CommandBase {

    @Override
    public int getRequiredPermissionLevel() {
        // TODO Auto-generated method stub
        return 2;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return "ee";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        // TODO Auto-generated method stub
        return "/ee <player> <x> <y> <z> <vx> <vy> <vz> <ax> <ay> <az> <delay> <fps> <duration> <unit> <size> <image>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length != 16) {
            sender.sendMessage(new TextComponentString("wrong args"));
            return;
        }
        List<Entity> list = getEntityList(server, sender, args[0]);
        double x = parseCoordinate(sender.getPositionVector().x, args[1], true).getResult();
        double y = parseCoordinate(sender.getPositionVector().y, args[2], true).getResult();
        double z = parseCoordinate(sender.getPositionVector().z, args[3], true).getResult();
        double vx = Double.valueOf(args[4]);
        double vy = Double.valueOf(args[5]);
        double vz = Double.valueOf(args[6]);
        double ax = Double.valueOf(args[7]);
        double ay = Double.valueOf(args[8]);
        double az = Double.valueOf(args[9]);
        int delay = Integer.valueOf(args[10]);
        int fps = Integer.valueOf(args[11]);
        int length = Integer.valueOf(args[12]);
        int unit = Integer.valueOf(args[13]);
        double size = Double.valueOf(args[14]);
        list.forEach((e) -> {
            if (e instanceof EntityPlayerMP) {
                EasyEffect.sendEffect((EntityPlayerMP)e, x, y, z, vx, vy, vz, ax, ay, az, delay, fps, length, unit, size, args[15]);
            }
        });

    }

}
