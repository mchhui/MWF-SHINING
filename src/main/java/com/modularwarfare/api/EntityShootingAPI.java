package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponFireMode;
import com.modularwarfare.common.guns.manager.FireManager;
import com.modularwarfare.common.guns.manager.FireManager.FireData;
import com.modularwarfare.common.network.PacketGunFire;
import com.modularwarfare.common.network.PacketDelayedShoot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.modularwarfare.common.guns.ItemAmmo;
import com.modularwarfare.common.guns.ItemBullet;

public class EntityShootingAPI {
    private static final ThreadLocal<Boolean> SKILL_SHOT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DEBUG_SHOT = new ThreadLocal<>();
    public static boolean isDebugShot() { return Boolean.TRUE.equals(DEBUG_SHOT.get()); }

    /** Exact server ray after weapon spread; observational only, including misses. */
    public static final class ShotTraceEvent extends Event {
        public final EntityLivingBase shooter;
        public final Vec3d origin, end;
        public final java.util.List<com.modularwarfare.utility.raycast.hits.BulletHit> hits;
        public ShotTraceEvent(EntityLivingBase shooter, Vec3d origin, Vec3d end,
                java.util.List<com.modularwarfare.utility.raycast.hits.BulletHit> hits) {
            this.shooter = shooter; this.origin = origin; this.end = end;
            this.hits = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(hits));
        }
    }
    /** True only while a skill invokes the native firing pipeline on this server thread. */
    public static boolean isSkillShot() { return Boolean.TRUE.equals(SKILL_SHOT.get()); }
    
    static {
        MinecraftForge.EVENT_BUS.register(EntityShootingAPI.class);
    }
    
    // ==================== 事件类定义 ====================
    
    @net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class EntityShootEvent extends Event {
        private final UUID entityUUID;
        private final EntityLivingBase entity;
        private final ItemStack weaponStack;
        private final ItemGun weapon;
        private final int shotCount;
        private final boolean useHeldWeapon;
        private final String specifiedWeaponName;
        private final String specifiedAmmoName;
        private final String specifiedMagazineName;
        
        public EntityShootEvent(UUID entityUUID, EntityLivingBase entity, ItemStack weaponStack, 
                              ItemGun weapon, int shotCount, boolean useHeldWeapon, 
                              String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
            this.entityUUID = entityUUID;
            this.entity = entity;
            this.weaponStack = weaponStack;
            this.weapon = weapon;
            this.shotCount = shotCount;
            this.useHeldWeapon = useHeldWeapon;
            this.specifiedWeaponName = specifiedWeaponName;
            this.specifiedAmmoName = specifiedAmmoName;
            this.specifiedMagazineName = specifiedMagazineName;
        }
        
        public UUID getEntityUUID() { return entityUUID; }
        public EntityLivingBase getEntity() { return entity; }
        public ItemStack getWeaponStack() { return weaponStack; }
        public ItemGun getWeapon() { return weapon; }
        public int getShotCount() { return shotCount; }
        public boolean isUseHeldWeapon() { return useHeldWeapon; }
        public String getSpecifiedWeaponName() { return specifiedWeaponName; }
        public String getSpecifiedAmmoName() { return specifiedAmmoName; }
        public String getSpecifiedMagazineName() { return specifiedMagazineName; }
    }
    
    @net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class EntityTargetShootEvent extends Event {
        private final UUID entityUUID;
        private final EntityLivingBase entity;
        private final UUID targetUUID;
        private final EntityLivingBase target;
        private final ItemStack weaponStack;
        private final ItemGun weapon;
        private final int shotCount;
        private final boolean useHeldWeapon;
        private final String specifiedWeaponName;
        private final String specifiedAmmoName;
        private final String specifiedMagazineName;
        private final double maxDistance;
        
        public EntityTargetShootEvent(UUID entityUUID, EntityLivingBase entity, UUID targetUUID, EntityLivingBase target,
                                    ItemStack weaponStack, ItemGun weapon, int shotCount, boolean useHeldWeapon,
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName,
                                    double maxDistance) {
            this.entityUUID = entityUUID;
            this.entity = entity;
            this.targetUUID = targetUUID;
            this.target = target;
            this.weaponStack = weaponStack;
            this.weapon = weapon;
            this.shotCount = shotCount;
            this.useHeldWeapon = useHeldWeapon;
            this.specifiedWeaponName = specifiedWeaponName;
            this.specifiedAmmoName = specifiedAmmoName;
            this.specifiedMagazineName = specifiedMagazineName;
            this.maxDistance = maxDistance;
        }
        
        public UUID getEntityUUID() { return entityUUID; }
        public EntityLivingBase getEntity() { return entity; }
        public UUID getTargetUUID() { return targetUUID; }
        public EntityLivingBase getTarget() { return target; }
        public ItemStack getWeaponStack() { return weaponStack; }
        public ItemGun getWeapon() { return weapon; }
        public int getShotCount() { return shotCount; }
        public boolean isUseHeldWeapon() { return useHeldWeapon; }
        public String getSpecifiedWeaponName() { return specifiedWeaponName; }
        public String getSpecifiedAmmoName() { return specifiedAmmoName; }
        public String getSpecifiedMagazineName() { return specifiedMagazineName; }
        public double getMaxDistance() { return maxDistance; }
    }
    
    @net.minecraftforge.fml.common.eventhandler.Cancelable
    public static class EntityDelayedShootEvent extends Event {
        private final UUID entityUUID;
        private final EntityLivingBase entity;
        private final UUID targetUUID;
        private final EntityLivingBase target;
        private final double targetX, targetY, targetZ;
        private final ItemStack weaponStack;
        private final ItemGun weapon;
        private final int shotCount;
        private final boolean useHeldWeapon;
        private final String specifiedWeaponName;
        private final String specifiedAmmoName;
        private final String specifiedMagazineName;
        private final double maxDistance;
        private final int delayTicks;
        private final float offsetX, offsetY, offsetZ;
        private final boolean isCoordinateShoot;
        
        public EntityDelayedShootEvent(UUID entityUUID, EntityLivingBase entity, UUID targetUUID, EntityLivingBase target,
                                     double targetX, double targetY, double targetZ, ItemStack weaponStack, ItemGun weapon, 
                                     int shotCount, boolean useHeldWeapon, String specifiedWeaponName, 
                                     String specifiedAmmoName, String specifiedMagazineName, double maxDistance,
                                     int delayTicks, float offsetX, float offsetY, float offsetZ, boolean isCoordinateShoot) {
            this.entityUUID = entityUUID;
            this.entity = entity;
            this.targetUUID = targetUUID;
            this.target = target;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.weaponStack = weaponStack;
            this.weapon = weapon;
            this.shotCount = shotCount;
            this.useHeldWeapon = useHeldWeapon;
            this.specifiedWeaponName = specifiedWeaponName;
            this.specifiedAmmoName = specifiedAmmoName;
            this.specifiedMagazineName = specifiedMagazineName;
            this.maxDistance = maxDistance;
            this.delayTicks = delayTicks;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.isCoordinateShoot = isCoordinateShoot;
        }
        
        public UUID getEntityUUID() { return entityUUID; }
        public EntityLivingBase getEntity() { return entity; }
        public UUID getTargetUUID() { return targetUUID; }
        public EntityLivingBase getTarget() { return target; }
        public double getTargetX() { return targetX; }
        public double getTargetY() { return targetY; }
        public double getTargetZ() { return targetZ; }
        public ItemStack getWeaponStack() { return weaponStack; }
        public ItemGun getWeapon() { return weapon; }
        public int getShotCount() { return shotCount; }
        public boolean isUseHeldWeapon() { return useHeldWeapon; }
        public String getSpecifiedWeaponName() { return specifiedWeaponName; }
        public String getSpecifiedAmmoName() { return specifiedAmmoName; }
        public String getSpecifiedMagazineName() { return specifiedMagazineName; }
        public double getMaxDistance() { return maxDistance; }
        public int getDelayTicks() { return delayTicks; }
        public float getOffsetX() { return offsetX; }
        public float getOffsetY() { return offsetY; }
        public float getOffsetZ() { return offsetZ; }
        public boolean isCoordinateShoot() { return isCoordinateShoot; }
    }
    
    public static boolean shootEntity(UUID entityUUID, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
        EntityLivingBase entity = findEntity(entityUUID);
        if (entity == null) {
            ModularWarfare.LOGGER.warn("Cannot find entity with UUID: {}", entityUUID);
            return false;
        }
        
        return shootEntity(entity, shotCount, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 0.0f);
    }

    public static boolean shootEntity(EntityLivingBase entity, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
        return shootEntity(entity, shotCount, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 0.0f, 0.0f);
    }

    public static boolean shootEntity(EntityLivingBase entity, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName, float customDamage) {
        return shootEntity(entity, shotCount, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, customDamage, 0.0f);
    }

    public static boolean shootEntity(EntityLivingBase entity, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName, float customDamage, float customHeadshotBonus) {
        return submit(entity, null, null, shotCount, Double.MAX_VALUE, 0, Vec3d.ZERO,
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, false);
    }
    public static boolean shootEntityAtTarget(UUID entityUUID, UUID targetUUID, int shotCount, double maxDistance,
                                            boolean useHeldWeapon, String specifiedWeaponName, 
                                            String specifiedAmmoName, String specifiedMagazineName) {
        EntityLivingBase entity = findEntity(entityUUID);
        if (entity == null) {
            ModularWarfare.LOGGER.warn("Cannot find shooting entity with UUID: {}", entityUUID);
            return false;
        }
        
        EntityLivingBase target = findEntity(targetUUID);
        if (target == null) {
            ModularWarfare.LOGGER.warn("Cannot find target entity with UUID: {}", targetUUID);
            return false;
        }
        
        return shootEntityAtTarget(entity, target, shotCount, maxDistance, useHeldWeapon, 
                                 specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
    }

    public static boolean shootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                            double maxDistance, boolean useHeldWeapon, String specifiedWeaponName,
                                            String specifiedAmmoName, String specifiedMagazineName) {
        return shootEntityAtTarget(entity, target, shotCount, maxDistance, useHeldWeapon, 
                                 specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 0.0f, 0.0f);
    }

    public static boolean shootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                            double maxDistance, boolean useHeldWeapon, String specifiedWeaponName,
                                            String specifiedAmmoName, String specifiedMagazineName, float customDamage) {
        return shootEntityAtTarget(entity, target, shotCount, maxDistance, useHeldWeapon, 
                                 specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, customDamage, 0.0f);
    }

    public static boolean shootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                            double maxDistance, boolean useHeldWeapon, String specifiedWeaponName,
                                            String specifiedAmmoName, String specifiedMagazineName, float customDamage, float customHeadshotBonus) {
        if (target == null) return false;
        return submit(entity, target, null, shotCount, maxDistance, 0, Vec3d.ZERO,
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, false);
    }
    public static boolean shootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ, 
                                                 int shotCount, double maxDistance, boolean useHeldWeapon, 
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
        return shootEntityAtCoordinates(entity, targetX, targetY, targetZ, shotCount, maxDistance, useHeldWeapon, 
                                      specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 0.0f, 0.0f);
    }

    public static boolean shootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ, 
                                                 int shotCount, double maxDistance, boolean useHeldWeapon, 
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName, float customDamage) {
        return shootEntityAtCoordinates(entity, targetX, targetY, targetZ, shotCount, maxDistance, useHeldWeapon, 
                                      specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, customDamage, 0.0f);
    }

    public static boolean shootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ, 
                                                 int shotCount, double maxDistance, boolean useHeldWeapon, 
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName, float customDamage, float customHeadshotBonus) {
        return submit(entity, null, new Vec3d(targetX, targetY, targetZ), shotCount, maxDistance, 0, Vec3d.ZERO,
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, false);
    }

    /** Player-fire variant using the native client ray suggestions. */
    public static boolean shootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ,
                                                 int shotCount, double maxDistance, boolean useHeldWeapon,
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName,
                                                 float customDamage, float customHeadshotBonus,
                                                 java.util.List<PacketGunFire.Hit> clientSuggestions) {
        return submit(entity, null, new Vec3d(targetX, targetY, targetZ), shotCount, maxDistance, 0, Vec3d.ZERO,
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, false, clientSuggestions);
    }

    /** Native shots at caller-selected world points. Terrain/area selection belongs to the caller.
     * spread uses GunType.bulletSpread units: zero = exact, -1 = snapshot current weapon accuracy.
     * This overload applies maxDistance to both validation and the actual ray length. */
    public static boolean shootEntityAtPoints(EntityLivingBase entity, Vec3d fixedOrigin, java.util.List<Vec3d> points,
            double maxDistance, boolean useHeldWeapon, String weaponName, String ammoName, String bulletName,
            float damage, float headshot, float spread, boolean debugTrace) {
        return shootEntityAtPoints(entity, fixedOrigin, null, points, maxDistance, useHeldWeapon,
                weaponName, ammoName, bulletName, damage, headshot, spread, debugTrace);
    }

    /** One immutable origin per target; the caller owns placement and clearance rules. */
    public static boolean shootEntityFromPoints(EntityLivingBase entity, java.util.List<Vec3d> origins, java.util.List<Vec3d> points,
            double maxDistance, boolean useHeldWeapon, String weaponName, String ammoName, String bulletName,
            float damage, float headshot, float spread, boolean debugTrace) {
        if (origins == null || points == null || origins.size() != points.size()) return false;
        for (Vec3d origin : origins) if (origin == null || !ProjectileAPI.finite(origin)) return false;
        return shootEntityAtPoints(entity, null, origins, points, maxDistance, useHeldWeapon,
                weaponName, ammoName, bulletName, damage, headshot, spread, debugTrace);
    }

    private static boolean shootEntityAtPoints(EntityLivingBase entity, Vec3d fixedOrigin, java.util.List<Vec3d> origins,
            java.util.List<Vec3d> points, double maxDistance, boolean useHeldWeapon, String weaponName, String ammoName,
            String bulletName, float damage, float headshot, float spread, boolean debugTrace) {
        if (!serverEntity(entity) || points == null || points.isEmpty() || points.size() > 1024
                || !Float.isFinite(spread) || (spread < 0 && spread != -1)
                || (fixedOrigin != null && !ProjectileAPI.finite(fixedOrigin))) return false;
        for (Vec3d point : points) if (point == null || !ProjectileAPI.finite(point)) return false;
        ItemStack stack = useHeldWeapon ? entity.getHeldItemMainhand() : createWeaponStack(weaponName, ammoName, bulletName);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) return false;
        float resolvedSpread = spread == -1 ? calculateServerAccuracy((ItemGun) stack.getItem(), entity) : spread;
        return submitInternal(entity, null, points.get(0), points, fixedOrigin, points.size(), maxDistance, 0,
                Vec3d.ZERO, useHeldWeapon, weaponName, ammoName, bulletName, damage, headshot, false, null,
                debugTrace, resolvedSpread, origins);
    }

    /** Submit a direct-fire burst at several server-selected points around a center. */
    public static boolean shootEntityAtCoordinatesSpread(EntityLivingBase entity, double centerX, double centerY, double centerZ,
            int shotCount, double maxDistance, double spread, boolean useHeldWeapon, String weaponName,
            String ammoName, String bulletName, float customDamage, float customHeadshotBonus) {
        return shootEntityAtCoordinatesSpread(entity, null, centerX, centerY, centerZ, shotCount, maxDistance, spread,
                useHeldWeapon, weaponName, ammoName, bulletName, customDamage, customHeadshotBonus);
    }

    /** Detached direct-fire burst: the muzzle origin is snapshotted when the skill is committed. */
    public static boolean shootEntityAtCoordinatesSpread(EntityLivingBase entity, Vec3d fixedOrigin,
            double centerX, double centerY, double centerZ, int shotCount, double maxDistance, double spread,
            boolean useHeldWeapon, String weaponName, String ammoName, String bulletName,
            float customDamage, float customHeadshotBonus) {
        return shootEntityAtCoordinatesSpread(entity, fixedOrigin, centerX, centerY, centerZ, shotCount,
                maxDistance, spread, useHeldWeapon, weaponName, ammoName, bulletName, customDamage, customHeadshotBonus, false);
    }

    /** Opt-in diagnostics are emitted per actual pellet, including scheduled shots. */
    public static boolean shootEntityAtCoordinatesSpread(EntityLivingBase entity, Vec3d fixedOrigin,
            double centerX, double centerY, double centerZ, int shotCount, double maxDistance, double spread,
            boolean useHeldWeapon, String weaponName, String ammoName, String bulletName,
            float customDamage, float customHeadshotBonus, boolean debugTrace) {
        if (!serverEntity(entity)) return false;
        if (!Double.isFinite(spread) || spread < 0 || spread > 64 || shotCount < 1 || shotCount > 1024) return false;
        if (fixedOrigin != null && !ProjectileAPI.finite(fixedOrigin)) return false;
        Vec3d center = new Vec3d(centerX, centerY, centerZ);
        java.util.ArrayList<Vec3d> points = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random(entity.getUniqueID().getLeastSignificantBits() ^ entity.world.getTotalWorldTime());
        Vec3d forward = fixedOrigin == null
                ? ProjectileAPI.direction(entity.rotationPitch, entity.rotationYaw).normalize()
                : center.subtract(fixedOrigin).normalize();
        if (forward.lengthSquared() < 1.0E-8D) forward = ProjectileAPI.direction(entity.rotationPitch, entity.rotationYaw).normalize();
        Vec3d upRef = Math.abs(forward.y) < .95 ? new Vec3d(0, 1, 0) : new Vec3d(1, 0, 0);
        Vec3d right = forward.crossProduct(upRef).normalize();
        Vec3d up = right.crossProduct(forward).normalize();
        for (int i = 0; i < shotCount; i++) {
            double r = spread * Math.sqrt(random.nextDouble());
            double a = random.nextDouble() * Math.PI * 2.0;
            points.add(center.add(right.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r)));
        }
        return submit(entity, null, points, fixedOrigin, shotCount, maxDistance, 0, Vec3d.ZERO, useHeldWeapon,
                weaponName, ammoName, bulletName, customDamage, customHeadshotBonus, false, null, debugTrace);
    }
    public static boolean delayedShootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                                   double maxDistance, int delayTicks, float offsetX, float offsetY, float offsetZ,
                                                   boolean useHeldWeapon, String specifiedWeaponName, 
                                                   String specifiedAmmoName, String specifiedMagazineName) {
        return delayedShootEntityAtTarget(entity, target, shotCount, maxDistance, delayTicks, offsetX, offsetY, offsetZ,
                                        useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 0.0f, 0.0f);
    }

    public static boolean delayedShootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                                   double maxDistance, int delayTicks, float offsetX, float offsetY, float offsetZ,
                                                   boolean useHeldWeapon, String specifiedWeaponName, 
                                                   String specifiedAmmoName, String specifiedMagazineName, float customDamage) {
        return delayedShootEntityAtTarget(entity, target, shotCount, maxDistance, delayTicks, offsetX, offsetY, offsetZ,
                                        useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, customDamage, 0.0f);
    }

    public static boolean delayedShootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                                   double maxDistance, int delayTicks, float offsetX, float offsetY, float offsetZ,
                                                   boolean useHeldWeapon, String specifiedWeaponName, 
                                                   String specifiedAmmoName, String specifiedMagazineName, float customDamage, float customHeadshotBonus) {
        if (target == null) return false;
        return submit(entity, target, null, shotCount, maxDistance, delayTicks, new Vec3d(offsetX, offsetY, offsetZ),
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, true);
    }
    public static boolean delayedShootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ,
                                                        int shotCount, double maxDistance, int delayTicks, 
                                                        float offsetX, float offsetY, float offsetZ,
                                                        boolean useHeldWeapon, String specifiedWeaponName, 
                                                        String specifiedAmmoName, String specifiedMagazineName) {
        return delayedShootEntityAtCoordinates(entity, targetX, targetY, targetZ, shotCount, maxDistance, delayTicks,
                                             offsetX, offsetY, offsetZ, useHeldWeapon, specifiedWeaponName, 
                                             specifiedAmmoName, specifiedMagazineName, 0.0f, 0.0f);
    }

    public static boolean delayedShootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ,
                                                        int shotCount, double maxDistance, int delayTicks, 
                                                        float offsetX, float offsetY, float offsetZ,
                                                        boolean useHeldWeapon, String specifiedWeaponName, 
                                                        String specifiedAmmoName, String specifiedMagazineName, float customDamage) {
        return delayedShootEntityAtCoordinates(entity, targetX, targetY, targetZ, shotCount, maxDistance, delayTicks,
                                             offsetX, offsetY, offsetZ, useHeldWeapon, specifiedWeaponName, 
                                             specifiedAmmoName, specifiedMagazineName, customDamage, 0.0f);
    }

    public static boolean delayedShootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ,
                                                        int shotCount, double maxDistance, int delayTicks, 
                                                        float offsetX, float offsetY, float offsetZ,
                                                        boolean useHeldWeapon, String specifiedWeaponName, 
                                                        String specifiedAmmoName, String specifiedMagazineName, float customDamage, float customHeadshotBonus) {
        return submit(entity, null, new Vec3d(targetX, targetY, targetZ), shotCount, maxDistance, delayTicks, new Vec3d(offsetX, offsetY, offsetZ),
                useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName,
                customDamage, customHeadshotBonus, true);
    }
    private static final ConcurrentHashMap<UUID, ShootTask> tasks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Cooldown> nextShotTicks = new ConcurrentHashMap<>();

    private static final class Cooldown {
        final World world;
        final double tick;
        Cooldown(World world, double tick) { this.world = world; this.tick = tick; }
    }
    private static double deadline(EntityLivingBase entity) {
        Cooldown cooldown = nextShotTicks.get(entity.getUniqueID());
        return cooldown != null && cooldown.world == entity.world ? cooldown.tick : 0;
    }

    private static final class ShootTask {
        final EntityLivingBase shooter, target;
        final World world;
        final Vec3d coordinates, offset, fixedOrigin;
        final ItemStack stack;
        final boolean held;
        final double range;
        final float damage, headshot;
        final java.util.List<PacketGunFire.Hit> clientSuggestions;
        final java.util.List<Vec3d> coordinateSequence;
        boolean debugTrace;
        Float spreadOverride;
        java.util.List<Vec3d> originSequence;
        int fired;
        int remaining;
        double nextTick;
        ShootTask(EntityLivingBase shooter, EntityLivingBase target, Vec3d coordinates, Vec3d fixedOrigin, Vec3d offset,
                  ItemStack stack, boolean held, double range, int count, int delay, float damage, float headshot,
                  java.util.List<PacketGunFire.Hit> clientSuggestions) {
            this(shooter, target, coordinates == null ? null : java.util.Collections.singletonList(coordinates), fixedOrigin, offset,
                    stack, held, range, count, delay, damage, headshot, clientSuggestions);
        }
        ShootTask(EntityLivingBase shooter, EntityLivingBase target, java.util.List<Vec3d> coordinateSequence, Vec3d fixedOrigin, Vec3d offset,
                  ItemStack stack, boolean held, double range, int count, int delay, float damage, float headshot,
                  java.util.List<PacketGunFire.Hit> clientSuggestions) {
            this.shooter = shooter; this.target = target; this.world = shooter.world;
            this.coordinateSequence = coordinateSequence == null ? null : new java.util.ArrayList<>(coordinateSequence);
            this.coordinates = coordinateSequence == null || coordinateSequence.isEmpty() ? null : coordinateSequence.get(0);
            this.fixedOrigin = fixedOrigin; this.offset = offset; this.stack = stack;
            this.held = held; this.range = range; this.remaining = count; this.damage = damage; this.headshot = headshot;
            this.clientSuggestions = clientSuggestions == null ? null : new java.util.ArrayList<>(clientSuggestions);
            this.nextTick = world.getTotalWorldTime() + delay;
        }
    }

    /** Server main thread only. false means rejected; multi-shot true means accepted, not all shots completed. */
    private static boolean submit(EntityLivingBase entity, EntityLivingBase target, Vec3d coordinates, int count,
            double range, int delay, Vec3d offset, boolean held, String weaponName, String ammoName, String bulletName,
            float damage, float headshot, boolean delayed) {
        return submit(entity, target, coordinates, count, range, delay, offset, held, weaponName, ammoName, bulletName,
                damage, headshot, delayed, null);
    }
    private static boolean submit(EntityLivingBase entity, EntityLivingBase target, Vec3d coordinates, int count,
            double range, int delay, Vec3d offset, boolean held, String weaponName, String ammoName, String bulletName,
            float damage, float headshot, boolean delayed, java.util.List<PacketGunFire.Hit> clientSuggestions) {
        return submitInternal(entity, target, coordinates, null, null, count, range, delay, offset, held, weaponName,
                ammoName, bulletName, damage, headshot, delayed, clientSuggestions, false);
    }
    private static boolean submit(EntityLivingBase entity, EntityLivingBase target, java.util.List<Vec3d> coordinateSequence, int count,
            double range, int delay, Vec3d offset, boolean held, String weaponName, String ammoName, String bulletName,
            float damage, float headshot, boolean delayed, java.util.List<PacketGunFire.Hit> clientSuggestions) {
        Vec3d first = coordinateSequence == null || coordinateSequence.isEmpty() ? null : coordinateSequence.get(0);
        return submitInternal(entity, target, first, coordinateSequence, null, count, range, delay, offset, held, weaponName,
                ammoName, bulletName, damage, headshot, delayed, clientSuggestions, false);
    }
    private static boolean submit(EntityLivingBase entity, EntityLivingBase target, java.util.List<Vec3d> coordinateSequence,
            Vec3d fixedOrigin, int count, double range, int delay, Vec3d offset, boolean held, String weaponName,
            String ammoName, String bulletName, float damage, float headshot, boolean delayed,
            java.util.List<PacketGunFire.Hit> clientSuggestions, boolean debugTrace) {
        Vec3d first = coordinateSequence == null || coordinateSequence.isEmpty() ? null : coordinateSequence.get(0);
        return submitInternal(entity, target, first, coordinateSequence, fixedOrigin, count, range, delay, offset, held,
                weaponName, ammoName, bulletName, damage, headshot, delayed, clientSuggestions, debugTrace);
    }
    private static boolean submitInternal(EntityLivingBase entity, EntityLivingBase target, Vec3d coordinates,
            java.util.List<Vec3d> coordinateSequence, Vec3d fixedOrigin, int count, double range, int delay, Vec3d offset, boolean held,
            String weaponName, String ammoName, String bulletName, float damage, float headshot, boolean delayed,
            java.util.List<PacketGunFire.Hit> clientSuggestions, boolean debugTrace) {
        return submitInternal(entity, target, coordinates, coordinateSequence, fixedOrigin, count, range, delay, offset,
                held, weaponName, ammoName, bulletName, damage, headshot, delayed, clientSuggestions, debugTrace, null);
    }
    private static boolean submitInternal(EntityLivingBase entity, EntityLivingBase target, Vec3d coordinates,
            java.util.List<Vec3d> coordinateSequence, Vec3d fixedOrigin, int count, double range, int delay, Vec3d offset, boolean held,
            String weaponName, String ammoName, String bulletName, float damage, float headshot, boolean delayed,
            java.util.List<PacketGunFire.Hit> clientSuggestions, boolean debugTrace, Float spreadOverride) {
        return submitInternal(entity, target, coordinates, coordinateSequence, fixedOrigin, count, range, delay, offset,
                held, weaponName, ammoName, bulletName, damage, headshot, delayed, clientSuggestions, debugTrace, spreadOverride, null);
    }
    private static boolean submitInternal(EntityLivingBase entity, EntityLivingBase target, Vec3d coordinates,
            java.util.List<Vec3d> coordinateSequence, Vec3d fixedOrigin, int count, double range, int delay, Vec3d offset, boolean held,
            String weaponName, String ammoName, String bulletName, float damage, float headshot, boolean delayed,
            java.util.List<PacketGunFire.Hit> clientSuggestions, boolean debugTrace, Float spreadOverride, java.util.List<Vec3d> origins) {
        if (!serverEntity(entity) || count < 1 || count > 1024 || delay < 0 || delay > 72000
                || !Double.isFinite(range) || range < 0 || !ProjectileAPI.finite(offset)
                || !Float.isFinite(damage) || !Float.isFinite(headshot) || damage < 0 || headshot < 0
                || (coordinates != null && !ProjectileAPI.finite(coordinates)) || tasks.containsKey(entity.getUniqueID())) return false;
        if (deadline(entity) > entity.world.getTotalWorldTime() + delay) return false;
        if (target != null && (!isAlive(target) || target.world != entity.world)) return false;
        if (clientSuggestions != null && (!(entity instanceof EntityPlayer) || clientSuggestions.size() > 64)) return false;
        ItemStack stack = held ? entity.getHeldItemMainhand() : createWeaponStack(weaponName, ammoName, bulletName);
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemGun)) return false;
        ItemGun gun = (ItemGun) stack.getItem();
        if (gun.type == null || gun.type.roundsPerMin <= 0 || !Float.isFinite(gun.type.roundsPerMin)) return false;
        ShootTask task = coordinateSequence == null
                ? new ShootTask(entity, target, coordinates, fixedOrigin, offset, stack, held, range, count, delay, damage, headshot, clientSuggestions)
                : new ShootTask(entity, target, coordinateSequence, fixedOrigin, offset, stack, held, range, count, delay, damage, headshot, clientSuggestions);
        task.debugTrace = debugTrace;
        task.spreadOverride = spreadOverride;
        task.originSequence = origins == null ? null : new java.util.ArrayList<>(origins);
        if (origins != null) for (int i = 0; i < origins.size(); i++)
            if (origins.get(i).distanceTo(coordinateSequence.get(i)) > range) return false;
        if (!validTask(task)) return false;
        Event pre;
        if (delayed) {
            Vec3d aim = target != null ? target.getPositionEyes(1) : coordinates;
            pre = new EntityDelayedShootEvent(entity.getUniqueID(), entity, target == null ? null : target.getUniqueID(),
                    target, aim.x, aim.y, aim.z, stack, gun, count, held, weaponName, ammoName, bulletName,
                    range, delay, (float) offset.x, (float) offset.y, (float) offset.z, coordinates != null);
        } else if (target != null) {
            pre = new EntityTargetShootEvent(entity.getUniqueID(), entity, target.getUniqueID(), target, stack, gun,
                    count, held, weaponName, ammoName, bulletName, range);
        } else {
            pre = new EntityShootEvent(entity.getUniqueID(), entity, stack, gun, count, held, weaponName, ammoName, bulletName);
        }
        if (MinecraftForge.EVENT_BUS.post(pre)) return false;
        if (delay == 0) {
            if (deadline(entity) > entity.world.getTotalWorldTime()) return false;
            if (!fire(task)) return false;
            task.remaining--; task.fired++;
            task.nextTick += interval(task);
            nextShotTicks.put(entity.getUniqueID(), new Cooldown(task.world, task.nextTick));
        }
        if (task.remaining > 0) tasks.put(entity.getUniqueID(), task);
        if (delay > 0) {
            Vec3d aim = target != null ? target.getPositionEyes(1) : coordinates;
            ModularWarfare.NETWORK.sendToAllAround(new PacketDelayedShoot(entity.getEntityId(), target == null ? -1 : target.getEntityId(),
                    aim.x, aim.y, aim.z, (float) offset.x, (float) offset.y, (float) offset.z, delay, coordinates != null),
                    entity.posX, entity.posY, entity.posZ, 256, entity.dimension);
        }
        return true;
    }

    /** Legacy argument order: weapon, magazine (ammo registry), bullet (bullet registry).
     * Direct-bullet weapons accept null for magazine. The returned stack is independent of held equipment. */
    public static ItemStack createWeaponStack(String weaponName, String magazineName, String bulletName) {
        if (weaponName == null || bulletName == null) return ItemStack.EMPTY;
        ItemGun gun = ModularWarfare.gunTypes.get(weaponName);
        ItemBullet bullet = ModularWarfare.bulletTypes.get(bulletName);
        if (gun == null || gun.type == null || bullet == null || gun.type.fireModes == null || gun.type.fireModes.length == 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(gun);
        net.minecraft.nbt.NBTTagCompound tag = new net.minecraft.nbt.NBTTagCompound();
        stack.setTagCompound(tag);
        tag.setString("firemode", gun.type.fireModes[0].name().toLowerCase(java.util.Locale.ROOT));
        if (gun.type.acceptedAmmo != null) {
            ItemAmmo magazine = magazineName == null ? null : ModularWarfare.ammoTypes.get(magazineName);
            if (magazine == null || !java.util.Arrays.asList(gun.type.acceptedAmmo).contains(magazineName)
                    || magazine.type.subAmmo == null || !java.util.Arrays.asList(magazine.type.subAmmo).contains(bulletName)) return ItemStack.EMPTY;
            ItemStack ammoStack = new ItemStack(magazine);
            net.minecraft.nbt.NBTTagCompound ammo = new net.minecraft.nbt.NBTTagCompound();
            ammoStack.setTagCompound(ammo);
            ammo.setInteger("ammocount", magazine.type.ammoCapacity);
            if (magazine.type.magazineCount > 1) {
                ammo.setInteger("magcount", 1);
                for (int i = 1; i <= magazine.type.magazineCount; i++) ammo.setInteger("ammocount" + i, magazine.type.ammoCapacity);
            }
            ammo.setTag("bullet", new ItemStack(bullet).writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            tag.setTag("ammo", ammoStack.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        } else if (gun.type.acceptedBullets != null && java.util.Arrays.asList(gun.type.acceptedBullets).contains(bulletName)) {
            tag.setTag("bullet", new ItemStack(bullet).writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            tag.setInteger("ammocount", gun.type.internalAmmoStorage == null ? 1 : gun.type.internalAmmoStorage);
        } else return ItemStack.EMPTY;
        return stack;
    }

    private static boolean fire(ShootTask task) {
        if (!validTask(task)) return false;
        EntityLivingBase entity = task.shooter;
        ItemGun gun = (ItemGun) task.stack.getItem();
        Vec3d origin = shotOrigin(task);
        Vec3d aim = task.target != null ? task.target.getPositionEyes(1)
                : task.coordinateSequence == null || task.coordinateSequence.isEmpty() ? task.coordinates
                : task.coordinateSequence.get(Math.min(task.fired, task.coordinateSequence.size() - 1));
        float pitch = entity.rotationPitch, yaw = entity.rotationYaw;
        if (aim != null) {
            Vec3d direction = aim.subtract(origin).normalize();
            yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
            pitch = (float) Math.toDegrees(Math.asin(-direction.y));
            if (!(entity instanceof EntityPlayer)) {
                entity.rotationPitch = pitch; entity.rotationYaw = yaw;
                entity.rotationYawHead = yaw; entity.renderYawOffset = yaw;
            }
        }
        WeaponFireMode mode = GunType.getFireMode(task.stack);
        if (mode == null) mode = gun.type.fireModes[0];
        // Legacy zero means use content-pack damage. Do not send zero as a damage override.
        FireData data = task.clientSuggestions != null
                ? FireData.buildServer(pitch, yaw, task.world, task.stack, gun, mode, task.held,
                        task.damage > 0 ? task.damage : null, task.headshot > 0 ? task.headshot : null, task.clientSuggestions)
                : FireData.buildServer(pitch, yaw, task.world, task.stack, gun, mode, task.held,
                        task.damage > 0 ? task.damage : null, task.headshot > 0 ? task.headshot : null);
        data.shotOrigin = origin;
        data.spreadOverride = task.spreadOverride;
        if (task.spreadOverride != null) data.weaponRange = task.range;
        SKILL_SHOT.set(Boolean.TRUE);
        DEBUG_SHOT.set(task.debugTrace);
        try { return FireManager.fire(entity, data); }
        finally { SKILL_SHOT.remove(); DEBUG_SHOT.remove(); }
    }

    private static boolean validTask(ShootTask task) {
        if (!isAlive(task.shooter) || task.shooter.world != task.world
                || task.world.getEntityByID(task.shooter.getEntityId()) != task.shooter) return false;
        if (task.held && (task.shooter.getHeldItemMainhand() != task.stack || !ItemGun.hasNextShot(task.stack))) return false;
        if (task.target != null && (!isAlive(task.target) || task.target.world != task.world
                || task.world.getEntityByID(task.target.getEntityId()) != task.target)) return false;
        Vec3d aim = task.target != null ? task.target.getPositionEyes(1)
                : task.coordinateSequence == null || task.coordinateSequence.isEmpty() ? task.coordinates
                : task.coordinateSequence.get(Math.min(task.fired, task.coordinateSequence.size() - 1));
        Vec3d origin = shotOrigin(task);
        return aim == null || origin.distanceTo(aim) <= task.range;
    }

    private static Vec3d shotOrigin(ShootTask task) {
        return (task.originSequence != null ? task.originSequence.get(Math.min(task.fired, task.originSequence.size() - 1))
                : task.fixedOrigin == null ? task.shooter.getPositionEyes(1) : task.fixedOrigin).add(task.offset);
    }

    private static double interval(ShootTask task) {
        return Math.max(1.0 / 16.0, 1200.0 / ((ItemGun) task.stack.getItem()).type.roundsPerMin);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.world.isRemote || event.phase != TickEvent.Phase.END) return;
        double now = event.world.getTotalWorldTime();
        nextShotTicks.entrySet().removeIf(entry -> entry.getValue().world == event.world && entry.getValue().tick <= now);
        for (java.util.Map.Entry<UUID, ShootTask> entry : tasks.entrySet()) {
            ShootTask task = entry.getValue();
            if (task.world != event.world) continue;
            if (!validTask(task)) { tasks.remove(entry.getKey(), task); continue; }
            int budget = 16;
            while (task.remaining > 0 && now >= task.nextTick && budget-- > 0) {
                if (!fire(task)) { task.remaining = 0; break; }
                task.remaining--; task.fired++;
                task.nextTick += interval(task);
                nextShotTicks.put(entry.getKey(), new Cooldown(task.world, task.nextTick));
            }
            if (task.remaining <= 0) tasks.remove(entry.getKey(), task);
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.getWorld().isRemote) return;
        tasks.entrySet().removeIf(entry -> entry.getValue().world == event.getWorld());
        // No world-independent tick deadlines may survive a server/world reload.
        nextShotTicks.entrySet().removeIf(entry -> entry.getValue().world == event.getWorld());
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        cancelShooting(event.player);
        nextShotTicks.remove(event.player.getUniqueID());
    }

    public static boolean cancelShooting(EntityLivingBase entity) {
        if (!serverEntity(entity)) return false;
        boolean removed = tasks.remove(entity.getUniqueID()) != null;
        if (removed) ModularWarfare.NETWORK.sendToAllAround(new PacketDelayedShoot(entity.getEntityId(), -1,
                0, 0, 0, 0, 0, 0, 0, true), entity.posX, entity.posY, entity.posZ, 256, entity.dimension);
        return removed;
    }

    public static boolean canEntityShoot(EntityLivingBase entity, boolean useHeldWeapon) {
        if (!serverEntity(entity) || tasks.containsKey(entity.getUniqueID())
                || deadline(entity) > entity.world.getTotalWorldTime()) return false;
        ItemStack stack = entity.getHeldItemMainhand();
        return !useHeldWeapon || (!stack.isEmpty() && stack.getItem() instanceof ItemGun && ItemGun.hasNextShot(stack));
    }
    public static boolean canEntityShoot(EntityLivingBase entity) { return canEntityShoot(entity, true); }

    /** Remaining cooldown in nominal milliseconds (20 TPS); -1 for invalid server entity. */
    public static long getEntityShootCooldown(EntityLivingBase entity) {
        if (!serverEntity(entity)) return -1;
        return (long) Math.ceil(Math.max(0, deadline(entity) - entity.world.getTotalWorldTime()) * 50);
    }

    private static boolean isAlive(EntityLivingBase entity) { return entity != null && !entity.isDead && entity.getHealth() > 0; }
    private static boolean serverEntity(EntityLivingBase entity) {
        return isAlive(entity) && entity.world instanceof net.minecraft.world.WorldServer
                && entity.getServer() != null && entity.getServer().isCallingFromMinecraftThread();
    }
    private static EntityLivingBase findEntity(UUID uuid) {
        net.minecraft.server.MinecraftServer server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
        if (uuid == null || server == null || !server.isCallingFromMinecraftThread()) return null;
        for (net.minecraft.world.WorldServer world : net.minecraftforge.common.DimensionManager.getWorlds()) {
            Entity entity = world.getEntityFromUuid(uuid);
            if (entity instanceof EntityLivingBase) return (EntityLivingBase) entity;
        }
        return null;
    }
    public static Vec3d getServerDefaultAccuracy(float pitch, float yaw, float accuracy, Random random) {
        float spreadPitch = random.nextFloat() * accuracy, spreadYaw = random.nextFloat() * accuracy;
        return new Vec3d(random.nextBoolean() ? spreadYaw : -spreadYaw, random.nextBoolean() ? spreadPitch : -spreadPitch, 100)
                .normalize().rotatePitch((float) Math.toRadians(-pitch)).rotateYaw((float) Math.toRadians(-yaw));
    }
    public static float calculateServerAccuracy(ItemGun item, EntityLivingBase entity) {
        if (item.type == null) return 1;
        float accuracy = item.type.bulletSpread;
        if (entity.posX != entity.lastTickPosX || entity.posZ != entity.lastTickPosZ) accuracy += item.type.accuracyMoveOffset;
        if (!entity.onGround) accuracy += item.type.accuracyHoverOffset;
        return Math.max(0, accuracy);
    }
}
