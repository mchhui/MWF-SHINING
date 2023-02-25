package mchhui.modularmovements.coremod.minecraft;

import org.objectweb.asm.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

public class Entity implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.entity.Entity")) {
            FMLLog.log.warn("[net.minecraft.entity.Entity]");
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("setEntityBoundingBox")) {
                    InsnList list = new InsnList();
                    list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    list.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mchhui/modularmovements/coremod/ModularMovementsHooks",
                            "getEntityBoundingBox",
                            "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;)Lnet/minecraft/util/math/AxisAlignedBB;",
                            false));
                    list.add(new VarInsnNode(Opcodes.ASTORE, 1));
                    list.add(new LabelNode());
                    method.instructions.insert(method.instructions.getFirst(), list);
                }
            }
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            FMLLog.log.warn("[net.minecraft.entity.Entity]");
            return classWriter.toByteArray();
        }
        return basicClass;
    }

}
