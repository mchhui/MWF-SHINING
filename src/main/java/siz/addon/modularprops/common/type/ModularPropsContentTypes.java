package siz.addon.modularprops.common.type;

import com.modularwarfare.ModularWarfare;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import com.modularwarfare.common.guns.WeaponAnimationType;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.common.type.TypeEntry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import siz.addon.modularprops.ModularProps;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomBlock;
import siz.addon.modularprops.client.fpp.enhanced.models.ModelEnhancedCustomItem;
import siz.addon.modularprops.common.custom.BlockCustom;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.ItemCustom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ModularPropsContentTypes {

    public static ArrayList<TypeEntry> values = new ArrayList<>();
    private static int typeId = 0;
    
    // 标记默认TileEntity是否已注册（避免重复注册）
    private static boolean defaultTileEntityRegistered = false;

    public static void registerTypes() {
        // 注册自定义道具类型
        registerType("customitems", CustomItemType.class, (type, reload) -> {
            CustomItemType itemType = (CustomItemType) type;
            
            if (!reload) {
                // 创建道具实例（注册名称已在 ItemCustom 构造函数中设置）
                ItemCustom item = new ItemCustom(itemType);
                ModularProps.customItemTypes.put(itemType.internalName, item);
            } else {
                // 重载时更新类型定义
                if (ModularProps.customItemTypes.containsKey(itemType.internalName)) {
                    ModularProps.customItemTypes.get(itemType.internalName).setType(itemType);
                }
            }

            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                if (itemType.animationType == WeaponAnimationType.ENHANCED) {
                    // 先获取配置，再创建模型（避免在构造函数中调用 getModelLocation 时 baseType 或 config 为 null）
                    CustomItemEnhancedRenderConfig config = ModularWarfare.getRenderConfig(itemType, CustomItemEnhancedRenderConfig.class);
                    if (config != null) {
                        if (itemType.enhancedModel == null) {
                            itemType.enhancedModel = new ModelEnhancedCustomItem(config, itemType);
                        } else {
                            itemType.enhancedModel.config = config;
                            itemType.enhancedModel.baseType = itemType;
                        }
                    }
                }
            }
        });

        // 注册自定义方块类型
        registerType("customblocks", CustomBlockType.class, (type, reload) -> {
            CustomBlockType blockType = (CustomBlockType) type;

            // 在客户端加载模型（必须在 @SideOnly(Side.CLIENT) 注解的地方调用）
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
                ModularProps.LOGGER.info("Loading model for block: " + blockType.internalName + " (animationType: " + blockType.animationType + ")");
                
                if (blockType.animationType == WeaponAnimationType.ENHANCED) {
                    // 先获取配置，再创建模型（避免在构造函数中调用 getModelLocation 时 baseType 或 config 为 null）
                    CustomBlockEnhancedRenderConfig config = ModularWarfare.getRenderConfig(blockType, CustomBlockEnhancedRenderConfig.class);
                    
                    ModularProps.LOGGER.info("Config loaded for block " + blockType.internalName + ": " + (config != null ? "SUCCESS" : "FAILED"));
                    
                    if (config != null) {
                        try {
                            if (blockType.enhancedModel == null) {
                                blockType.enhancedModel = new ModelEnhancedCustomBlock(config, blockType);
                                ModularProps.LOGGER.info("Created new enhanced model for block: " + blockType.internalName);
                            } else {
                                blockType.enhancedModel.config = config;
                                blockType.enhancedModel.baseType = blockType;
                                ModularProps.LOGGER.info("Updated existing enhanced model for block: " + blockType.internalName);
                            }
                        } catch (Exception e) {
                            ModularProps.LOGGER.error("Exception while creating model for block: " + blockType.internalName, e);
                        }
                    } else {
                        // 配置加载失败，输出错误信息
                        ModularProps.LOGGER.error("Failed to load render config for custom block: " + blockType.internalName);
                        ModularProps.LOGGER.error("Expected config file: " + blockType.contentPack + "/customblocks/render/" + blockType.internalName + ".render.json");
                    }
                }
            }

            if (!reload) {
                // 创建方块实例（统一注册到 modularwarfare 模组下）
                BlockCustom block = new BlockCustom(blockType);
                block.setRegistryName(ModularWarfare.MOD_ID, blockType.internalName);
                // translationKey 不需要 modid 前缀，与 BaseItem 保持一致
                block.setTranslationKey(blockType.internalName);
                ModularProps.customBlockTypes.put(blockType.internalName, block);

                // 创建并存储ItemBlock（统一注册到 modularwarfare 模组下）
                ItemBlockCustom itemBlock = new ItemBlockCustom(block, blockType);
                itemBlock.setRegistryName(ModularWarfare.MOD_ID, blockType.internalName);
                // 不设置 translationKey，让 ItemBlockCustom 自己处理
                ModularProps.customBlockItemBlocks.put(blockType.internalName, itemBlock);

                // 注册 TileEntity（所有自定义方块都需要 TileEntity 来渲染模型）
                if (blockType.tileEntityClass != null) {
                    // 如果方块有自定义的 TileEntity 类，为该方块单独注册
                    net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                        blockType.tileEntityClass,
                        new net.minecraft.util.ResourceLocation(ModularWarfare.MOD_ID, "tile_" + blockType.internalName)
                    );
                } else if (!defaultTileEntityRegistered) {
                    // 默认的 TileEntityCustomBlock 只注册一次（所有使用默认TileEntity的方块共享）
                    net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(
                        siz.addon.modularprops.common.custom.TileEntityCustomBlock.class,
                        new net.minecraft.util.ResourceLocation(ModularWarfare.MOD_ID, "tile_custom_block")
                    );
                    defaultTileEntityRegistered = true;
                }
            } else {
                // 重载时更新类型定义
                if (ModularProps.customBlockTypes.containsKey(blockType.internalName)) {
                    ModularProps.customBlockTypes.get(blockType.internalName).setType(blockType);
                }
            }
        });
    }

    public static <T extends BaseType, U extends ItemCustom> void assignType(HashMap<String, U> map, Function<T, U> factory, T type, Boolean reload) {
        if (reload) {
            map.get(type.internalName).setType(type);
        } else {
            map.put(type.internalName, factory.apply((T) type));
        }
    }

    public static void registerType(String name, Class<? extends BaseType> typeClass, BiConsumer<BaseType, Boolean> typeAssignFunction) {
        values.add(new TypeEntry(name, typeClass, typeId, typeAssignFunction));
        typeId += 1;
    }
}

