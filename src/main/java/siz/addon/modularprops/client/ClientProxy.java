package siz.addon.modularprops.client;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.api.GenerateJsonModelsEvent;
import com.modularwarfare.api.HandleKeyEvent;
import com.modularwarfare.api.RenderHandFisrtPersonEnhancedEvent;
import com.modularwarfare.client.export.ItemModelExport;
import com.modularwarfare.client.input.KeyType;
import com.modularwarfare.common.type.BaseType;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.IStateMapper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import siz.addon.modularprops.ModularProps;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomBlockEnhancedRenderConfig;
import siz.addon.modularprops.client.fpp.enhanced.configs.CustomItemEnhancedRenderConfig;
import siz.addon.modularprops.client.render.RenderCustomBlock;
import siz.addon.modularprops.common.CommonProxy;
import siz.addon.modularprops.common.custom.BlockCustom;
import siz.addon.modularprops.common.custom.CustomBlockType;
import siz.addon.modularprops.common.custom.CustomItemType;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.custom.TileEntityCustomBlock;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    private static final Logger LOGGER = LogManager.getLogger("ModularProps");
    private static final Pattern zipJar = Pattern.compile(".*\\.(zip|jar)$");
    public static RenderCustomBlock customBlockTESR;

    @Override
    public void preInit() {
        super.preInit();
        
        // 注册 TileEntity 渲染器
        customBlockTESR = new RenderCustomBlock();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCustomBlock.class, customBlockTESR);
        
        // 注册渲染集成
        MinecraftForge.EVENT_BUS.register(new ClientRenderHooksIntegration());
        
        // 注册自定义道具和方块事件处理器
        MinecraftForge.EVENT_BUS.register(new siz.addon.modularprops.common.handler.CustomItemEventHandler());
        
        // 注册渲染层处理器
        MinecraftForge.EVENT_BUS.register(new ModularPropsLayerHandler());
        
        // 注册USE进度条HUD
        MinecraftForge.EVENT_BUS.register(new siz.addon.modularprops.client.gui.CustomItemUseHUD());
        
        // 注册事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void postInit() {
        super.postInit();
    }

    // 注意：第一人称渲染通过RenderHandFisrtPersonEnhancedEvent事件处理

    /**
     * 监听ENHANCED第一人称渲染事件
     */
    @SubscribeEvent
    public void onRenderHandEnhanced(RenderHandFisrtPersonEnhancedEvent.PreFirstLayer event) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) {
            return;
        }

        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty()) {
            return;
        }

        // 处理自定义道具
        if (stack.getItem() instanceof ItemCustom) {
            ItemCustom item = (ItemCustom) stack.getItem();
            if (item.type != null && item.type.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                event.renderer = ClientRenderHooksIntegration.customItemRenderer;
            }
        }
        // 处理自定义方块
        else if (stack.getItem() instanceof ItemBlockCustom) {
            ItemBlockCustom itemBlock = (ItemBlockCustom) stack.getItem();
            if (itemBlock.type != null && itemBlock.type.animationType == com.modularwarfare.common.guns.WeaponAnimationType.ENHANCED) {
                event.renderer = ClientRenderHooksIntegration.customBlockRenderer;
            }
        }
    }
    
    /**
     * 监听F9重载事件
     */
    @SubscribeEvent
    public void onHandleKey(HandleKeyEvent event) {
        EntityPlayer entityPlayer = Minecraft.getMinecraft().player;
        if (entityPlayer == null) {
            return;
        }
        
        // 只处理 F9 重载事件
        if (event.keyType == KeyType.ClientReload) {
            // 重置渲染器的模型缓存
            if (ClientRenderHooksIntegration.customItemRenderer != null) {
                ClientRenderHooksIntegration.customItemRenderer.resetModels();
            }
            if (ClientRenderHooksIntegration.customBlockRenderer != null) {
                ClientRenderHooksIntegration.customBlockRenderer.resetModels();
            }
            if (customBlockTESR != null) {
                customBlockTESR.clearAllModelCache();
            }

            ItemStack heldStack = entityPlayer.getHeldItemMainhand();
            if (heldStack.isEmpty()) {
                return;
            }

            // 重载自定义道具的配置
            if (heldStack.getItem() instanceof ItemCustom) {
                CustomItemType customItemType = ((ItemCustom) heldStack.getItem()).type;
                if (customItemType != null) {
                    customItemType.reloadModel();
                    if (customItemType.enhancedModel != null) {
                        customItemType.enhancedModel.forceReload();
                    }
                }
            }

            // 重载自定义方块的配置
            if (heldStack.getItem() instanceof ItemBlockCustom) {
                CustomBlockType customBlockType = ((ItemBlockCustom) heldStack.getItem()).type;
                if (customBlockType != null) {
                    customBlockType.reloadModel();
                    if (customBlockType.enhancedModel != null) {
                        customBlockType.enhancedModel.forceReload();
                    }
                }
            }
        }
    }
    
    /**
     * 注册模型资源位置（参考 ModularWarfare ClientProxy）
     * 这会让 Minecraft 知道在哪里找到 ItemCustom 的 JSON 模型文件
     */
    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {
        // 注册自定义道具的模型
        for (ItemCustom itemCustom : ModularProps.customItemTypes.values()) {
            if (itemCustom.getRegistryName() != null) {
                ModelLoader.setCustomModelResourceLocation(
                    itemCustom, 
                    0, 
                    new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + itemCustom.type.internalName, "inventory")
                );
                LOGGER.info("Registered model for custom item: " + itemCustom.getRegistryName());
            }
        }
        
        // 为自定义方块设置空状态映射器（方块本身不渲染，仅使用TileEntity渲染）
        // 使用空映射器，方块不会加载任何模型文件
        IStateMapper emptyStateMapper = new IStateMapper() {
            @Override
            @SuppressWarnings("unchecked")
            public Map<IBlockState, ModelResourceLocation> putStateModelLocations(@javax.annotation.Nonnull Block blockIn) {
                return Collections.EMPTY_MAP;
            }
        };
        
        for (BlockCustom block : ModularProps.customBlockTypes.values()) {
            if (block != null) {
                ModelLoader.setCustomStateMapper(block, emptyStateMapper);
                LOGGER.info("Set empty state mapper for block: " + block.getRegistryName());
            }
        }
        
        // 注册方块的ItemBlock模型
        for (Item itemBlock : ModularProps.customBlockItemBlocks.values()) {
            if (itemBlock != null) {
                net.minecraft.util.ResourceLocation registryName = itemBlock.getRegistryName();
                if (registryName != null) {
                    String registryPath = registryName.getPath();
                    if (registryPath != null) {
                        ModelLoader.setCustomModelResourceLocation(
                            itemBlock, 
                            0, 
                            new ModelResourceLocation(ModularWarfare.MOD_ID + ":" + registryPath, "inventory")
                        );
                        LOGGER.info("Registered model for block item: " + registryName);
                    }
                }
            }
        }
    }
    
    @Override
    public void generateJsonModels(ArrayList<BaseType> types) {
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        
        GenerateJsonModelsEvent event = new GenerateJsonModelsEvent();
        MinecraftForge.EVENT_BUS.post(event);
        
        for (BaseType type : types) {
            if (type.contentPack == null)
                continue;
            
            File contentPackDir = new File(ModularWarfare.CONTENT_DIR, type.contentPack);
            
            if (zipJar.matcher(contentPackDir.getName()).matches())
                continue;
            
            if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                // 生成物品模型 JSON
                File itemModelsDir = new File(contentPackDir, "/assets/modularwarfare/models/item");
                if (!itemModelsDir.exists())
                    itemModelsDir.mkdirs();
                
                File typeModel = new File(itemModelsDir, type.internalName + ".json");
                if (!typeModel.exists()) {
                    try {
                        FileWriter fileWriter = new FileWriter(typeModel, false);
                        gson.toJson(createJson(type), fileWriter);
                        fileWriter.flush();
                        fileWriter.close();
                        LOGGER.info("Generated item model JSON: " + typeModel.getAbsolutePath());
                    } catch (Exception e) {
                        LOGGER.error("Failed to generate item model JSON for: " + type.internalName, e);
                    }
                }
                
                // 生成 .render.json 文件（如果不存在）
                if (ModularWarfare.DEV_ENV) {
                    final File dir = new File(contentPackDir, "/" + type.getAssetDir() + "/render");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    final File renderFile = new File(dir, type.internalName + ".render.json");
                    if (!renderFile.exists()) {
                        try {
                            FileWriter fileWriter = new FileWriter(renderFile, true);
                            if (type instanceof CustomItemType) {
                                CustomItemEnhancedRenderConfig renderConfig = new CustomItemEnhancedRenderConfig();
                                renderConfig.modelFileName = type.internalName.replaceAll(type.contentPack + ".", "");
                                renderConfig.modelFileName = renderConfig.modelFileName + ".glb";
                                gson.toJson(renderConfig, fileWriter);
                            } else if (type instanceof CustomBlockType) {
                                CustomBlockEnhancedRenderConfig renderConfig = new CustomBlockEnhancedRenderConfig();
                                renderConfig.modelFileName = type.internalName.replaceAll(type.contentPack + ".", "");
                                renderConfig.modelFileName = renderConfig.modelFileName + ".glb";
                                // 方块渲染配置已在 thirdPerson.renderElements["block"] 中默认初始化
                                gson.toJson(renderConfig, fileWriter);
                            }
                            fileWriter.flush();
                            fileWriter.close();
                            LOGGER.info("Generated render config JSON: " + renderFile.getAbsolutePath());
                        } catch (Exception e) {
                            LOGGER.error("Failed to generate render config JSON for: " + type.internalName, e);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void generateLangFiles(ArrayList<BaseType> types, boolean replace) {
        HashMap<String, ArrayList<BaseType>> langEntryMap = new HashMap<String, ArrayList<BaseType>>();
        
        for (BaseType baseType : types) {
            if (baseType.contentPack == null)
                continue;
            
            String contentPack = baseType.contentPack;
            
            if (!langEntryMap.containsKey(contentPack))
                langEntryMap.put(contentPack, new ArrayList<BaseType>());
            
            if (baseType.displayName != null && !langEntryMap.get(contentPack).contains(baseType))
                langEntryMap.get(contentPack).add(baseType);
        }
        
        for (String contentPack : langEntryMap.keySet()) {
            try {
                File contentPackDir = new File(ModularWarfare.CONTENT_DIR, contentPack);
                if (contentPackDir.exists() && contentPackDir.isDirectory()) {
                    ArrayList<BaseType> langEntries = langEntryMap.get(contentPack);
                    if (langEntries != null && !langEntries.isEmpty()) {
                        Path langDir = Paths.get(
                            ModularWarfare.CONTENT_DIR.getAbsolutePath() + "/" + contentPack + "/assets/modularwarfare/lang/");
                        if (!Files.exists(langDir))
                            Files.createDirectories(langDir);
                        Path langPath = Paths.get(langDir + "/en_US.lang");
                        
                        boolean langExists = Files.exists(langPath);
                        boolean shouldCreate = langExists ? replace : true;
                        if (shouldCreate) {
                            if (!langExists)
                                Files.createFile(langPath);
                            
                            ArrayList<String> langEntriesList = new ArrayList<String>();
                            String format = "item.%s.name=%s";
                            for (BaseType type : langEntries) {
                                langEntriesList.add(String.format(format, type.internalName, type.displayName));
                            }
                            // 使用 UTF-8 编码写入文件
                            StandardOpenOption[] options = langExists ? 
                                new StandardOpenOption[]{StandardOpenOption.TRUNCATE_EXISTING} : 
                                new StandardOpenOption[]{StandardOpenOption.CREATE};
                            Files.write(langPath, langEntriesList, StandardCharsets.UTF_8, options);
                            LOGGER.info("Generated lang file: " + langPath.toString());
                        }
                    }
                }
            } catch (Exception exception) {
                if (ModularWarfare.DEV_ENV) {
                    exception.printStackTrace();
                } else {
                    LOGGER.error(String.format("Failed to create lang file for content pack '%s'", contentPack));
                }
            }
        }
    }
    
    private ItemModelExport createJson(BaseType type) {
        ItemModelExport exportedModel = new ItemModelExport();
        
        // 自定义物品和方块第三人称不显示，缩放全部设置为0
        exportedModel.display.thirdperson_lefthand.scale[0] = 0f;
        exportedModel.display.thirdperson_lefthand.scale[1] = 0f;
        exportedModel.display.thirdperson_lefthand.scale[2] = 0f;
        
        exportedModel.display.thirdperson_righthand.scale[0] = 0f;
        exportedModel.display.thirdperson_righthand.scale[1] = 0f;
        exportedModel.display.thirdperson_righthand.scale[2] = 0f;
        
        // 贴图路径需要包含 assetDir 子目录
        // items/customitems/test.custom_item 或 items/customblock/test.custom_block
        String texturePath = type.getAssetDir() + "/" + (type.iconName != null ? type.iconName : type.internalName);
        // 注意：customblocks 的 assetDir 可能需要映射为 customblock（单数）
        if (type instanceof CustomBlockType) {
            texturePath = "customblock/" + (type.iconName != null ? type.iconName : type.internalName);
        }
        exportedModel.setBaseLayer(texturePath);
        return exportedModel;
    }
}

