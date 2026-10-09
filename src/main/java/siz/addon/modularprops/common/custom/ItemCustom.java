package siz.addon.modularprops.common.custom;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.type.BaseItem;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

public class ItemCustom extends BaseItem {

    public CustomItemType type;

    public ItemCustom(CustomItemType type) {
        super(type);
        this.type = type;
        this.render3d = true;
        
        // 使用反射重置注册名称，然后设置为 modularwarfare modid
        try {
            // 获取 registryName 字段
            Field registryNameField = net.minecraftforge.registries.IForgeRegistryEntry.Impl.class.getDeclaredField("registryName");
            registryNameField.setAccessible(true);
            // 重置为 null，然后重新设置
            registryNameField.set(this, null);
        } catch (Exception e) {
            // 如果反射失败，忽略（可能字段名不同或已设置）
        }
        
        // 统一注册到 modularwarfare 模组下
        this.setRegistryName(ModularWarfare.MOD_ID, type.internalName);
        // translationKey 不需要 modid 前缀，BaseItem 也是这样设置的
        this.setTranslationKey(type.internalName);
    }

    @Override
    public void setType(BaseType type) {
        this.type = (CustomItemType) type;
        this.baseType = type;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, net.minecraft.world.World worldIn, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        
        // 可以在这里添加自定义tooltip信息
        if (type != null) {
            tooltip.add("Type: " + type.itemType.name());
        }
    }
}
