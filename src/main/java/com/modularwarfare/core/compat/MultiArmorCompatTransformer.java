package com.modularwarfare.core.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

/**
 * MultiArmor模组兼容性转换器
 * 
 * 问题：MultiArmor模组尝试访问 ArmorType.bipedModel 字段，
 * 但在新版本中该字段的类型已从 ModelBiped 更改为 MWModelBipedBase
 * 
 * 解决方案：通过ASM字节码转换，将对 bipedModel 字段的访问保持兼容
 */
public class MultiArmorCompatTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.startsWith("mchhui.multiarmor.MultiArmorMod")) {
            FMLLog.getLogger().info("[MultiArmorCompat] 开始转换 {} 类以修复兼容性问题...", name);
            
            try {
                ClassNode classNode = new ClassNode(Opcodes.ASM5);
                ClassReader classReader = new ClassReader(basicClass);
                classReader.accept(classNode, 0);
                
                int transformCount = 0;
                
                for (MethodNode method : classNode.methods) {
                    for (AbstractInsnNode insn : method.instructions.toArray()) {
                        if (insn.getOpcode() == Opcodes.GETFIELD) {
                            FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                            
                            if ((fieldInsn.owner.equals("com/modularwarfare/common/armor/ArmorType") 
                                || fieldInsn.owner.equals("com/modularwarfare/common/type/BaseType"))
                                && fieldInsn.name.equals("bipedModel")) {
                                
                                // 修改字段描述符
                                // 从: Lnet/minecraft/client/model/ModelBiped;
                                // 到: Lcom/modularwarfare/client/objloader/MWModelBipedBase;
                                String oldDesc = fieldInsn.desc;
                                fieldInsn.desc = "Lcom/modularwarfare/client/objloader/MWModelBipedBase;";
                                
                                transformCount++;
                                FMLLog.getLogger().info("[MultiArmorCompat] 修复字段访问 (方法: {}): {}.{} {} -> {}", 
                                    method.name, fieldInsn.owner, fieldInsn.name, oldDesc, fieldInsn.desc);
                            }
                        }
                        
                        if (insn.getOpcode() == Opcodes.CHECKCAST) {
                            org.objectweb.asm.tree.TypeInsnNode typeInsn = (org.objectweb.asm.tree.TypeInsnNode) insn;
                            

                        }
                    }
                }
                
                if (transformCount > 0) {
                    // 使用 COMPUTE_MAXS 而不是 COMPUTE_FRAMES 来避免类加载问题
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    classNode.accept(classWriter);
                    
                    FMLLog.getLogger().info("[MultiArmorCompat] {} 转换完成 - 修复了 {} 处字段访问", name, transformCount);
                    return classWriter.toByteArray();
                } else {
                    FMLLog.getLogger().info("[MultiArmorCompat] {} 未发现需要修复的字段访问", name);
                }
                
            } catch (Exception e) {
                FMLLog.getLogger().error("[MultiArmorCompat] 转换失败: ", e);
                // 如果转换失败，返回原始字节码
                return basicClass;
            }
        }
        
        return basicClass;
    }
}

