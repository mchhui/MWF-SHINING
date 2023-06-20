package mchhui.modularmovements.coremod;

import mchhui.modularmovements.tactical.client.ClientLitener;
import mchhui.modularmovements.tactical.server.ServerListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

public class ModularMovementsHooks {

    public static Vec3d onGetPositionEyes(EntityPlayer player, float partialTicks) {
        Vec3d vec3d;
        if (partialTicks == 1.0F) {
            vec3d = new Vec3d(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ);
        } else {
            double d0 = player.prevPosX + (player.posX - player.prevPosX) * (double) partialTicks;
            double d1 = player.prevPosY + (player.posY - player.prevPosY) * (double) partialTicks
                    + (double) player.getEyeHeight();
            double d2 = player.prevPosZ + (player.posZ - player.prevPosZ) * (double) partialTicks;
            vec3d = new Vec3d(d0, d1, d2);
        }

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            vec3d = ClientLitener.onGetPositionEyes(player, partialTicks, vec3d);
        } else {
            vec3d = ServerListener.onGetPositionEyes(player, partialTicks, vec3d);
        }
        return vec3d;
    }

    public static AxisAlignedBB getEntityBoundingBox(Entity entity, AxisAlignedBB bb) {
        /*
        AxisAlignedBB client= ClientLitener.getEntityBoundingBox(entity, bb);
        if(client!=bb) {
            return client;
        }
        */
        return bb;
    }
}
