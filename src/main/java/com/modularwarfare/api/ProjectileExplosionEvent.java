package com.modularwarfare.api;

import java.util.*;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.Vec3d;
import com.modularwarfare.common.entity.EntityExplosiveProjectile;
import net.minecraftforge.fml.common.eventhandler.Event;

/** Server notification AFTER native launcher explosion damage. Not cancellable.
 * Targets lost health or absorption to this explosion; blocked/immune targets are excluded. */
public final class ProjectileExplosionEvent extends Event {
    public final EntityExplosiveProjectile projectile;
    public final Vec3d position;
    public final List<EntityLivingBase> damagedEntities;
    public ProjectileExplosionEvent(EntityExplosiveProjectile projectile, List<EntityLivingBase> damagedEntities) {
        this.projectile = projectile;
        this.position = projectile.getPositionVector();
        this.damagedEntities = Collections.unmodifiableList(new ArrayList<>(damagedEntities));
    }
}
