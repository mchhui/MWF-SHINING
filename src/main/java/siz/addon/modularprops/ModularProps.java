package siz.addon.modularprops;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.modularwarfare.ModularWarfare;
import com.modularwarfare.common.type.BaseType;
import com.modularwarfare.utility.GSONUtils;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;
import siz.addon.modularprops.common.CommonProxy;
import siz.addon.modularprops.common.custom.BlockCustom;
import siz.addon.modularprops.common.custom.ItemBlockCustom;
import siz.addon.modularprops.common.custom.ItemCustom;
import siz.addon.modularprops.common.network.PacketCustomItemUse;
import siz.addon.modularprops.common.type.ModularPropsContentTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import moe.komi.mwprotect.IZipEntry;
import com.modularwarfare.utility.ZipContentPack;
import static com.modularwarfare.common.CommonProxy.zipJar;

@Mod(modid = ModularProps.MOD_ID, name = ModularProps.MOD_NAME, version = ModularProps.MOD_VERSION, 
     acceptedMinecraftVersions = "[1.12,1.13)", dependencies = "required-after:modularwarfare")
@Mod.EventBusSubscriber
public class ModularProps {

    public static final String MOD_ID = "modularprops";
    public static final String MOD_NAME = "ModularProps";
    public static final String MOD_VERSION = "1.0.0";

    @Mod.Instance(ModularProps.MOD_ID)
    public static ModularProps INSTANCE;

    @SidedProxy(clientSide = "siz.addon.modularprops.client.ClientProxy", 
                serverSide = "siz.addon.modularprops.common.CommonProxy")
    public static CommonProxy PROXY;

    public static Logger LOGGER;
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // 网络包通道
    public static SimpleNetworkWrapper NETWORK;

    // 内容包目录（使用MWF的目录）
    public static File CONTENT_DIR;
    public static List<File> contentPacks = new ArrayList<>();
    public static ArrayList<BaseType> baseTypes = new ArrayList<>();

    // 自定义道具和方块注册表
    public static HashMap<String, ItemCustom> customItemTypes = new HashMap<>();
    public static HashMap<String, BlockCustom> customBlockTypes = new HashMap<>();
    public static HashMap<String, Item> customBlockItemBlocks = new HashMap<>(); // 方块的ItemBlock

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        LOGGER.info("ModularProps PreInitialization");

        // 确保使用 MWF 的内容包目录
        if (CONTENT_DIR == null) {
            CONTENT_DIR = ModularWarfare.CONTENT_DIR;
        }

        // 初始化网络包系统
        NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MOD_ID);
        int packetId = 0;
        NETWORK.registerMessage(
            PacketCustomItemUse.Handler.class,
            PacketCustomItemUse.class,
            packetId++,
            Side.SERVER
        );
        LOGGER.info("Registered network packets");

        // 注册类型系统
        ModularPropsContentTypes.registerTypes();

        PROXY.preInit();
        
        // 注册到事件总线
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        LOGGER.info("ModularProps Initialization");
        PROXY.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("ModularProps PostInitialization");
        PROXY.postInit();
    }

    /**
     * 获取内容包列表（支持目录和压缩包）
     */
    private static List<File> getContentPacks() {
        List<File> packs = new ArrayList<>();
        if (CONTENT_DIR != null && CONTENT_DIR.exists()) {
            File[] files = CONTENT_DIR.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().contains("cache")) {
                        continue;
                    }
                    
                    if (file.isDirectory()) {
                        // 检查是否包含 customitems 或 customblocks 文件夹
                        File itemsFolder = new File(file, "customitems");
                        File blocksFolder = new File(file, "customblocks");
                        if (itemsFolder.exists() || blocksFolder.exists()) {
                            packs.add(file);
                            LOGGER.info("Found ModularProps content pack (directory): " + file.getName());
                        }
                    } else if (zipJar.matcher(file.getName()).matches()) {
                        // 检查压缩包中是否包含 customitems 或 customblocks 文件夹
                        // 注意：此时 zipContentsPack 可能还未初始化，我们只检查文件名
                        // 实际的文件检查会在 loadContentPacks 中进行
                        packs.add(file);
                        LOGGER.info("Found ModularProps content pack (zip): " + file.getName());
                    }
                }
            }
        } else {
            LOGGER.warn("CONTENT_DIR is null or does not exist: " + CONTENT_DIR);
        }
        LOGGER.info("Found " + packs.size() + " content packs with ModularProps content");
        return packs;
    }

    /**
     * 加载内容包（支持目录和压缩包）
     */
    public static void loadContentPacks() {
        baseTypes.clear();
        
        for (File contentPack : contentPacks) {
            LOGGER.info("Loading content pack: " + contentPack.getName());
            
            if (contentPack.isDirectory()) {
                // 从目录加载
                for (com.modularwarfare.common.type.TypeEntry type : ModularPropsContentTypes.values) {
                    File typeFolder = new File(contentPack, "/" + type.name + "/");
                    if (typeFolder.exists() && typeFolder.isDirectory()) {
                        for (File typeFile : typeFolder.listFiles()) {
                            if (typeFile.isFile() && typeFile.getName().endsWith(".json") && !typeFile.getName().contains(".render.json")) {
                                try {
                                    JsonReader jsonReader = new JsonReader(new InputStreamReader(new FileInputStream(typeFile), StandardCharsets.UTF_8));
                                    BaseType parsedType = GSONUtils.fromJson(gson, jsonReader, type.typeClass, typeFile.getName());
                                    
                                    if (parsedType != null) {
                                        parsedType.id = type.id;
                                        parsedType.contentPack = contentPack.getName();
                                        parsedType.isInDirectory = true;
                                        baseTypes.add(parsedType);
                                        
                                        LOGGER.info("Loaded " + type.name + ": " + parsedType.internalName);
                                    } else {
                                        LOGGER.error("Failed to parse type file (parsedType is null): " + typeFile.getName());
                                    }
                                } catch (FileNotFoundException ex) {
                                    LOGGER.error("Failed to load type file: " + typeFile.getName(), ex);
                                } catch (Exception ex) {
                                    LOGGER.error("Error parsing type file: " + typeFile.getName(), ex);
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }
            } else if (zipJar.matcher(contentPack.getName()).matches()) {
                // 从压缩包加载
                if (ModularWarfare.zipContentsPack.containsKey(contentPack.getName())) {
                    ZipContentPack zipPack = ModularWarfare.zipContentsPack.get(contentPack.getName());
                    for (IZipEntry fileHeader : zipPack.fileHeaders) {
                        for (com.modularwarfare.common.type.TypeEntry type : ModularPropsContentTypes.values) {
                            final String zipName = fileHeader.getFileName();
                            final String typeName = type.name;
                            // 检查文件路径是否匹配类型文件夹，且不是渲染配置文件
                            if (zipName.startsWith(typeName + "/") && 
                                zipName.split(typeName + "/").length > 1 && 
                                zipName.split(typeName + "/")[1].length() > 0 && 
                                !zipName.contains(".render.json") &&
                                zipName.endsWith(".json")) {
                                InputStream stream = null;
                                try {
                                    stream = fileHeader.getInputStream();
                                    JsonReader jsonReader = new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                                    BaseType parsedType = GSONUtils.fromJson(gson, jsonReader, type.typeClass, zipName);
                                    
                                    if (parsedType != null) {
                                        parsedType.id = type.id;
                                        parsedType.contentPack = contentPack.getName();
                                        parsedType.isInDirectory = false;
                                        baseTypes.add(parsedType);
                                        
                                        LOGGER.info("Loaded " + type.name + " from zip: " + parsedType.internalName);
                                    } else {
                                        LOGGER.error("Failed to parse type file from zip (parsedType is null): " + zipName);
                                    }
                                } catch (Exception ex) {
                                    LOGGER.error("Error parsing type file from zip: " + zipName, ex);
                                    ex.printStackTrace();
                                } finally {
                                    if (stream != null) {
                                        try {
                                            stream.close();
                                        } catch (Exception e) {
                                            // Ignore
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LOGGER.warn("Zip content pack not found in ModularWarfare.zipContentsPack: " + contentPack.getName());
                }
            }
        }
        
        // 处理类型赋值
        for (BaseType baseType : baseTypes) {
            baseType.loadExtraValues();
            ModularPropsContentTypes.values.get(baseType.id).typeAssignFunction.accept(baseType, false);
        }
        
        // 在 DEV_ENV 模式下生成 JSON 模型和语言文件
        if (ModularWarfare.DEV_ENV) {
            PROXY.generateJsonModels(baseTypes);
        }
        
        if (ModularWarfare.DEV_ENV) {
            PROXY.generateLangFiles(baseTypes, false);
        }
        
        LOGGER.info("Loaded " + baseTypes.size() + " ModularProps types");
    }


    
    /**
     * 注册方块（Forge 事件，必须在这里注册方块本身）
     */
    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        LOGGER.info("ModularProps registerBlocks event received");
        
        // 如果内容包还没有加载，先加载
        if (CONTENT_DIR == null) {
            CONTENT_DIR = ModularWarfare.CONTENT_DIR;
        }
        if (contentPacks.isEmpty()) {
            contentPacks = getContentPacks();
        }
        if (customBlockTypes.isEmpty()) {
            loadContentPacks();
        }
        
        LOGGER.info("Found " + customBlockTypes.size() + " blocks to register");
        
        // 注册方块
        for (BlockCustom block : customBlockTypes.values()) {
            if (block.getRegistryName() != null) {
                try {
                    event.getRegistry().register(block);
                    LOGGER.info("Registered block: " + block.getRegistryName());
                } catch (Exception e) {
                    LOGGER.error("Failed to register block: " + block.type.internalName, e);
                }
            } else {
                LOGGER.error("Block has null registry name: " + block.type.internalName);
            }
        }
        
        LOGGER.info("ModularProps registered " + customBlockTypes.size() + " blocks");
    }
    
    /**
     * 注册物品（使用 Forge 的 RegistryEvent，确保在 MWF 的 ItemRegisterEvent 之前注册）
     */
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        LOGGER.info("ModularProps registerItems event received");
        
        // 如果内容包还没有加载，先加载
        if (CONTENT_DIR == null) {
            CONTENT_DIR = ModularWarfare.CONTENT_DIR;
        }
        if (contentPacks.isEmpty()) {
            contentPacks = getContentPacks();
        }
        if (customItemTypes.isEmpty() && customBlockItemBlocks.isEmpty()) {
            loadContentPacks();
        }
        
        LOGGER.info("Found " + customItemTypes.size() + " custom items and " + customBlockItemBlocks.size() + " block items to register");
        
        // 注册自定义道具
        for (ItemCustom item : customItemTypes.values()) {
            if (item.getRegistryName() != null) {
                try {
                    event.getRegistry().register(item);
                    LOGGER.info("Registered custom item: " + item.getRegistryName());
                } catch (Exception e) {
                    LOGGER.error("Failed to register custom item: " + item.type.internalName, e);
                }
            } else {
                LOGGER.error("Item has null registry name: " + (item.type != null ? item.type.internalName : "null type"));
            }
        }
        
        // 注册方块的ItemBlock
        for (Item itemBlock : customBlockItemBlocks.values()) {
            if (itemBlock.getRegistryName() != null) {
                try {
                    event.getRegistry().register(itemBlock);
                    LOGGER.info("Registered block item: " + itemBlock.getRegistryName());
                } catch (Exception e) {
                    LOGGER.error("Failed to register block item", e);
                }
            } else {
                LOGGER.error("ItemBlock has null registry name");
            }
        }
        
        LOGGER.info("ModularProps registered " + customItemTypes.size() + " items and " + customBlockItemBlocks.size() + " block items");
    }
    
    /**
     * 监听 MWF 的 ItemRegisterEvent，将我们的物品添加到创造栏顺序中
     * 注意：MWF 按内容包逐个触发此事件，我们需要根据内容包名称过滤物品
     */
    @SubscribeEvent
    public static void onItemRegister(com.modularwarfare.api.ItemRegisterEvent event) {
        LOGGER.info("ModularProps ItemRegisterEvent received, adding items to creative tab...");
        
        // 从 tabOrder 中推断当前内容包名称（MWF 会先注册该内容包的物品）
        String currentContentPack = null;
        if (!event.tabOrder.isEmpty() && event.tabOrder.get(0) instanceof com.modularwarfare.common.type.BaseItem) {
            com.modularwarfare.common.type.BaseItem firstItem = (com.modularwarfare.common.type.BaseItem) event.tabOrder.get(0);
            if (firstItem.baseType != null) {
                currentContentPack = firstItem.baseType.contentPack;
                LOGGER.info("Detected content pack: " + currentContentPack);
            }
        }
        
        if (currentContentPack == null) {
            LOGGER.warn("Could not determine content pack from ItemRegisterEvent");
            return;
        }
        
        int addedItems = 0;
        int addedBlocks = 0;
        
        // 添加自定义道具到创造栏顺序（按内容包过滤）
        for (ItemCustom item : customItemTypes.values()) {
            if (item.type != null && item.type.contentPack.equals(currentContentPack)) {
                if (!event.tabOrder.contains(item)) {
                    event.tabOrder.add(item);
                    addedItems++;
                    LOGGER.info("Added custom item to creative tab: " + item.getRegistryName());
                }
            }
        }
        
        // 添加方块的ItemBlock到创造栏顺序（按内容包过滤）
        for (Item itemBlock : customBlockItemBlocks.values()) {
            if (itemBlock.getRegistryName() != null) {
                BlockCustom block = customBlockTypes.get(itemBlock.getRegistryName().getPath());
                if (block != null && block.type != null && block.type.contentPack.equals(currentContentPack)) {
                    // 确保创造栏已设置（ItemBlockCustom 会通过 getCreativeTab() 动态获取，但这里也设置一下以确保兼容性）
                    if (itemBlock instanceof ItemBlockCustom && ModularWarfare.MODS_TABS.containsKey(currentContentPack)) {
                        ItemBlockCustom itemBlockCustom = (ItemBlockCustom) itemBlock;
                        itemBlockCustom.setCreativeTab(ModularWarfare.MODS_TABS.get(currentContentPack));
                        LOGGER.info("Set creative tab for block item: " + itemBlock.getRegistryName() + " -> " + currentContentPack);
                    }
                    
                    if (!event.tabOrder.contains(itemBlock)) {
                        event.tabOrder.add(itemBlock);
                        addedBlocks++;
                        LOGGER.info("Added block item to creative tab order: " + itemBlock.getRegistryName());
                    } else {
                        LOGGER.warn("Block item already in tab order: " + itemBlock.getRegistryName());
                    }
                } else {
                    if (block == null) {
                        LOGGER.warn("Block not found for itemBlock: " + itemBlock.getRegistryName());
                    } else if (block.type == null) {
                        LOGGER.warn("Block type is null for itemBlock: " + itemBlock.getRegistryName());
                    } else if (!block.type.contentPack.equals(currentContentPack)) {
                        LOGGER.debug("Block item belongs to different content pack: " + itemBlock.getRegistryName() + " (expected: " + currentContentPack + ", actual: " + block.type.contentPack + ")");
                    }
                }
            } else {
                LOGGER.warn("ItemBlock has null registry name");
            }
        }
        
        LOGGER.info("ModularProps added " + addedItems + " items and " + addedBlocks + " block items to creative tab");
    }

    /**
     * 监听 MWF 的类型注册事件，在这里加载内容包
     */
    @SubscribeEvent
    public void onTypeRegister(com.modularwarfare.api.TypeRegisterEvent event) {
        LOGGER.info("ModularProps received TypeRegisterEvent, loading content packs...");
        
        // 获取内容包目录（使用 MWF 的目录）
        if (CONTENT_DIR == null) {
            CONTENT_DIR = ModularWarfare.CONTENT_DIR;
        }
        
        // 加载内容包
        if (contentPacks.isEmpty()) {
            contentPacks = getContentPacks();
        }
        loadContentPacks();
        
        LOGGER.info("ModularProps content packs loaded");
    }
}

