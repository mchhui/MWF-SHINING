package net.minecraft.entity;

public class EntityHelper {
    public static void setFlag(Entity entity, int flag, boolean state) {
        entity.setFlag(flag, state);
    }

    public static int getTicksElytraFlying(EntityLivingBase entityLivingBase) {
        return entityLivingBase.ticksElytraFlying;
    }
}
