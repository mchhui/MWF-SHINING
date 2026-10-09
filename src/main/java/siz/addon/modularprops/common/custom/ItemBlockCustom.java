package siz.addon.modularprops.common.custom;

import com.modularwarfare.ModularWarfare;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockCustom extends ItemBlock {

    public CustomBlockType type;

    public ItemBlockCustom(Block block, CustomBlockType type) {
        super(block);
        this.type = type;
        // 注意：注册名称在 ModularPropsContentTypes 中设置，这里不设置
    }
    
    @Override
    public String getItemStackDisplayName(@Nonnull ItemStack stack) {
        // 直接返回 displayName，让 Minecraft 的翻译系统自动处理翻译
        // 注意：不能使用客户端专用的 I18n，因为此方法在服务端也会被调用
        if (type != null && type.displayName != null) {
            return type.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }
    
    @Override
    public CreativeTabs getCreativeTab() {
        // 动态获取创造栏（与 BaseItem 保持一致）
        if (type != null && type.contentPack != null && ModularWarfare.MODS_TABS.containsKey(type.contentPack)) {
            return ModularWarfare.MODS_TABS.get(type.contentPack);
        }
        return super.getCreativeTab();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(@Nonnull ItemStack stack, @Nullable World worldIn, @Nonnull List<String> tooltip, @Nonnull net.minecraft.client.util.ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        
        // 可以在这里添加自定义tooltip信息
        if (type != null) {
            tooltip.add("Type: " + type.animationType.name());
        }
    }
}

