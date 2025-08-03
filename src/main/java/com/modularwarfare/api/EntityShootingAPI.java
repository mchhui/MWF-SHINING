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

/**
 * 生物射击API
 * 允许指定UUID的生物进行射击操作
 * 
 * @author ModularWarfare Team
 * @version 1.0
 */
public class EntityShootingAPI {
    
    // 静态初始化器，注册事件处理器
    static {
        MinecraftForge.EVENT_BUS.register(EntityShootingAPI.class);
    }
    
    // ==================== 事件类定义 ====================
    
    /**
     * 生物射击事件 - 射击前
     */
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
        
        // Getters
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
    
    /**
     * 带目标的射击事件 - 射击前
     */
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
        
        // Getters
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
    
    /**
     * 延迟射击事件 - 延迟射击前
     */
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
        
        // Getters
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
    
    /**
     * 延迟射击任务数据
     */
    private static class DelayedShootTask {
        public final EntityLivingBase entity;
        public final EntityLivingBase target;
        public final double targetX, targetY, targetZ;
        public final ItemStack weaponStack;
        public final ItemGun weapon;
        public final int shotCount;
        public final double maxDistance;
        public final int delayTicks;
        public final float offsetX, offsetY, offsetZ;
        public final boolean isCoordinateShoot;
        public final boolean useHeldWeapon;
        public int remainingTicks;
        public boolean rayStarted;
        
        public DelayedShootTask(EntityLivingBase entity, EntityLivingBase target, double targetX, double targetY, double targetZ,
                              ItemStack weaponStack, ItemGun weapon, int shotCount, double maxDistance,
                              int delayTicks, float offsetX, float offsetY, float offsetZ, boolean isCoordinateShoot, boolean useHeldWeapon) {
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
            this.remainingTicks = delayTicks;
            this.rayStarted = false;
        }
    }
    
    /**
     * 武器配置数据
     */
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
    
    // ==================== 工具方法 ====================
    
    /**
     * 服务端版本的默认散射计算
     */
    private static Vec3d getServerDefaultAccuracy(float pitch, float yaw, final float accuracy, final Random rand) {
        final float randAccPitch = rand.nextFloat() * accuracy;
        final float randAccYaw = rand.nextFloat() * accuracy;
        Vec3d vec3d = new Vec3d(rand.nextBoolean() ? randAccYaw : (-randAccYaw), 
                               rand.nextBoolean() ? randAccPitch : (-randAccPitch), 
                               100).normalize();
        return vec3d.rotatePitch((float)(-pitch * Math.PI / 180))
                   .rotateYaw((float)(-yaw * Math.PI / 180));
    }
    
    /**
     * 服务端版本的精度计算
     */
    private static float calculateServerAccuracy(final ItemGun item, final EntityLivingBase entity) {
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
    
    /**
     * 计算朝向目标的角度
     */
    private static float[] calculateTargetAngles(EntityLivingBase entity, EntityLivingBase target) {
        Vec3d entityPos = entity.getPositionEyes(1.0f);
        Vec3d targetPos = target.getPositionEyes(1.0f);
        
        Vec3d direction = targetPos.subtract(entityPos).normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        
        return new float[]{pitch, yaw};
    }
    
    /**
     * 计算朝向指定坐标的角度
     */
    private static float[] calculateCoordinateAngles(EntityLivingBase entity, double targetX, double targetY, double targetZ) {
        Vec3d entityPos = entity.getPositionEyes(1.0f);
        Vec3d targetPos = new Vec3d(targetX, targetY, targetZ);
        
        Vec3d direction = targetPos.subtract(entityPos).normalize();
        
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) Math.toDegrees(Math.asin(-direction.y));
        
        return new float[]{pitch, yaw};
    }
    
    /**
     * 检查实体是否面向目标（允许一定的角度误差）
     */
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
    
    /**
     * 强制实体面向目标并等待确认（仅对非玩家实体有效）
     */
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
    
    /**
     * 强制实体面向目标（仅对非玩家实体有效）
     */
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
    
    /**
     * 查找目标实体
     */
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
    
    /**
     * 查找实体
     */
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
    
    /**
     * 验证实体有效性
     */
    private static boolean isValidEntity(EntityLivingBase entity) {
        return entity != null && !entity.isDead && entity.getHealth() > 0;
    }
    
    /**
     * 创建武器配置
     */
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
    
    /**
     * 执行单次射击
     */
    private static boolean executeSingleShot(EntityLivingBase entity, ItemStack weaponStack, ItemGun weapon, boolean useHeldWeapon) {
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
                gunType.recoilAimReducer, gunType.bulletSpread, useHeldWeapon
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
    
    /**
     * 让指定UUID的生物进行射击
     */
    public static boolean shootEntity(UUID entityUUID, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
        EntityLivingBase entity = findEntity(entityUUID);
        if (entity == null) {
            ModularWarfare.LOGGER.warn("Cannot find entity with UUID: {}", entityUUID);
            return false;
        }
        
        return shootEntity(entity, shotCount, useHeldWeapon, specifiedWeaponName, specifiedAmmoName, specifiedMagazineName);
    }
    
    /**
     * 让指定实体进行射击
     */
    public static boolean shootEntity(EntityLivingBase entity, int shotCount, boolean useHeldWeapon, 
                                    String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
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
        
        boolean success = false;
        for (int i = 0; i < shotCount; i++) {
            if (executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon)) {
                success = true;
            }
        }
        return success;
    }
    
    /**
     * 让指定UUID的生物向目标进行射击
     */
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
    
    /**
     * 让指定实体向目标进行射击
     */
    public static boolean shootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                            double maxDistance, boolean useHeldWeapon, String specifiedWeaponName,
                                            String specifiedAmmoName, String specifiedMagazineName) {
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
        
        boolean success = false;
        for (int i = 0; i < shotCount; i++) {
            if (target.isDead || target.getHealth() <= 0) {
                break;
            }
            
            distance = entity.getDistance(target);
            if (distance > maxDistance) {
                break;
            }
            
            forceEntityFaceTarget(entity, target);
            
            if (executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon)) {
                success = true;
            }
        }
        
        return success;
    }
    
    /**
     * 让指定实体向指定坐标射击
     */
    public static boolean shootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ, 
                                                 int shotCount, double maxDistance, boolean useHeldWeapon, 
                                                 String specifiedWeaponName, String specifiedAmmoName, String specifiedMagazineName) {
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
        
        boolean success = false;
        for (int i = 0; i < shotCount; i++) {
            distance = entity.getDistance(targetX, targetY, targetZ);
            if (distance > maxDistance) {
                break;
            }
            
            angles = calculateCoordinateAngles(entity, targetX, targetY, targetZ);
            if (!(entity instanceof EntityPlayer)) {
                entity.rotationPitch = angles[0];
                entity.rotationYaw = angles[1];
                entity.rotationYawHead = angles[1];
                entity.renderYawOffset = angles[1];
            }
            
            if (executeSingleShot(entity, weaponConfig.weaponStack, weaponConfig.weapon, useHeldWeapon)) {
                success = true;
            }
        }
        
        return success;
    }
    
    /**
     * 延迟射击 - 向目标实体
     */
    public static boolean delayedShootEntityAtTarget(EntityLivingBase entity, EntityLivingBase target, int shotCount, 
                                                   double maxDistance, int delayTicks, float offsetX, float offsetY, float offsetZ,
                                                   boolean useHeldWeapon, String specifiedWeaponName, 
                                                   String specifiedAmmoName, String specifiedMagazineName) {
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
        
        // 强制实体面向目标（不等待）
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
            offsetX, offsetY, offsetZ, false, useHeldWeapon
        );
        delayedShootTasks.put(entity.getUniqueID(), task);
        
        if (!entity.world.isRemote) {
            // 使用目标的眼睛位置而不是脚下位置
            Vec3d targetEyePos = target.getPositionEyes(1.0f);
            ModularWarfare.NETWORK.sendToAllAround(
                new PacketDelayedShoot(
                    entity.getEntityId(), target.getEntityId(), targetEyePos.x, targetEyePos.y, targetEyePos.z,
                    offsetX, offsetY, offsetZ, delayTicks, false
                ),
                entity.posX, entity.posY, entity.posZ, 64.0f, entity.world.provider.getDimension()
            );
        }
        
        return true;
    }
    
    /**
     * 延迟射击 - 向指定坐标
     */
    public static boolean delayedShootEntityAtCoordinates(EntityLivingBase entity, double targetX, double targetY, double targetZ,
                                                        int shotCount, double maxDistance, int delayTicks, 
                                                        float offsetX, float offsetY, float offsetZ,
                                                        boolean useHeldWeapon, String specifiedWeaponName, 
                                                        String specifiedAmmoName, String specifiedMagazineName) {
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
            offsetX, offsetY, offsetZ, true, useHeldWeapon
        );
        delayedShootTasks.put(entity.getUniqueID(), task);
        
        if (!entity.world.isRemote) {
            ModularWarfare.NETWORK.sendToAllAround(
                new PacketDelayedShoot(
                    entity.getEntityId(), -1, targetX, targetY, targetZ,
                    offsetX, offsetY, offsetZ, delayTicks, true
                ),
                entity.posX, entity.posY, entity.posZ, 64.0f, entity.world.provider.getDimension()
            );
        }
        
        return true;
    }
    
    /**
     * 处理延迟射击任务的世界tick事件
     */
    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
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
                
                task.remainingTicks--;
                
                if (task.remainingTicks <= 0) {
                    if (task.target != null) {
                        forceEntityFaceTarget(task.entity, task.target);
                        
                        boolean success = false;
                        for (int i = 0; i < task.shotCount; i++) {
                            if (executeSingleShot(task.entity, task.weaponStack, task.weapon, task.useHeldWeapon)) {
                                success = true;
                            }
                        }
                    } else {
                        float[] angles = calculateCoordinateAngles(task.entity, task.targetX, task.targetY, task.targetZ);
                        if (!(task.entity instanceof EntityPlayer)) {
                            task.entity.rotationPitch = angles[0];
                            task.entity.rotationYaw = angles[1];
                            task.entity.rotationYawHead = angles[1];
                            task.entity.renderYawOffset = angles[1];
                        }
                        
                        boolean success = false;
                        for (int i = 0; i < task.shotCount; i++) {
                            if (executeSingleShot(task.entity, task.weaponStack, task.weapon, task.useHeldWeapon)) {
                                success = true;
                            }
                        }
                    }
                    
                    return true;
                }
                
                return false;
            });
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查实体是否可以射击
     */
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
    
    /**
     * 检查实体是否可以射击（兼容旧版本，默认检查手中武器）
     */
    public static boolean canEntityShoot(EntityLivingBase entity) {
        return canEntityShoot(entity, true);
    }
    
    /**
     * 获取实体的射击冷却时间
     */
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
    
    /**
     * 使用示例和说明：
     * 
     * 1. 让指定UUID的生物使用手中武器射击3次：
     *    EntityShootingAPI.shootEntity(entityUUID, 3, true, null, null, null);
     * 
     * 2. 让指定UUID的生物使用指定武器射击1次：
     *    EntityShootingAPI.shootEntity(entityUUID, 1, false, "ak47", "5.56x45", "30rnd_mag");
     * 
     * 3. 让指定UUID的生物向目标射击5次（距离限制50格）：
     *    EntityShootingAPI.shootEntityAtTarget(entityUUID, targetUUID, 5, 50.0, true, null, null, null);
     * 
     * 4. 让指定实体向目标射击3次（使用指定武器，距离限制30格）：
     *    EntityShootingAPI.shootEntityAtTarget(entity, target, 3, 30.0, false, "sniper", "7.62x51", "10rnd_mag");
     * 
     * 5. 检查实体是否可以射击：
     *    if (EntityShootingAPI.canEntityShoot(entity)) {
     *        // 执行射击逻辑
     *    }
     * 
     * 6. 获取射击冷却时间：
     *    long cooldown = EntityShootingAPI.getEntityShootCooldown(entity);
     *    if (cooldown > 0) {
     *        // 等待冷却时间
     *    }
     * 
     * 注意事项：
     * - 生物射击不会触发后坐力和屏幕晃动效果
     * - 非玩家实体使用服务端散射计算，基于RayUtil的默认散射算法
     * - 带目标射击会强制非玩家实体面向目标（包括头部和身体）
     * - 射击前会发送EntityShootEvent或EntityTargetShootEvent事件
     * - 支持指定武器、弹药和弹匣类型
     * - 自动处理客户端和服务端的射击逻辑
     * - 带目标射击会在每次射击前重新检查距离和目标状态
     */
} 