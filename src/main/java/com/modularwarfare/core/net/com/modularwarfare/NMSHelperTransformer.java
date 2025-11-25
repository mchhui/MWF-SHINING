package com.modularwarfare.core.net.com.modularwarfare;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

import java.util.HashMap;
import java.util.Map;

/**
 * 自动将 NMS 类名转换为 Forge 类名的 Transformer
 * 例如：net.minecraft.server.v1_12_R1.Entity -> net.minecraft.entity.Entity
 */
public class NMSHelperTransformer implements IClassTransformer {

    /**
     * NMS 类名到 Forge 类名的映射表
     * key: NMS 类名（内部名称格式，如 net/minecraft/server/v1_12_R1/Entity）
     * value: Forge 类名（内部名称格式，如 net/minecraft/entity/Entity）
     */
    private static final Map<String, String> NMS_TO_FORGE_MAPPING = new HashMap<>();

    static {
        // 初始化映射表
        // Entity 相关
        NMS_TO_FORGE_MAPPING.put("net/minecraft/server/v1_12_R1/Entity", "net/minecraft/entity/Entity");
        NMS_TO_FORGE_MAPPING.put("net.minecraft.server.v1_12_R1.Entity", "net.minecraft.entity.Entity");
        
        // ItemStack 相关
        NMS_TO_FORGE_MAPPING.put("net/minecraft/server/v1_12_R1/ItemStack", "net/minecraft/item/ItemStack");
        NMS_TO_FORGE_MAPPING.put("net.minecraft.server.v1_12_R1.ItemStack", "net.minecraft.item.ItemStack");
        
        // 可以根据需要添加更多映射
        // NMS_TO_FORGE_MAPPING.put("net/minecraft/server/v1_12_R1/World", "net/minecraft/world/World");
        // NMS_TO_FORGE_MAPPING.put("net.minecraft.server.v1_12_R1.World", "net.minecraft.world.World");
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.equals("com.modularwarfare.NMSHelper")) {
            FMLLog.getLogger().warn("[Transforming:com.modularwarfare.NMSHelper]");
            
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            
            int transformCount = 0;
            
            // 遍历所有方法
            for (MethodNode method : classNode.methods) {
                // 遍历方法中的所有指令
                for (AbstractInsnNode insn : method.instructions.toArray()) {
                    // 处理类型转换指令 (CHECKCAST)
                    if (insn.getOpcode() == Opcodes.CHECKCAST) {
                        TypeInsnNode typeInsn = (TypeInsnNode) insn;
                        String originalType = typeInsn.desc;
                        String mappedType = mapNmsToForge(originalType);
                        if (!originalType.equals(mappedType)) {
                            typeInsn.desc = mappedType;
                            transformCount++;
                            FMLLog.getLogger().warn("[NMS->Forge] CHECKCAST: {} -> {}", originalType, mappedType);
                        }
                    }
                    
                    // 处理方法调用指令中的类型描述符
                    if (insn instanceof MethodInsnNode) {
                        MethodInsnNode methodInsn = (MethodInsnNode) insn;
                        
                        // 转换方法所有者类名
                        String owner = mapNmsToForge(methodInsn.owner);
                        if (!owner.equals(methodInsn.owner)) {
                            methodInsn.owner = owner;
                            transformCount++;
                            FMLLog.getLogger().warn("[NMS->Forge] Method owner: {} -> {}", methodInsn.owner, owner);
                        }
                        
                        // 转换方法描述符中的类型
                        String desc = mapDescriptor(methodInsn.desc);
                        if (!desc.equals(methodInsn.desc)) {
                            methodInsn.desc = desc;
                            transformCount++;
                            FMLLog.getLogger().warn("[NMS->Forge] Method desc: {} -> {}", methodInsn.desc, desc);
                        }
                    }
                }
            }
            
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(classWriter);
            
            FMLLog.getLogger().warn("[Transformed:com.modularwarfare.NMSHelper] - 转换了 {} 处类名引用", transformCount);
            return classWriter.toByteArray();
        }
        return basicClass;
    }

    /**
     * 将 NMS 类名映射为 Forge 类名
     * @param className 类名（可以是内部名称格式或二进制名称格式）
     * @return 映射后的类名，如果没有映射则返回原类名
     */
    private String mapNmsToForge(String className) {
        // 先尝试直接映射
        String mapped = NMS_TO_FORGE_MAPPING.get(className);
        if (mapped != null) {
            return mapped;
        }
        
        // 尝试将二进制名称转换为内部名称后再映射
        String internalName = className.replace('.', '/');
        mapped = NMS_TO_FORGE_MAPPING.get(internalName);
        if (mapped != null) {
            return mapped;
        }
        
        // 检查是否是 NMS 包下的类，尝试自动转换
        if (className.startsWith("net.minecraft.server.v1_12_R1.") || 
            className.startsWith("net/minecraft/server/v1_12_R1/")) {
            // 自动将 net.minecraft.server.v1_12_R1.XXX 转换为 net.minecraft.XXX
            String autoMapped = className
                .replace("net.minecraft.server.v1_12_R1.", "net.minecraft.")
                .replace("net/minecraft/server/v1_12_R1/", "net/minecraft/");
            
            // 将 Entity -> entity.Entity, ItemStack -> item.ItemStack 等
            // 这里需要根据实际情况调整，因为 NMS 的包结构可能与 Forge 不同
            FMLLog.getLogger().warn("[NMS->Forge] 自动映射尝试: {} -> {}", className, autoMapped);
            return autoMapped;
        }
        
        return className;
    }

    /**
     * 映射类型描述符中的类名
     * @param descriptor 类型描述符，如 "(Lnet/minecraft/server/v1_12_R1/Entity;)V"
     * @return 映射后的描述符
     */
    private String mapDescriptor(String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        Type returnType = methodType.getReturnType();
        Type[] argumentTypes = methodType.getArgumentTypes();
        
        boolean changed = false;
        
        // 映射返回类型
        String mappedReturnType = mapType(returnType);
        if (!mappedReturnType.equals(returnType.getDescriptor())) {
            changed = true;
        }
        
        // 映射参数类型
        StringBuilder newDesc = new StringBuilder("(");
        for (Type argType : argumentTypes) {
            String mappedArg = mapType(argType);
            newDesc.append(mappedArg);
            if (!mappedArg.equals(argType.getDescriptor())) {
                changed = true;
            }
        }
        newDesc.append(")").append(mappedReturnType);
        
        return changed ? newDesc.toString() : descriptor;
    }

    /**
     * 映射单个类型
     * @param type ASM Type 对象
     * @return 映射后的类型描述符
     */
    private String mapType(Type type) {
        if (type.getSort() == Type.OBJECT) {
            String className = type.getInternalName();
            String mapped = mapNmsToForge(className);
            if (!mapped.equals(className)) {
                return "L" + mapped + ";";
            }
        } else if (type.getSort() == Type.ARRAY) {
            // 对于数组类型，需要递归处理元素类型
            Type elementType = type.getElementType();
            String mappedElement = mapType(elementType);
            if (!mappedElement.equals(elementType.getDescriptor())) {
                // 计算数组维度
                int dimensions = type.getDimensions();
                StringBuilder arrayDesc = new StringBuilder();
                for (int i = 0; i < dimensions; i++) {
                    arrayDesc.append('[');
                }
                arrayDesc.append(mappedElement);
                return arrayDesc.toString();
            }
        }
        return type.getDescriptor();
    }
}
