package com.modularwarfare.core.net.optifine.shaders;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * <b><s>
 * 奶奶滴，玩阴的是吧？那就来吧！<br>
 * optifine my ass :)
 * <s><b>
 */

public class ShadersRender implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.equals("net.optifine.shaders.ShadersRender")) {
            FMLLog.getLogger().warn("[Transforming:net.optifine.shaders.ShadersRender]");
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("renderHand0")) {
                    InsnList list = new InsnList();
                    list.add(new LabelNode());
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/modularwarfare/core/MWFCoreHooks", "onRender0", "()V", false));
                    method.instructions.insertBefore(method.instructions.getFirst(), list);
                }
                if (method.name.equals("renderHand1")) {
                    InsnList list = new InsnList();
                    list.add(new LabelNode());
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/modularwarfare/core/MWFCoreHooks", "onRender1", "()V", false));
                    method.instructions.insertBefore(method.instructions.getFirst(), list);
                }
            }
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            FMLLog.getLogger().warn("[Transformed:net.optifine.shaders.ShadersRender]");
            return classWriter.toByteArray();
        }
        return basicClass;
    }

}
