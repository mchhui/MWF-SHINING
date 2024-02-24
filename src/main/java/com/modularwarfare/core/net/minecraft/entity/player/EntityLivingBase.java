package com.modularwarfare.core.net.minecraft.entity.player;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

public class EntityLivingBase implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.equals("net.minecraft.entity.EntityLivingBase")||name.equals("vp")) {
            FMLLog.getLogger().warn("[Transforming:net.minecraft.entity.EntityLivingBase]");
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            classNode.methods.forEach((method) -> {
                if (method.name.equals("updateElytra") || method.name.equals("func_184616_r")
                    || method.name.equals("r")) {
                    InsnList list = new InsnList();
                    list.add(new LabelNode());
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/modularwarfare/core/MWFCoreHooks",
                        "updateElytra", "(Lnet/minecraft/entity/EntityLivingBase;)V", false));
                    list.add(new InsnNode(Opcodes.RETURN));
                    list.add(new LabelNode());
                    method.instructions.clear();
                    method.instructions.insert(list);
                    FMLLog.getLogger().warn("[Transformed:updateElytra]");
                }
            });
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            FMLLog.getLogger().warn("[Transformed:net.minecraft.entity.EntityLivingBase]");
            return classWriter.toByteArray();
        }
        return basicClass;
    }

}
