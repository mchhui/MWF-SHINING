package com.modularwarfare.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.modularwarfare.common.guns.BulletType;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Shared launcher ballistics. Gravity is a signed per-tick Y acceleration (negative falls). */
public final class ProjectileAPI {
    private ProjectileAPI() {}

    /** Trusted server skill entry point: exact launch vector, no spread, ammo consumption or weapon cooldown.
     * The skill service must authorize the cast and debit its own cost before calling this method. */
    public static com.modularwarfare.common.entity.EntityExplosiveProjectile launchLauncher(
            net.minecraft.entity.EntityLivingBase shooter, BulletType bullet, Vec3d origin, Vec3d velocity) {
        if (shooter == null || shooter.isDead || shooter.getHealth() <= 0 || shooter.world.isRemote
                || shooter.getServer() == null || !shooter.getServer().isCallingFromMinecraftThread()
                || bullet == null || bullet.internalName == null || bullet.internalName.isEmpty()
                || !finite(origin) || !finite(velocity) || velocity.squareDistanceTo(Vec3d.ZERO) <= 0
                || !Float.isFinite(bullet.gravity)) return null;
        com.modularwarfare.common.entity.EntityExplosiveProjectile projectile =
                new com.modularwarfare.common.entity.EntityExplosiveProjectile(shooter.world, shooter,
                        bullet.impactDamage, 0, 1, bullet.internalName, bullet.gravity, bullet.isSmoke, bullet.isExplosion, 0, 0);
        projectile.setPosition(origin.x, origin.y, origin.z);
        projectile.motionX = velocity.x; projectile.motionY = velocity.y; projectile.motionZ = velocity.z;
        return shooter.world.spawnEntity(projectile) ? projectile : null;
    }

    public static Vec3d direction(float pitch, float yaw) {
        double p = Math.toRadians(pitch), y = Math.toRadians(yaw);
        return new Vec3d(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
    }

    public static AxisAlignedBB bounds(Vec3d position) {
        return new AxisAlignedBB(position.x - 0.1, position.y, position.z - 0.1,
                position.x + 0.1, position.y + 0.2, position.z + 0.1);
    }

    /** Move with current velocity, then apply water drag and gravity, matching EntityExplosiveProjectile. */
    public static Vec3d nextVelocity(World world, Vec3d nextPosition, Vec3d velocity, float gravity) {
        double drag = world.isMaterialInBB(bounds(nextPosition), Material.WATER) ? 0.8 : 1.0;
        return velocity.scale(drag).add(0, gravity, 0);
    }

    /** Nearest block/entity along the actual movement segment; shooter and projectile never intercept it. */
    public static RayTraceResult trace(World world, Entity shooter, Entity projectile, Vec3d start,
                                       Vec3d end, boolean includeEntities) {
        return trace(world, shooter, projectile, start, end, includeEntities, java.util.Collections.emptySet());
    }

    public static RayTraceResult trace(World world, Entity shooter, Entity projectile, Vec3d start,
                                       Vec3d end, boolean includeEntities, Set<String> ignoredEntityTypes) {
        RayTraceResult closest = world.rayTraceBlocks(start, end, false, true, false);
        double distance = closest == null ? start.squareDistanceTo(end) : start.squareDistanceTo(closest.hitVec);
        if (includeEntities) {
            AxisAlignedBB area = bounds(start).expand(end.x - start.x, end.y - start.y, end.z - start.z).grow(0.3);
            for (Entity target : world.getEntitiesWithinAABBExcludingEntity(projectile, area)) {
                if (target == shooter || target == projectile || !target.canBeCollidedWith() || target.isDead
                        || matchesIgnoredType(target, ignoredEntityTypes)) continue;
                AxisAlignedBB box = target.getEntityBoundingBox().grow(0.3);
                RayTraceResult hit = box.calculateIntercept(start, end);
                Vec3d hitPos = box.contains(start) ? start : hit == null ? null : hit.hitVec;
                if (hitPos != null && start.squareDistanceTo(hitPos) <= distance) {
                    distance = start.squareDistanceTo(hitPos);
                    closest = new RayTraceResult(target, hitPos);
                }
            }
        }
        return closest;
    }

    private static boolean matchesIgnoredType(Entity entity, Set<String> ignored) {
        if (ignored == null || ignored.isEmpty()) return false;
        String simple = entity.getClass().getSimpleName();
        String full = entity.getClass().getName();
        net.minecraft.util.ResourceLocation key = EntityList.getKey(entity.getClass());
        String registry = key == null ? "" : key.toString();
        for (String value : ignored) if (value != null && (value.equalsIgnoreCase(simple)
                || value.equalsIgnoreCase(full) || value.equalsIgnoreCase(registry))) return true;
        return false;
    }

    /** Nominal (zero spread) launcher preview. A missing impact means the path was truncated, not a landing. */
    public static Trajectory predictLauncher(World world, Entity shooter, Vec3d origin, Vec3d initialVelocity,
                                              float gravity, int maxTicks, boolean includeEntities) {
        if (world == null || !finite(origin) || !finite(initialVelocity) || !Float.isFinite(gravity)
                || maxTicks < 1 || maxTicks > 600) throw new IllegalArgumentException("Invalid trajectory parameters");
        List<Vec3d> points = new ArrayList<>();
        points.add(origin);
        Vec3d position = origin, velocity = initialVelocity;
        for (int tick = 0; tick < maxTicks; tick++) {
            Vec3d next = position.add(velocity);
            RayTraceResult hit = trace(world, shooter, null, position, next, includeEntities);
            if (hit != null) {
                points.add(hit.hitVec);
                return new Trajectory(points, hit, tick + 1);
            }
            points.add(next);
            velocity = nextVelocity(world, next, velocity, gravity);
            position = next;
        }
        return new Trajectory(points, null, maxTicks);
    }

    public static Trajectory predictLauncher(World world, Entity shooter, Vec3d origin, float pitch, float yaw,
                                              BulletType bullet, int maxTicks, boolean includeEntities) {
        if (bullet == null || !Float.isFinite(bullet.projectileVelocity) || bullet.projectileVelocity <= 0
                || !Float.isFinite(pitch) || !Float.isFinite(yaw)) throw new IllegalArgumentException("Invalid launch parameters");
        return predictLauncher(world, shooter, origin, direction(pitch, yaw).scale(bullet.projectileVelocity),
                bullet.gravity, maxTicks, includeEntities);
    }

    public static boolean finite(Vec3d vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    public static final class Trajectory {
        public final List<Vec3d> points;
        public final RayTraceResult impact;
        public final int simulatedTicks;
        private Trajectory(List<Vec3d> points, RayTraceResult impact, int simulatedTicks) {
            this.points = Collections.unmodifiableList(points);
            this.impact = impact;
            this.simulatedTicks = simulatedTicks;
        }
    }
}
