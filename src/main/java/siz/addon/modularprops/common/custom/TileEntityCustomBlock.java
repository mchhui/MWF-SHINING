package siz.addon.modularprops.common.custom;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import siz.addon.modularprops.client.fpp.enhanced.animation.CustomBlockAnimationController;

import javax.annotation.Nonnull;

public class TileEntityCustomBlock extends TileEntity implements ITickable {

    /**
     * 动画状态
     */
    public float animationTime = 0.0F;

    /**
     * 动画速度
     */
    public float animationSpeed = 1.0F;
    
    /**
     * 是否激活（用于动画状态）
     */
    private boolean activated = false;
    
    /**
     * 是否正在破坏
     */
    private boolean breaking = false;
    
    /**
     * 方块朝向（玩家放置时的朝向）
     */
    private EnumFacing facing = EnumFacing.NORTH;
    
    /**
     * 客户端动画控制器（仅在客户端使用）
     */
    @SideOnly(Side.CLIENT)
    private CustomBlockAnimationController animationController;

    @Override
    public void update() {
        // 服务器端更新动画时间
        if (world != null && !world.isRemote) {
            animationTime += animationSpeed;
            if (animationTime > 360.0F) {
                animationTime -= 360.0F;
            }
        }
        
        // 客户端不需要在这里更新动画控制器
        // 动画更新在渲染时直接使用世界时间计算（参考第三人称渲染）
    }
    
    /**
     * 设置激活状态
     */
    public void setActivated(boolean activated) {
        this.activated = activated;
        markDirty();
    }
    
    /**
     * 获取激活状态
     */
    public boolean isActivated() {
        return activated;
    }
    
    /**
     * 设置动画控制器（仅客户端）
     */
    @SideOnly(Side.CLIENT)
    public void setAnimationController(CustomBlockAnimationController controller) {
        this.animationController = controller;
    }
    
    /**
     * 获取动画控制器（仅客户端）
     */
    @SideOnly(Side.CLIENT)
    public CustomBlockAnimationController getAnimationController() {
        return animationController;
    }
    
    /**
     * 设置破坏状态
     */
    public void setBreaking(boolean breaking) {
        this.breaking = breaking;
    }
    
    /**
     * 是否正在破坏
     */
    public boolean isBreaking() {
        return breaking;
    }
    
    /**
     * 设置方块朝向
     */
    public void setFacing(EnumFacing facing) {
        this.facing = facing;
        markDirty();
    }
    
    /**
     * 获取方块朝向
     */
    public EnumFacing getFacing() {
        return facing;
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat("animationTime", animationTime);
        compound.setFloat("animationSpeed", animationSpeed);
        compound.setBoolean("activated", activated);
        compound.setInteger("facing", facing.getIndex());
        return compound;
    }

    @Override
    public void readFromNBT(@Nonnull NBTTagCompound compound) {
        super.readFromNBT(compound);
        animationTime = compound.getFloat("animationTime");
        animationSpeed = compound.getFloat("animationSpeed");
        activated = compound.getBoolean("activated");
        if (compound.hasKey("facing")) {
            facing = EnumFacing.byIndex(compound.getInteger("facing"));
        }
    }
    
    /**
     * 获取用于客户端同步的NBT数据
     */
    @Override
    @Nonnull
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }
    
    /**
     * 获取用于客户端同步的数据包
     */
    @Override
    @javax.annotation.Nullable
    public net.minecraft.network.play.server.SPacketUpdateTileEntity getUpdatePacket() {
        return new net.minecraft.network.play.server.SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }
    
    /**
     * 客户端接收到数据包时的处理
     */
    @Override
    public void onDataPacket(@Nonnull net.minecraft.network.NetworkManager net, @Nonnull net.minecraft.network.play.server.SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }
}

