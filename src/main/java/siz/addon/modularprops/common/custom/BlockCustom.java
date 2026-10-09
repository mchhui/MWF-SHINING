package siz.addon.modularprops.common.custom;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import siz.addon.modularprops.ModularProps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockCustom extends Block implements ITileEntityProvider {

    public CustomBlockType type;

    public BlockCustom(CustomBlockType type) {
        super(type.blockProperties.material);
        this.type = type;
        // 注意：注册名称在 ModularPropsContentTypes 中设置，这里不设置
        this.setHardness(type.blockProperties.hardness);
        this.setResistance(type.blockProperties.resistance);
        this.setLightLevel(type.blockProperties.lightLevel / 15.0F);
        
        // 设置创造栏（ItemBlock 会从 Block 获取创造栏）
        if (type != null && type.contentPack != null && com.modularwarfare.ModularWarfare.MODS_TABS.containsKey(type.contentPack)) {
            this.setCreativeTab(com.modularwarfare.ModularWarfare.MODS_TABS.get(type.contentPack));
        }
    }

    public void setType(CustomBlockType type) {
        this.type = type;
        this.setHardness(type.blockProperties.hardness);
        this.setResistance(type.blockProperties.resistance);
        this.setLightLevel(type.blockProperties.lightLevel / 15.0F);
    }

    @Override
    public boolean isOpaqueCube(@Nonnull IBlockState state) {
        // 方块本身是空模型，使用 TE 渲染，是否透明由模型自己控制
        return false;
    }

    @Override
    public boolean isFullCube(@Nonnull IBlockState state) {
        // 方块本身是空模型，使用 TE 渲染
        return false;
    }
    
    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(@Nonnull IBlockState state) {
        // 使用 TileEntity 渲染器，方块本身不渲染
        return EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public boolean isPassable(@Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        // 根据配置决定是否可通过
        if (type != null && type.blockProperties != null) {
            return type.blockProperties.isPassable;
        }
        return false;
    }

    @Override
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState blockState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
        // 如果可通过，返回 NULL_AABB（无碰撞箱）
        if (type != null && type.blockProperties != null && type.blockProperties.isPassable) {
            return NULL_AABB;
        }
        return super.getCollisionBoundingBox(blockState, worldIn, pos);
    }

    @Override
    @Nullable
    public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
        // 所有自定义方块都需要 TileEntity 来渲染模型
        try {
            if (type != null && type.tileEntityClass != null) {
                return type.tileEntityClass.newInstance();
            } else {
                return new TileEntityCustomBlock();
            }
        } catch (Exception e) {
            ModularProps.LOGGER.error("Failed to create TileEntity for block: " + (type != null ? type.internalName : "null"), e);
            return new TileEntityCustomBlock();
        }
    }

    /**
     * 方块被放置时调用
     */
    @Override
    public void onBlockPlacedBy(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityLivingBase placer, @Nonnull ItemStack stack) {
        super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
        
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityCustomBlock) {
                TileEntityCustomBlock tileEntity = (TileEntityCustomBlock) te;
            
            // 保存玩家放置时的朝向
            if (placer != null) {
                net.minecraft.util.EnumFacing facing = net.minecraft.util.EnumFacing.fromAngle(placer.rotationYaw);
                tileEntity.setFacing(facing);
            }
            
            // 触发第三人称放置动画
            if (worldIn.isRemote && type != null) {
                if (tileEntity.getAnimationController() != null) {
                    tileEntity.getAnimationController().triggerThirdPlace();
                }
            }
        }
    }
    
    /**
     * 方块被破坏时调用
     */
    @Override
    public void onBlockHarvested(@Nonnull World worldIn, @Nonnull BlockPos pos, @Nonnull IBlockState state, @Nonnull EntityPlayer player) {
        // 触发第三人称破坏动画
        if (worldIn.isRemote && type != null) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TileEntityCustomBlock) {
                TileEntityCustomBlock tileEntity = (TileEntityCustomBlock) te;
                if (tileEntity.getAnimationController() != null) {
                    tileEntity.getAnimationController().triggerBreak();
                    // 标记为正在破坏，等待动画完成后再移除
                    tileEntity.setBreaking(true);
                }
            }
        }
        
        super.onBlockHarvested(worldIn, pos, state, player);
    }
    
    // 注意：在1.12.2中，animateTick方法在Block类中不存在，需要通过其他方式实现世界动画
    // 可以通过TileEntity或事件系统来实现
}

