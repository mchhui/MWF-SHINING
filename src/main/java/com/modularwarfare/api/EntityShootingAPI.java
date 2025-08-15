package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.guns.GunType;
import com.modularwarfare.common.guns.ItemGun;
import com.modularwarfare.common.guns.WeaponFireMode;
import com.modularwarfare.common.guns.manager.ShotManager;
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
    
    static {
        MinecraftForge.EVENT_BUS.register(EntityShootingAPI.class);
    }
    
    // ==================== 事件类定义 ====================
    
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
    
    // ==================== 内部类定义 ====================
    
    private static class DelayedShootTask {
        public final EntityLivingBase entity;
        public final EntityLivingBase target;
        public final double targetX, targetY, targetZ;
        public final ItemStack weaponStack;
        public final ItemGun weapon;
        public int shotCount;
        public final double maxDistance;
        public final int delayTicks;
        public final float offsetX, offsetY, offsetZ;
        public final boolean isCoordinateShoot;
        public final boolean useHeldWeapon;
        public final float customDamage;
        public final float customHeadshotBonus;
        public int remainingTicks;
        public boolean rayStarted;
        public long shootIntervalMs;
        public long nextShootTime;
        
        public DelayedShootTask(EntityLivingBase entity, EntityLivingBase target, double targetX, double targetY, double targetZ,
                              ItemStack weaponStack, ItemGun weapon, int shotCount, double maxDistance,
                              int delayTicks, float offsetX, float offsetY, float offsetZ, boolean isCoordinateShoot, boolean useHeldWeapon, float customDamage, float customHeadshotBonus) {
            this.entity = entity;
            this.target = target;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.weaponStack = weaponStack;
            this.weapon = weapon;
            this.shotCount = shotCount;
            this.maxDistance = maxDistance;
            this.delayTicks = delayTicks;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.isCoordinateShoot = isCoordinateShoot;
            this.useHeldWeapon = useHeldWeapon;
            this.customDamage = customDamage;
            this.customHeadshotBonus = customHeadshotBonus;
            this.remainingTicks = delayTicks;
            this.rayStarted = false;
            this.shootIntervalMs = 0;
            this.nextShootTime = 0;
        }
    }
    
    private static class WeaponConfig {
        public final ItemStack weaponStack;
        public final ItemGun weapon;
        public final boolean useHeldWeapon;
        public final String specifiedWeaponName;
        public final String specifiedAmmoName;
        public final String specifiedMagazineName;
        
        public WeaponConfig(ItemStack weaponStack, ItemGun weapon, boolean useHeldWeapon,
                          String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
            this.weaponStack = weaponStack;
            this.weapon = weapon;
            this.useHeldWeapon = useHeldWeapon;
            this.specifiedWeaponName = specifiedWeaponName;
            this.specifiedAmmoName = specifiedAmmoName;
            this.specifiedMagazineName = specifiedMagazineName;
        }
    }
    
    // ==================== 静态字段 ====================
    
    private static final ConcurrentHashMap<UUID, DelayedShootTask> delayedShootTasks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> entityLastShootTime = new ConcurrentHashMap<>();
    
    // ==================== 工具方法 ====================
    
    public static Vec3d getServerDefaultAccuracy(float pitch, float yaw, final float accuracy, final Random rand) {
        final float randAccPitch = rand.nextFloat() * accuracy;
        final float randAccYaw = rand.nextFloat() * accuracy;
        Vec3d vec3d = new Vec3d(rand.nextBoolean() ? randAccYaw : (-randAccYaw), 
                               rand.nextBoolean() ? randAccPitch : (-randAccPitch), 
                               100).normalize();
        return vec3d.rotatePitch((float)(-pitch * Math.PI / 180))
                   .rotateYaw((float)(-yaw * Math.PI / 180));
    }
    
    public static float calculateServerAccuracy(final ItemGun item, final EntityLivingBase entity) {
        final GunType gun = item.type;
        if (gun == null) {
            return 1.0f;
        }
        
        float acc = gun.bulletSpread;
        
        if (entity.posX != entity.lastTickPosX || entity.posZ != entity.lastTickPosZ) {
            acc += gun.accuracyMoveOffset;
        }
        
        if (!entity.onGround) {
            acc += gun.accuracyHoverOffset;
        }
        
        if (acc < 0) {
            acc = 0;
        }
        
        return acc;
    }
    
    private static float[] calculateTargetAngles(EntityLivingBase entity, EntityLivingBase target) {
        Vec3d entityPos = entity.getPositionEyes(1.0f);
        Vec3d targetPos = target.getPositionEyes(1.0f);
        
        Vec3d direction = targetPos.subtract(entityPos).normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        
        return new float[]{pitch, yaw};
    }
    
    private static float[] calculateCoordinateAngles(EntityLivingBase entity, double targetX, double targetY, double targetZ) {
        Vec3d entityPos = entity.getPositionEyes(1.0f);
        Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);
        
        Vec3d direction = targetPos.subtract(entityPos).normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        
        return new float[]{pitch, yaw};
    }
    
    private static boolean isEntityFacingTarget(EntityLivingBase entity, EntityLivingBase target, float tolerance) {
        float[] targetAngles = calculateTargetAngles(entity, target);
        float currentPitch = entity.rotationPitch;
        float currentYaw = entity.rotationYaw;
        
        float pitchDiff = Math.abs(targetAngles[0] - currentPitch);
        float yawDiff = Math.abs(targetAngles[1] - currentYaw);
        
        if (yawDiff > 180) {
            yawDiff = 360 - yawDiff;
        }
        
        return pitchDiff <= tolerance && yawDiff <= tolerance;
    }
    
    private static boolean forceEntityFaceTargetAndWait(EntityLivingBase entity, EntityLivingBase target, int maxWaitTicks) {
        if (entity instanceof EntityPlayer) {
            return true;
        }
        
        float[] targetAngles = calculateTargetAngles(entity, target);
        entity.rotationPitch = targetAngles[0];
        entity.rotationYaw = targetAngles[1];
        entity.rotationYawHead = targetAngles[1];
        entity.renderYawOffset = targetAngles[1];
        
        int waitTicks = 0;
        while (!isEntityFacingTarget(entity, target, 5.0f) && waitTicks < maxWaitTicks) {
            waitTicks++;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return isEntityFacingTarget(entity, target, 5.0f);
    }
    
    private static void forceEntityFaceTarget(EntityLivingBase entity, EntityLivingBase target) {
        if (entity instanceof EntityPlayer) {
            return;
        }
        
        float[] angles = calculateTargetAngles(entity, target);
        entity.rotationPitch = angles[0];
        entity.rotationYaw = angles[1];
        entity.rotationYawHead = angles[1];
        entity.renderYawOffset = angles[1];
    }
    
    private static EntityLivingBase findTargetEntity(UUID targetUUID) {
        for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
            for (Entity e : w.loadedEntityList) {
                if (e instanceof EntityLivingBase && e.getUniqueID().equals(targetUUID)) {
                    return (EntityLivingBase) e;
                }
            }
        }
        return null;
    }
    
    private static EntityLivingBase findEntity(UUID entityUUID) {
        for (World w : net.minecraftforge.common.DimensionManager.getWorlds()) {
            for (Entity e : w.loadedEntityList) {
                if (e instanceof EntityLivingBase && e.getUniqueID().equals(entityUUID)) {
                    return (EntityLivingBase) e;
                }
            }
        }
        return null;
    }
    
    private static boolean isValidEntity(EntityLivingBase entity) {
        return entity != null && !entity.isDead && entity.getHealth() > 0;
    }
    
    private static WeaponConfig createWeaponConfig(EntityLivingBase entity, boolean useHeldWeapon,
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
        ItemStack weaponStack = null;
        ItemGun weapon = null;
        
        if (useHeldWeapon) {
            weaponStack = entity.getHeldItemMainhand();
            if (weaponStack.isEmpty() || !(weaponStack.getItem() instanceof ItemGun)) {
                return null;
            }
            weapon = (ItemGun) weaponStack.getItem();
        } else {
            if (specifiedWeaponName == null || specifiedWeaponName.isEmpty()) {
                ModularWarfare.LOGGER.warn("Weapon name not specified, must provide weapon name when useHeldWeapon is false");
                return null;
            }
            
            weapon = ModularWarfare.gunTypes.get(specifiedWeaponName);
            if (weapon == null) {
                ModularWarfare.LOGGER.warn("Cannot find specified weapon: {}", specifiedWeaponName);
                return null;
            }
            
            if (specifiedAmmoName == null || specifiedAmmoName.isEmpty()) {
                ModularWarfare.LOGGER.warn("Ammo name not specified, must provide ammo name when useHeldWeapon is false");
                return null;
            }
            
            if (specifiedMagazineName == null || specifiedMagazineName.isEmpty()) {
                ModularWarfare.LOGGER.warn("Bullet name not specified, must provide bullet name when useHeldWeapon is false");
                return null;
            }
            
            weaponStack = new ItemStack(weapon);
            
            if (weaponStack.getTagCompound() == null) {
                weaponStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            }
            
            weaponStack.getTagCompound().setString("firemode", weapon.type.fireModes[0].name().toLowerCase());
            
            ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(specifiedAmmoName);
            if (itemAmmo == null) {
                ModularWarfare.LOGGER.warn("Cannot find specified ammo: {}", specifiedAmmoName);
                return null;
            }
            
            ItemBullet itemBullet = ModularWarfare.bulletTypes.get(specifiedMagazineName);
            if (itemBullet == null) {
                ModularWarfare.LOGGER.warn("Cannot find specified bullet: {}", specifiedMagazineName);
                return null;
            }
            
            ItemStack ammoStack = new ItemStack(itemAmmo);
            if (ammoStack.getTagCompound() == null) {
                ammoStack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
            }
            
            ammoStack.getTagCompound().setInteger("ammocount", itemAmmo.type.ammoCapacity);
            
            ItemStack bulletStack = new ItemStack(itemBullet);
            ammoStack.getTagCompound().setTag("bullet", bulletStack.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
            
            weaponStack.getTagCompound().setTag("ammo", ammoStack.writeToNBT(new net.minecraft.nbt.NBTTagCompound()));
        }
        
        return new WeaponConfig(weaponStack, weapon, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
    }
    
    private static boolean executeSingleShot(EntityLivingBase entity, ItemStack weaponStack, ItemGun weapon, boolean useHeldWeapon, float customDamage, float customHeadshotBonus) {
        if (entity == null) {
            return false;
        }
        
        if (weapon == null || weaponStack == null) {
            return true;
        }
        
        GunType gunType = weapon.type;
        if (gunType == null) {
            return false;
        }
        
        if (useHeldWeapon) {
            if (!ItemGun.hasNextShot(weaponStack)) {
                return false;
            }
        }
        
        long currentTime = System.currentTimeMillis();
        String entityKey = entity.getUniqueID().toString();
        
        Long lastShootTime = entityLastShootTime.get(entityKey);
        if (lastShootTime != null) {
            long shootInterval = (long) (60.0 * 1000.0 / gunType.roundsPerMin);
            
            if (currentTime - lastShootTime < shootInterval) {
                return false;
            }
        }
        
        entityLastShootTime.put(entityKey, currentTime);
        
        WeaponFireMode fireMode = WeaponFireMode.SEMI;
        if (entity instanceof EntityPlayer) {
            fireMode = GunType.getFireMode(weaponStack);
        } else {
            if (gunType.fireModes != null && gunType.fireModes.length > 0) {
                fireMode = gunType.fireModes[0];
            }
        }
        
        float rotationPitch = entity.rotationPitch;
        float rotationYaw = entity.rotationYaw;
        
        if (!(entity instanceof EntityPlayer)) {
            float accuracy = calculateServerAccuracy(weapon, entity);
            Vec3d scatteredDirection = getServerDefaultAccuracy(rotationPitch, rotationYaw, accuracy, entity.world.rand);
            
            double x = scatteredDirection.x;
            double y = scatteredDirection.y;
            double z = scatteredDirection.z;
            
            rotationYaw = (float) Math.toDegrees(Math.atan2(-x, z));
            
            double horizontalDistance = Math.sqrt(x * x + z * z);
            rotationPitch = (float) Math.toDegrees(Math.atan2(-y, horizontalDistance));
        }
        
        if (!entity.world.isRemote) {
            boolean shotSuccess = ShotManager.fireServerForEntity(
                entity, rotationPitch, rotationYaw, entity.world, weaponStack, weapon, fireMode,
                gunType.fireTickDelay, gunType.recoilPitch, gunType.recoilYaw, 
                gunType.recoilAimReducer, gunType.bulletSpread, useHeldWeapon, customDamage, customHeadshotBonus
            );
            
            if (!shotSuccess) {
                return false;
            }
        } else {
            ModularWarfare.NETWORK.sendToServer(new PacketGunFire(
                gunType.internalName, gunType.fireTickDelay, gunType.recoilPitch, 
                gunType.recoilYaw, gunType.recoilAimReducer, gunType.bulletSpread,
                rotationPitch, rotationYaw
            ));
        }
        
        return true;
    }
    
    // 任务内执行：忽略实体冷却，允许同tick内多发
    private static boolean executeScheduledShot(EntityLivingBase entity, ItemStack weaponStack, ItemGun weapon, boolean useHeldWeapon) {
        if (entity == null) {
            return false;
        }
        if (weapon == null || weaponStack == null) {
            return true;
        }
        GunType gunType = weapon.type;
        if (gunType == null) {
            return false;
        }
        if (useHeldWeapon) {
            if (!ItemGun.hasNextShot(weaponStack)) {
                return false;
            }
        }
        WeaponFireMode fireMode = WeaponFireMode.SEMI;
        if (entity instanceof EntityPlayer) {
            fireMode = GunType.getFireMode(weaponStack);
        } else if (gunType.fireModes != null && gunType.fireModes.length > 0) {
            fireMode = gunType.fireModes[0];
        }
        float rotationPitch = entity.rotationPitch;
        float rotationYaw = entity.rotationYaw;
        if (!(entity instanceof EntityPlayer)) {
            float accuracy = calculateServerAccuracy(weapon, entity);
            Vec3d scatteredDirection = getServerDefaultAccuracy(rotationPitch, rotationYaw, accuracy, entity.world.rand);
            double x = scatteredDirection.x;
            double y = scatteredDirection.y;
            double z = scatteredDirection.z;
            rotationYaw = (float) Math.toDegrees(Math.atan2(-x, z));
            double horizontalDistance = Math.sqrt(x * x + z * z);
            rotationPitch = (float) Math.toDegrees(Math.atan2(-y, horizontalDistance));
        }
        if (!entity.world.isRemote) {
            boolean shotSuccess = ShotManager.fireServerForEntity(
                entity, rotationPitch, rotationYaw, entity.world, weaponStack, weapon, fireMode,
                gunType.fireTickDelay, gunType.recoilPitch, gunType.recoilYaw,
                gunType.recoilAimReducer, gunType.bulletSpread, useHeldWeapon, 0.0f, 0.0f
            );
            if (!shotSuccess) {
                return false;
            }
        } else {
            ModularWarfare.NETWORK.sendToServer(new PacketGunFire(
                gunType.internalName, gunType.fireTickDelay, gunType.recoilPitch,
                gunType.recoilYaw, gunType.recoilAimReducer, gunType.bulletSpread,
                rotationPitch, rotationYaw
            ));
        }
        return true;
    }
    
    // ==================== 公共API方法 ====================
    
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
        if (!isValidEntity(entity)) {
            return false;
        }
        
        WeaponConfig weaponConfig = createWeaponConfig(entity, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
        if (weaponConfig == null) {
            return false;
        }
        
        EntityShootEvent preEvent = new EntityShootEvent(
            entity.getUniqueID(), entity, weaponConfig.weaponStack, weaponConfig.weapon, shotCount, 
            useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName
        );
        MinecraftForge.EVENT_BUS.post(preEvent);
        
        if (preEvent.isCanceled()) {
            return false;
        }
        
        if (shotCount <= 1 || useHeldWeapon) {
            return executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon, customDamage, customHeadshotBonus);
        } else {
            GunType gunType = weaponConfig.weapon.type;
            long shootInterval = (long) (60.0 * 1000.0 / gunType.roundsPerMin);
            
            DelayedShootTask task = new DelayedShootTask(
                entity, null, 0, 0, 0,
                weaponConfig.weaponStack, weaponConfig.weapon, shotCount,
                0, 0, 0, 0, 0, true, useHeldWeapon, customDamage, customHeadshotBonus
            );
            task.shootIntervalMs = shootInterval;
            task.nextShootTime = System.currentTimeMillis();
            
            delayedShootTasks.put(UUID.randomUUID(), task);
            return true;
    }
    }
    
    public static boolean shootEntityAtTarget(UUID entityUUID, UUID targetUUID, int shotCount, double maxDistance,
                                            boolean useHeldWeapon, String specifiedWeaponName, 
                                            String specifiedAmmoName, String specifiedMagazineName) {
        EntityLivingBase entity = findEntity(entityUUID);
        if (entity == null) {
            ModularWarfare.LOGGER.warn("Cannot find shooting entity with UUID: {}", entityUUID);
            return false;
        }
        
        EntityLivingBase target = findTargetEntity(targetUUID);
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
        if (!isValidEntity(entity)) {
            ModularWarfare.LOGGER.warn("Shooting entity is invalid or dead");
            return false;
        }
        
        if (!isValidEntity(target)) {
            ModularWarfare.LOGGER.warn("Target entity is invalid or dead");
            return false;
        }
        
        double distance = entity.getDistance(target);
        if (distance > maxDistance) {
            ModularWarfare.LOGGER.warn("Target distance {} exceeds max shooting distance {}", distance, maxDistance);
            return false;
        }
        
        WeaponConfig weaponConfig = createWeaponConfig(entity, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
        if (weaponConfig == null) {
            return false;
        }
        
        EntityTargetShootEvent preEvent = new EntityTargetShootEvent(
            entity.getUniqueID(), entity, target.getUniqueID(), target, weaponConfig.weaponStack, weaponConfig.weapon, shotCount,
            useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, maxDistance
        );
        MinecraftForge.EVENT_BUS.post(preEvent);
        
        if (preEvent.isCanceled()) {
            return false;
        }
        
        forceEntityFaceTarget(entity, target);
        
        if (shotCount <= 1 || useHeldWeapon) {
            return executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon, customDamage, customHeadshotBonus);
        } else {
            GunType gunType = weaponConfig.weapon.type;
            long shootInterval = (long) (60.0 * 1000.0 / gunType.roundsPerMin);
            
            DelayedShootTask task = new DelayedShootTask(
                entity, target, target.posX, target.posY, target.posZ,
                weaponConfig.weaponStack, weaponConfig.weapon, shotCount,
                maxDistance, 0, 0, 0, 0, false, useHeldWeapon, customDamage, customHeadshotBonus
            );
            task.shootIntervalMs = shootInterval;
            task.nextShootTime = System.currentTimeMillis();
            
            delayedShootTasks.put(UUID.randomUUID(), task);
            return true;
    }
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
        if (!isValidEntity(entity)) {
            ModularWarfare.LOGGER.warn("Shooting entity is invalid or dead");
            return false;
        }
        
        double distance = entity.getDistance(targetX, targetY, targetZ);
        if (distance > maxDistance) {
            ModularWarfare.LOGGER.warn("Target distance {} exceeds max shooting distance {}", distance, maxDistance);
            return false;
        }
        
        WeaponConfig weaponConfig = createWeaponConfig(entity, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
        if (weaponConfig == null) {
            return false;
        }
        
        float[] angles = calculateCoordinateAngles(entity, targetX, targetY, targetZ);
        
        if (!(entity instanceof EntityPlayer)) {
            entity.rotationPitch = angles[0];
            entity.rotationYaw = angles[1];
            entity.rotationYawHead = angles[1];
            entity.renderYawOffset = angles[1];
        }
        
        if (shotCount <= 1 || useHeldWeapon) {
            return executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon, customDamage, customHeadshotBonus);
        } else {
            GunType gunType = weaponConfig.weapon.type;
            long shootInterval = (long) (60.0 * 1000.0 / gunType.roundsPerMin);
            
            DelayedShootTask task = new DelayedShootTask(
                entity, null, targetX, targetY, targetZ,
                weaponConfig.weaponStack, weaponConfig.weapon, shotCount,
                maxDistance, 0, 0, 0, 0, true, useHeldWeapon, customDamage, customHeadshotBonus
            );
            task.shootIntervalMs = shootInterval;
            task.nextShootTime = System.currentTimeMillis();
            
            delayedShootTasks.put(UUID.randomUUID(), task);
            return true;
    }
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
        if (!isValidEntity(entity)) {
            ModularWarfare.LOGGER.warn("Shooting entity is invalid or dead");
            return false;
        }
        
        if (!isValidEntity(target)) {
            ModularWarfare.LOGGER.warn("Target entity is invalid or dead");
            return false;
        }
        
        double distance = entity.getDistance(target);
        if (distance > maxDistance) {
            ModularWarfare.LOGGER.warn("Target distance {} exceeds max shooting distance {}", distance, maxDistance);
            return false;
        }
        
        forceEntityFaceTarget(entity, target);
        
        WeaponConfig weaponConfig = createWeaponConfig(entity, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
        if (weaponConfig == null) {
            return false;
        }
        
        EntityDelayedShootEvent preEvent = new EntityDelayedShootEvent(
            entity.getUniqueID(), entity, target.getUniqueID(), target, 
            target.posX, target.posY, target.posZ, weaponConfig.weaponStack, weaponConfig.weapon, shotCount,
            useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName, 
            maxDistance, delayTicks, offsetX, offsetY, offsetZ, false
        );
        MinecraftForge.EVENT_BUS.post(preEvent);
        
        if (preEvent.isCanceled()) {
            return false;
        }
        
        DelayedShootTask task = new DelayedShootTask(
            entity, target, target.posX, target.posY, target.posZ,
            weaponConfig.weaponStack, weaponConfig.weapon, shotCount, maxDistance, delayTicks,
            offsetX, offsetY, offsetZ, false, useHeldWeapon, customDamage, customHeadshotBonus
        );
        delayedShootTasks.put(entity.getUniqueID(), task);
        
        if (!entity.world.isRemote) {
            Vec3d targetEyePos = target.getPositionEyes(1.0f);
            ModularWarfare.NETWORK.sendToAllAround(
                new PacketDelayedShoot(
                    entity.getEntityId(), target.getEntityId(), targetEyePos.x, targetEyePos.y, targetEyePos.z,
                    offsetX, offsetY, offsetZ, delayTicks, false
                ),
                entity.posX, entity.posY, entity.posZ, 256.0f, entity.world.provider.getDimension()
            );
        }
        
        return true;
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
        if (!isValidEntity(entity)) {
            ModularWarfare.LOGGER.warn("Shooting entity is invalid or dead");
            return false;
        }
        
        double distance = entity.getDistance(targetX, targetY, targetZ);
        if (distance > maxDistance) {
            ModularWarfare.LOGGER.warn("Target distance {} exceeds max shooting distance {}", distance, maxDistance);
            return false;
        }
        
        float[] angles = calculateCoordinateAngles(entity, targetX, targetY, targetZ);
        
        if (!(entity instanceof EntityPlayer)) {
            entity.rotationPitch = angles[0];
            entity.rotationYaw = angles[1];
            entity.rotationYawHead = angles[1];
            entity.renderYawOffset = angles[1];
        }
        
        WeaponConfig weaponConfig = createWeaponConfig(entity, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
        if (weaponConfig == null) {
            return false;
        }
        
        EntityDelayedShootEvent preEvent = new EntityDelayedShootEvent(
            entity.getUniqueID(), entity, null, null, targetX, targetY, targetZ,
            weaponConfig.weaponStack, weaponConfig.weapon, shotCount, useHeldWeapon, specifiedWeaponName, 
            specifiedAmmoName, specifiedMagazineName, maxDistance, delayTicks,
            offsetX, offsetY, offsetZ, true
        );
        MinecraftForge.EVENT_BUS.post(preEvent);
        
        if (preEvent.isCanceled()) {
            return false;
        }
        
        DelayedShootTask task = new DelayedShootTask(
            entity, null, targetX, targetY, targetZ,
            weaponConfig.weaponStack, weaponConfig.weapon, shotCount, maxDistance, delayTicks,
            offsetX, offsetY, offsetZ, true, useHeldWeapon, customDamage, customHeadshotBonus
        );
        delayedShootTasks.put(entity.getUniqueID(), task);
        
        if (!entity.world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(
                new PacketDelayedShoot(
                    entity.getEntityId(), -1, targetX, targetY, targetZ,
                    offsetX, offsetY, offsetZ, delayTicks, true
                ),
                entity.posX, entity.posY, entity.posZ, 256.0f, entity.world.provider.getDimension()
            );
        }
        
        return true;
    }
    
    private static void executeDelayedShooting(DelayedShootTask task) {
        if (task.entity == null) {
            return;
        }
        
        if (task.weapon == null || task.weaponStack == null) {
            return;
        }
        
        GunType gunType = task.weapon.type;
        if (gunType == null) {
            return;
        }
        
        if (task.useHeldWeapon) {
            if (!ItemGun.hasNextShot(task.weaponStack)) {
                return;
            }
        }
        
        forceEntityFaceTarget(task.entity, task.target);
        
        WeaponFireMode fireMode = WeaponFireMode.SEMI;
        if (task.entity instanceof EntityPlayer) {
            fireMode = GunType.getFireMode(task.weaponStack);
        } else {
            if (gunType.fireModes != null && gunType.fireModes.length > 0) {
                fireMode = gunType.fireModes[0];
            }
        }
        
        float rotationPitch = task.entity.rotationPitch;
        float rotationYaw = task.entity.rotationYaw;
        
        if (!(task.entity instanceof EntityPlayer)) {
            float accuracy = calculateServerAccuracy(task.weapon, task.entity);
            Vec3d scatteredDirection = getServerDefaultAccuracy(rotationPitch, rotationYaw, accuracy, task.entity.world.rand);
            
            double x = scatteredDirection.x;
            double y = scatteredDirection.y;
            double z = scatteredDirection.z;
            
            rotationYaw = (float) Math.toDegrees(Math.atan2(-x, z));
            
            double horizontalDistance = Math.sqrt(x * x + z * z);
            rotationPitch = (float) Math.toDegrees(Math.atan2(-y, horizontalDistance));
        }
        
        if (!task.entity.world.isRemote) {
            boolean shotSuccess = ShotManager.fireServerForEntity(
                task.entity, rotationPitch, rotationYaw, task.entity.world, task.weaponStack, task.weapon, fireMode,
                gunType.fireTickDelay, gunType.recoilPitch, gunType.recoilYaw, 
                gunType.recoilAimReducer, gunType.bulletSpread, task.useHeldWeapon, task.customDamage, task.customHeadshotBonus
            );
            
            if (!shotSuccess) {
                return;
            }
        } else {
            ModularWarfare.NETWORK.sendToServer(new PacketGunFire(
                gunType.internalName, gunType.fireTickDelay, gunType.recoilPitch, 
                gunType.recoilYaw, gunType.recoilAimReducer, gunType.bulletSpread,
                rotationPitch, rotationYaw
            ));
        }
        
        if (task.shotCount > 1) {
            long shootInterval = (long) (60.0 * 1000.0 / gunType.roundsPerMin);
            task.nextShootTime = System.currentTimeMillis() + shootInterval;
        }
    }
    
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            long currentTime = System.currentTimeMillis();
            long cleanupThreshold = (long) (0.5 * 60 * 1000);
            
            entityLastShootTime.entrySet().removeIf(entry -> {
                return currentTime - entry.getValue() > cleanupThreshold;
            });
            
            delayedShootTasks.entrySet().removeIf(entry -> {
                DelayedShootTask task = entry.getValue();
                
                if (task.entity.isDead || task.entity.getHealth() <= 0) {
                    return true;
                }
                
                if (task.target != null && (task.target.isDead || task.target.getHealth() <= 0)) {
                    return true;
                }
                
                double distance;
                if (task.target != null) {
                    distance = task.entity.getDistance(task.target);
                } else {
                    distance = task.entity.getDistance(task.targetX, task.targetY, task.targetZ);
                }
                
                if (distance > task.maxDistance) {
                    return true;
                }
                
                if (task.delayTicks > 0) {
                    task.remainingTicks--;
                    if (task.remainingTicks <= 0) {
                        executeDelayedShooting(task);
                        return true;
                    }
                    return false;
                }
                
                if (task.shootIntervalMs > 0) {
                    long currentTimeMs = System.currentTimeMillis();
                    if (currentTimeMs >= task.nextShootTime) {
                        int safetyCounter = 0;
                        while (task.shotCount > 0 && currentTimeMs >= task.nextShootTime) {
                            if (task.target != null) {
                                forceEntityFaceTarget(task.entity, task.target);
                            } else if (task.isCoordinateShoot) {
                                float[] angles = calculateCoordinateAngles(task.entity, task.targetX, task.targetY, task.targetZ);
                                if (!(task.entity instanceof EntityPlayer)) {
                                    task.entity.rotationPitch = angles[0];
                                    task.entity.rotationYaw = angles[1];
                                    task.entity.rotationYawHead = angles[1];
                                    task.entity.renderYawOffset = angles[1];
                                }
                            }
                            if (executeScheduledShot(task.entity, task.weaponStack, task.weapon, task.useHeldWeapon)) {
                                task.shotCount--;
                                task.nextShootTime += task.shootIntervalMs;
                                safetyCounter++;
                                if (safetyCounter > 100) { // 避免极端情况下的死循环
                                    break;
                                }
                            } else {
                                return true; // 执行失败，移除任务
                            }
                        }
                        if (task.shotCount <= 0) {
                            return true;
                        }
                        return false;
                    }
                    return false;
                }
                
                return false;
            });
        }
    }
    
    // ==================== 辅助方法 ====================
    
    public static boolean canEntityShoot(EntityLivingBase entity, boolean useHeldWeapon) {
        if (!isValidEntity(entity)) {
            return false;
        }
        
        if (useHeldWeapon) {
            ItemStack heldItem = entity.getHeldItemMainhand();
            if (!heldItem.isEmpty() && heldItem.getItem() instanceof ItemGun) {
                ItemGun weapon = (ItemGun) heldItem.getItem();
                return ItemGun.hasNextShot(heldItem);
            }
            return false;
        }
        
        return true;
    }
    
    public static boolean canEntityShoot(EntityLivingBase entity) {
        return canEntityShoot(entity, true);
    }
    
    public static long getEntityShootCooldown(EntityLivingBase entity) {
        if (!canEntityShoot(entity)) {
            return -1;
        }
        
        ItemStack heldItem = entity.getHeldItemMainhand();
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof ItemGun)) {
            return -1;
        }
        
        ItemGun weapon = (ItemGun) heldItem.getItem();
        GunType gunType = weapon.type;
        
        if (gunType == null) {
            return -1;
        }
        
        return (long) (60.0 * 1000.0 / gunType.roundsPerMin);
    }
} 