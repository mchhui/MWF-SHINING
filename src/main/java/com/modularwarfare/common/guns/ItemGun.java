package com.modularwarfare.common.guns;

import com.google.common.collect.Multimap;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.client.fpp.basic.renderers.RenderParameters;
import com.modularwarfare.client.handler.ClientTickHandler;
import com.modularwarfare.common.guns.manager.FireManager;
import com.modularwarfare.common.guns.manager.FireManager.FireData;
import com.modularwarfare.common.handler.ServerTickHandler;
import com.modularwarfare.common.network.PacketBulletHole;
import com.modularwarfare.common.network.PacketHitEffect;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import safx.SagerFX;
import safx.SAPackets;
import safx.packets.PacketSpawnParticle;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemAir;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemGun extends BaseItem {

    protected static final UUID MOVEMENT_SPEED_MODIFIER = UUID.fromString("99999999-4180-4865-B01B-BCCE9785ACA3");
    public static boolean canDryFireClient = true;
    public static boolean fireButtonHeld = false;
    public static boolean lastFireButtonHeld = false;
    public static final String LASER_ENABLED_NBT = "laser_enabled";
    public static final String TRANSFORM_STATE_NBT = "transform_state";
    public static final String LAST_TRANSFORM_STATE_NBT = "last_transform_state";
    public GunType type;

    public ItemGun(GunType type) {
        super(type);
        this.type = type;
        this.setNoRepair();
    }

    /**
     * If the player is on a shoot cooldown
     *
     * @return shoot cooldown
     */
    public static boolean isOnShootCooldown(UUID uuid) {
        return System.currentTimeMillis() < ClientTickHandler.playerNextTime.getOrDefault(uuid, 0L);
    }

    /**
     * If the player is on a reload cooldown
     *
     * @param entityPlayer
     * @return reload cooldown
     */
    public static boolean isClientReloading(EntityPlayer entityPlayer) {
        return ClientTickHandler.playerReloadCooldown.containsKey(entityPlayer.getUniqueID());
    }

    /**
     * If the player is on a reload cooldown
     *
     * @param entityPlayer
     * @return reload cooldown
     */
    public static boolean isServerReloading(EntityPlayer entityPlayer) {
        return ServerTickHandler.playerReloadCooldown.containsKey(entityPlayer.getUniqueID());
    }

    public static boolean hasAmmoLoaded(ItemStack gunStack) {
        return !gunStack.isEmpty() ? !(gunStack.getItem() instanceof ItemAir) ? gunStack.hasTagCompound() ? gunStack.getTagCompound().hasKey("ammo") ? gunStack.getTagCompound().getTag("ammo") != null : false : false : false : false;
    }

    public static int getMagazineBullets(ItemStack gunStack) {
        if (hasAmmoLoaded(gunStack)) {
            ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
            if (ammoStack.getItem() instanceof ItemAmmo) {
                ItemAmmo itemAmmo = (ItemAmmo)ammoStack.getItem();
                if (ammoStack.getTagCompound() != null) {
                    String key = itemAmmo.type.magazineCount > 1 ? "ammocount" + ammoStack.getTagCompound().getInteger("magcount") : "ammocount";
                    int ammoCount = ammoStack.getTagCompound().getInteger(key);
                    return ammoCount;
                }
            }
        }
        return 0;
    }

    public static int getAmmoCount(ItemStack gunStack) {
        if (hasAmmoLoaded(gunStack)) {
            ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
            if (ammoStack != null) {
                if (ammoStack.getItem() instanceof ItemAmmo) {
                    ItemAmmo itemAmmo = (ItemAmmo)ammoStack.getItem();
                    if (ammoStack.getTagCompound() != null) {
                        String key = itemAmmo.type.magazineCount > 1 ? "ammocount" + ammoStack.getTagCompound().getInteger("magcount") : "ammocount";
                        return ammoStack.getTagCompound().getInteger(key);
                    }
                }
            }
        } else if (gunStack.getTagCompound() != null && gunStack.getTagCompound().hasKey("ammocount")) {
            return gunStack.getTagCompound().getInteger("ammocount");
        }
        return 0;
    }

    public static boolean hasNextShot(ItemStack gunStack) {
        return getAmmoCount(gunStack) > 0;
    }

    public static void consumeShot(ItemStack gunStack) {
        if (hasAmmoLoaded(gunStack)) {
            ItemStack ammoStack = new ItemStack(gunStack.getTagCompound().getCompoundTag("ammo"));
            ItemAmmo itemAmmo = (ItemAmmo)ammoStack.getItem();
            if (ammoStack.getTagCompound() != null) {
                NBTTagCompound nbtTagCompound = ammoStack.getTagCompound();
                String key = itemAmmo.type.magazineCount > 1 ? "ammocount" + nbtTagCompound.getInteger("magcount") : "ammocount";
                nbtTagCompound.setInteger(key, nbtTagCompound.getInteger(key) - 1);
                gunStack.getTagCompound().setTag("ammo", ammoStack.writeToNBT(new NBTTagCompound()));
            }
        } else if (gunStack.getTagCompound() != null && gunStack.getTagCompound().hasKey("ammocount")) {
            int ammoCount = gunStack.getTagCompound().getInteger("ammocount");
            gunStack.getTagCompound().setInteger("ammocount", ammoCount - 1);
        }
    }

    public static ItemBullet getUsedBullet(ItemStack gunStack, GunType gunType) {
        if (gunType.acceptedAmmo != null)
            return ItemAmmo.getUsedBullet(gunStack);
        else if (gunType.acceptedBullets != null) {
            if (gunStack.hasTagCompound() && gunStack.getTagCompound().hasKey("bullet")) {
                ItemStack usedBullet = new ItemStack(gunStack.getTagCompound().getCompoundTag("bullet"));
                ItemBullet usedBulletItem = (ItemBullet)usedBullet.getItem();
                return usedBulletItem;
            }
        }
        return null;
    }

    public static boolean isIndoors(final EntityLivingBase givenEntity) {
        final BlockPos blockPos = givenEntity.world.getPrecipitationHeight(givenEntity.getPosition());
        if (blockPos != null) {
            if (blockPos.getY() > givenEntity.posY) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public void setType(BaseType type) {
        this.type = (GunType)type;
    }

    @Override
    public void onUpdate(ItemStack unused, World world, Entity holdingEntity, int intI, boolean flag) {
        if (holdingEntity instanceof EntityPlayer) {
            EntityPlayer entityPlayer = (EntityPlayer)holdingEntity;

            if (entityPlayer.getHeldItemMainhand() != null && entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun) {
                ItemStack heldStack = entityPlayer.getHeldItemMainhand();
                ItemGun itemGun = (ItemGun)heldStack.getItem();
                GunType gunType = itemGun.type;

                if (world.isRemote) {
                    // onUpdateClient(entityPlayer, world, heldStack, itemGun, gunType);
                } else {
                    onUpdateServer(entityPlayer, world, heldStack, itemGun, gunType);
                }

                if (heldStack.getTagCompound() == null) {
                    heldStack.setTagCompound(new NBTTagCompound());
                }

                if (!heldStack.getTagCompound().hasKey("init")) {
                    NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
                    nbtTagCompound.setString("firemode", gunType.fireModes[0].name().toLowerCase());
                    nbtTagCompound.setInteger("skinId", 0);
                    nbtTagCompound.setBoolean("punched", gunType.isEnergyGun);

                    nbtTagCompound.setInteger("init", 1);

                    if (gunType.defaultAttachments != null) {
                        for (Map.Entry<AttachmentPresetEnum, String> e : gunType.defaultAttachments.entrySet()) {
                            GunType.addAttachment(heldStack, e.getKey(), new ItemStack(ModularWarfare.attachmentTypes.get(e.getValue())));
                        }
                    }

                    if (gunType.defaultBullet != null) {
                        ItemBullet itemBullet = ModularWarfare.bulletTypes.get(gunType.defaultBullet);
                        if (itemBullet != null) {
                            ItemStack bullet = new ItemStack(itemBullet);

                            if (gunType.acceptedAmmo != null && gunType.acceptedAmmo.length > 0 && gunType.defaultAmmo != null) {

                                ItemAmmo itemAmmo = ModularWarfare.ammoTypes.get(gunType.defaultAmmo);
                                if (itemAmmo != null) {
                                    ItemStack ammo = new ItemStack(itemAmmo);
                                    if (ammo.getTagCompound() == null)
                                        ammo.setTagCompound(new NBTTagCompound());

                                    ammo.getTagCompound().setInteger("ammocount", itemAmmo.type.ammoCapacity);
                                    ammo.getTagCompound().setTag("bullet", bullet.writeToNBT(new NBTTagCompound()));
                                    nbtTagCompound.setTag("ammo", ammo.writeToNBT(new NBTTagCompound()));
                                }

                            } else if (gunType.acceptedBullets != null && gunType.acceptedBullets.length > 0) {
                                nbtTagCompound.setInteger("ammocount", gunType.internalAmmoStorage);
                                nbtTagCompound.setTag("bullet", bullet.writeToNBT(new NBTTagCompound()));
                            }
                        }
                    }
                }
            }
        }
    }

    public void onUpdateClient(EntityPlayer entityPlayer, World world, ItemStack heldStack, ItemGun itemGun, GunType gunType) {
        if (entityPlayer.getHeldItemMainhand() != null && entityPlayer.getHeldItemMainhand().getItem() instanceof ItemGun && RenderParameters.GUN_CHANGE_Y == 0F && RenderParameters.collideFrontDistance <= 0.2f) {
            if (gunType.getFireMode(heldStack) == WeaponFireMode.SAFE) {
                return;
            }
            Runnable fire=()->{
                FireData fireData = FireData.buildClient(itemGun, heldStack, gunType, gunType.getFireMode(heldStack), world);
                FireManager.fire(entityPlayer, fireData);
            };
            if (fireButtonHeld && Minecraft.getMinecraft().inGameHasFocus && gunType.getFireMode(heldStack) == WeaponFireMode.FULL) {
                fire.run();
            } else if (fireButtonHeld & !lastFireButtonHeld && Minecraft.getMinecraft().inGameHasFocus && gunType.getFireMode(heldStack) == WeaponFireMode.SEMI) {
                fire.run();
            } else if (gunType.getFireMode(heldStack) == WeaponFireMode.BURST) {
                NBTTagCompound tagCompound = heldStack.getTagCompound();
                boolean canFire = true;
                if (tagCompound.hasKey("shotsremaining") && tagCompound.getInteger("shotsremaining") > 0) {
                    fire.run();
                    canFire = false;
                } else if (fireButtonHeld & !lastFireButtonHeld && Minecraft.getMinecraft().inGameHasFocus && canFire) {
                    fire.run();
                }
            }
            lastFireButtonHeld = fireButtonHeld;
            if (!fireButtonHeld) {
                ItemGun.canDryFireClient = true;
            }
        }
    }

    public void onUpdateServer(EntityPlayer entityPlayer, World world, ItemStack heldStack, ItemGun itemGun, GunType gunType) {

    }

    public static void playImpactSound(World world, double posX, double posY, double posZ, EnumFacing facing, BlockPos blockPos, GunType gunType) {
        if (facing != null) {
            // 根据射线击中的面调整方块坐标
            switch (facing) {
                case EAST:
                    blockPos = blockPos.west();
                    break;
                case SOUTH:
                    blockPos = blockPos.north();
                    break;
                case WEST:
                    break;
                case NORTH:
                    break;
                case UP:
                    blockPos = blockPos.down();
                    break;
                case DOWN:
                    break;
            }
        }

        Material material = world.getBlockState(blockPos).getMaterial();
        WeaponSoundType soundType = WeaponSoundType.ImpactDirt;

        if (material == Material.ROCK) {
            soundType = WeaponSoundType.ImpactStone;
        } else if (material == Material.GRASS || material == Material.GROUND || material == Material.SAND) {
            soundType = WeaponSoundType.ImpactDirt;
        } else if (material == Material.WOOD) {
            soundType = WeaponSoundType.ImpactWood;
        } else if (material == Material.GLASS) {
            soundType = WeaponSoundType.ImpactGlass;
        } else if (material == Material.WATER) {
            soundType = WeaponSoundType.ImpactWater;
        } else if (material == Material.IRON) {
            soundType = WeaponSoundType.ImpactMetal;
        }

        BlockPos hitPos = new BlockPos(posX, posY, posZ);
        gunType.playSoundPos(hitPos, world, soundType, null, 1f, true);
    }

    public static void playImpactSound(World world, RayTraceResult rayTrace, GunType gunType) {
        if (rayTrace == null || rayTrace.hitVec == null)
            return;

        playImpactSound(world, rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, rayTrace.sideHit, rayTrace.getBlockPos(), gunType);
    }

    // 保留旧方法以保持兼容性
    public static void playImpactSound(World world, BlockPos pos, GunType gunType) {
        playImpactSound(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, null, pos, gunType);
    }

    public static void playHitEffect(World world, double posX, double posY, double posZ, EnumFacing facing, BlockPos blockPos) {
        RayTraceResult rayTrace = new RayTraceResult(RayTraceResult.Type.BLOCK, new Vec3d(posX, posY, posZ), facing, blockPos);
        playHitEffect(world, rayTrace);
    }

    public static void playHitEffect(World world, RayTraceResult rayTrace) {
        if (rayTrace == null || rayTrace.hitVec == null)
            return;

        BlockPos pos = rayTrace.getBlockPos();
        if (rayTrace.sideHit != null) {
            // 根据射线击中的面调整方块坐标
            switch (rayTrace.sideHit) {
                case EAST:
                    pos = pos.west();
                    break;
                case SOUTH:
                    pos = pos.north();
                    break;
                case WEST:
                    break;
                case NORTH:
                    break;
                case UP:
                    pos = pos.down();
                    break;
                case DOWN:
                    break;
            }
        }

        Material material = world.getBlockState(pos).getMaterial();

        // String materialType = "未知";
        if (material == Material.IRON) {
            // materialType = "金属";
            SAPackets.network.sendToAll(new PacketSpawnParticle("GunHit", rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, 0, 0, 0));
        } else if (material == Material.ROCK) {
            // materialType = "石头";
            SAPackets.network.sendToAll(new PacketSpawnParticle("GunExp", rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, 0, 0, 0));
        } else if (material == Material.GROUND) {
            // materialType = "泥土";
            SAPackets.network.sendToAll(new PacketSpawnParticle("GunExp", rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, 0, 0, 0));
        } else if (material == Material.WOOD) {
            // materialType = "木头";
            SAPackets.network.sendToAll(new PacketSpawnParticle("GunExp", rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, 0, 0, 0));
        } else if (material == Material.GRASS) {
            // materialType = "草地";
            SAPackets.network.sendToAll(new PacketSpawnParticle("GunExp", rayTrace.hitVec.x, rayTrace.hitVec.y, rayTrace.hitVec.z, 0, 0, 0));
        }

        // String message = String.format("§7命中坐标: §f[%.2f, %.2f, %.2f] §7材质: §f%s §7方向: §f%s",
        // rayTrace.hitVec.x,
        // rayTrace.hitVec.y,
        // rayTrace.hitVec.z,
        // materialType,
        // rayTrace.sideHit);
        // if(Minecraft.getMinecraft().player != null) {
        // Minecraft.getMinecraft().player.sendMessage(new TextComponentString(message));
        // }
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
        Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
        if (slot == EntityEquipmentSlot.MAINHAND) {
            if (type.moveSpeedModifier - 1.0f != 0) {
                multimap.put(SharedMonsterAttributes.MOVEMENT_SPEED.getName(), new AttributeModifier(MOVEMENT_SPEED_MODIFIER, "MovementSpeed", type.moveSpeedModifier - 1.0f, 2));
            }
        }
        return multimap;
    }

    public static void doHit(double posX, double posY, double posZ, EnumFacing facing) {
        doHit(new RayTraceResult(RayTraceResult.Type.BLOCK, new Vec3d(posX, posY, posZ), facing, new BlockPos(posX, posY, posZ)));
    }

    public static void doHit(RayTraceResult raytraceResultIn) {
        if (raytraceResultIn.getBlockPos() != null) {
            BlockPos pos = raytraceResultIn.getBlockPos();
            double hitX = raytraceResultIn.hitVec.x;
            double hitY = raytraceResultIn.hitVec.y;
            double hitZ = raytraceResultIn.hitVec.z;
            double aX = 0;
            double aY = 0;
            double aZ = 0;
            switch (raytraceResultIn.sideHit) {
                case EAST:
                    aX = -1;
                    break;
                case SOUTH:
                    aZ = -1;
                    break;
                case WEST:
                    aX = 1;
                    break;
                case NORTH:
                    aZ = 1;
                    break;
                case UP:
                    aY = -1;
                    break;
                case DOWN:
                    aY = 1;
                    break;
                default:
                    break;
            }
            ModularWarfare.NETWORK.sendToAll(new PacketBulletHole(100, hitX, hitY, hitZ, aX, aY, aZ));
            ModularWarfare.NETWORK.sendToAll(new PacketHitEffect(hitX, hitY, hitZ));
        }
    }

    public static boolean canEntityGetHeadshot(Entity e) {
        return e instanceof EntityZombie || e instanceof EntitySkeleton || e instanceof EntityCreeper || e instanceof EntityWitch || e instanceof EntityPigZombie || e instanceof EntityEnderman || e instanceof EntityWitherSkeleton || e instanceof EntityPlayer || e instanceof EntityVillager || e instanceof EntityEvoker || e instanceof EntityStray || e instanceof EntityVindicator || e instanceof EntityIronGolem || e instanceof EntitySnowman || e.getName().contains("common");
    }

    public void onGunSwitchMode(EntityPlayer entityPlayer, World world, ItemStack gunStack, ItemGun itemGun, WeaponFireMode fireMode) {
        GunType.setFireMode(gunStack, fireMode);

        GunType gunType = itemGun.type;
        if (WeaponSoundType.ModeSwitch != null && !world.isRemote) {
            gunType.playSound(entityPlayer, WeaponSoundType.ModeSwitch, gunStack);
        }
    }

    /**
     * Minecraft Overrides
     */
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        GunType gunType = ((ItemGun)stack.getItem()).type;
        super.addInformation(stack, worldIn, tooltip, flagIn);

        /*
        if (hasAmmoLoaded(stack)) {
            ItemStack ammoStack = new ItemStack(stack.getTagCompound().getCompoundTag("ammo"));
            if (ammoStack.getItem() instanceof ItemAmmo) {
                ItemAmmo itemAmmo = (ItemAmmo) ammoStack.getItem();
        
                if (itemAmmo.type.magazineCount == 1) {
                    int currentAmmoCount = 0;
                    if (ammoStack.getTagCompound() != null) {
                        NBTTagCompound tag = ammoStack.getTagCompound();
                        currentAmmoCount = tag.hasKey("ammocount") ? tag.getInteger("ammocount") : 0;
                    }
        
                    tooltip.add(generateLoreLineAlt("Ammo", Integer.toString(currentAmmoCount), Integer.toString(itemAmmo.type.ammoCapacity)));
                } else {
                    if (stack.getTagCompound() != null) {
                        String baseDisplayLine = "Ammo %s: %g%s%dg/%g%s";
                        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
                        baseDisplayLine = baseDisplayLine.replaceAll("%dg", TextFormatting.DARK_GRAY.toString());
        
                        for (int i = 1; i < itemAmmo.type.magazineCount + 1; i++) {
                            NBTTagCompound tag = ammoStack.getTagCompound();
                            String displayLine = baseDisplayLine.replaceAll("%g", i == tag.getInteger("magcount") ? TextFormatting.YELLOW.toString() : TextFormatting.GRAY.toString());
                            tooltip.add(String.format(displayLine, i, tag.getInteger("ammocount" + i), itemAmmo.type.ammoCapacity));
                        }
                    }
                }
            }
        }
        
        if (stack.getTagCompound() != null) {
            if (gunType.acceptedBullets != null) {
                int ammoCount = stack.getTagCompound().hasKey("ammocount") ? stack.getTagCompound().getInteger("ammocount") : 0;
                tooltip.add(generateLoreLineAlt("Ammo", Integer.toString(ammoCount), Integer.toString(gunType.internalAmmoStorage)));
            }
        }
        
        if (ItemAmmo.getUsedBullet(stack) != null) {
            ItemBullet itemBullet = ItemAmmo.getUsedBullet(stack);
            tooltip.add(generateLoreLine("Bullet", itemBullet.type.displayName));
        }
        
        String baseDisplayLine = "%bFire Mode: %g%s";
        baseDisplayLine = baseDisplayLine.replaceAll("%b", TextFormatting.BLUE.toString());
        baseDisplayLine = baseDisplayLine.replaceAll("%g", TextFormatting.GRAY.toString());
        tooltip.add(String.format(baseDisplayLine, GunType.getFireMode(stack) != null ? GunType.getFireMode(stack) : gunType.fireModes[0]));
        
        
        if (GuiScreen.isShiftKeyDown()) {
            DecimalFormat decimalFormat = new DecimalFormat("#.#");
        
            String damageLine = "%bDamage: %g%s";
            damageLine = damageLine.replaceAll("%b", TextFormatting.BLUE.toString());
            damageLine = damageLine.replaceAll("%g", TextFormatting.RED.toString());
            if (gunType.numBullets > 1) {
                tooltip.add(String.format(damageLine, gunType.gunDamage + " x " + gunType.numBullets));
            } else {
                tooltip.add(String.format(damageLine, gunType.gunDamage));
            }
        
        
            String accuracyLine = "%bAccuracy: %g%s";
            accuracyLine = accuracyLine.replaceAll("%b", TextFormatting.BLUE.toString());
            accuracyLine = accuracyLine.replaceAll("%g", TextFormatting.RED.toString());
        
            tooltip.add(String.format(accuracyLine, decimalFormat.format((1 / gunType.bulletSpread) * 100) + "%"));
        
            if (gunType.acceptedAttachments != null) {
                if (!gunType.acceptedAttachments.isEmpty()) {
                    tooltip.add("" + TextFormatting.BLUE.toString() + "Accepted attachments:");
                    for (ArrayList<String> strings : gunType.acceptedAttachments.values()) {
                        for (int i = 0; i < strings.size(); i++) {
                            try {
                                final String attachment = ModularWarfare.attachmentTypes.get(strings.get(i)).type.displayName;
                                if (attachment != null) {
                                    tooltip.add("- " + attachment);
                                }
                            } catch (NullPointerException error) {
                            }
                        }
                    }
                }
            }
        
            if (gunType.acceptedAmmo != null) {
                tooltip.add("" + TextFormatting.BLUE.toString() + "Accepted mags:");
                if (gunType.acceptedAmmo.length > 0) {
                    for (String internalName : gunType.acceptedAmmo) {
                        if (ModularWarfare.ammoTypes.containsKey(internalName)) {
                            final String magName = ModularWarfare.ammoTypes.get(internalName).type.displayName;
                            if (magName != null) {
                                tooltip.add("- " + magName);
                            }
                        }
                    }
                }
            }
        
            if (gunType.acceptedBullets != null) {
                tooltip.add("" + TextFormatting.BLUE.toString() + "Accepted bullets:");
        
                if (gunType.acceptedBullets.length > 0) {
                    for (String internalName : gunType.acceptedBullets) {
                        if (ModularWarfare.bulletTypes.containsKey(internalName)) {
                            final String magName = ModularWarfare.bulletTypes.get(internalName).type.displayName;
                            if (magName != null) {
                                tooltip.add("- " + magName);
                            }
                        }
                    }
                }
            }
        
            if(gunType.extraLore != null) {
                tooltip.add("" + TextFormatting.BLUE.toString() + "Lore:");
                tooltip.add(gunType.extraLore);
            }
        } else {
            tooltip.add("\u00a7e" + "[Shift]");
        }
        */
    }

    @Override
    public boolean getShareTag() {
        return true;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack p_77626_1_) {
        return 0;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack p_77661_1_) {
        return EnumAction.NONE;
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack) {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        boolean result = !oldStack.equals(newStack);
        if (result) {
            // TODO: Requip animation
        }
        return result;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, EntityPlayer player) {
        World world = player.world;
        if (!world.isRemote) {
            // Client will still render block break if player is in creative so update block state
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
        return true;
    }

    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return false;
    }

    @Override
    public boolean canHarvestBlock(IBlockState state, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canItemEditBlocks() {
        return false;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity) {
        return true;
    }

    public void setLaserEnabled(ItemStack stack, boolean enabled) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean(LASER_ENABLED_NBT, enabled);
    }

    public boolean getLaserEnabled(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            return false;
        }
        return stack.getTagCompound().getBoolean(LASER_ENABLED_NBT);
    }

    /**
     * 获取枪械当前的变换状态
     */
    public static int getTransformState(ItemStack heldStack) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            return nbtTagCompound.hasKey(TRANSFORM_STATE_NBT) ? nbtTagCompound.getInteger(TRANSFORM_STATE_NBT) : 0;
        }
        return 0;
    }

    /**
     * 设置枪械当前的变换状态
     */
    public static void setTransformState(ItemStack heldStack, int state) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            nbtTagCompound.setInteger(LAST_TRANSFORM_STATE_NBT, getTransformState(heldStack));
            nbtTagCompound.setInteger(TRANSFORM_STATE_NBT, state);
            heldStack.setTagCompound(nbtTagCompound);
        }
    }

    /**
     * 获取枪械上一次的变换状态
     */
    public static int getLastTransformState(ItemStack heldStack) {
        if (heldStack.getTagCompound() != null) {
            NBTTagCompound nbtTagCompound = heldStack.getTagCompound();
            return nbtTagCompound.hasKey(LAST_TRANSFORM_STATE_NBT) ? nbtTagCompound.getInteger(LAST_TRANSFORM_STATE_NBT) : 0;
        }
        return 0;
    }

    /**
     * 切换到上一次的变换状态
     */
    public static void switchToLastTransformState(ItemStack heldStack) {
        int currentState = getTransformState(heldStack);
        int lastState = getLastTransformState(heldStack);
        setTransformState(heldStack, lastState);
    }
}