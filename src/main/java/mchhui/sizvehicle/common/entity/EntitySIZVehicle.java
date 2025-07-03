 package mchhui.sizvehicle.common.entity;

import mchhui.sizvehicle.common.type.TypeBase;
import net.minecraft.entity.EntityLiving;
import net.minecraft.world.World;

public abstract class EntitySIZVehicle extends EntityLiving{
     public EntitySIZVehicle(World worldIn) {
        super(worldIn);
         // TODO Auto-generated constructor stub
    }

    public abstract TypeBase getType();
}
