package mchhui.modularmovements.coremod.minecraft;

import org.objectweb.asm.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.FMLLog;

public class EntityPlayerSP implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.client.entity.EntityPlayerSP")) {
            FMLLog.log.warn("[Transforming:net.minecraft.client.entity.EntityPlayerSP]");
            ClassNode classNode = new ClassNode(Opcodes.ASM5);
            ClassReader classReader = new ClassReader(basicClass);
            classReader.accept(classNode, 0);
            MethodNode methodNode = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "getPositionEyes",
                    "(F)Lnet/minecraft/util/math/Vec3d;", null, null);
            methodNode.visitMaxs(2, 2);
            InsnList list = new InsnList();
            LabelNode startLabelNode = new LabelNode();
            LabelNode endLabelNode = new LabelNode();
            list.add(startLabelNode);
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new VarInsnNode(Opcodes.FLOAD, 1));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mchhui/modularmovements/coremod/ModularMovementsHooks", "onGetPositionEyes",
                    "(Lnet/minecraft/entity/player/EntityPlayer;F)Lnet/minecraft/util/math/Vec3d;", false));
            list.add(endLabelNode);
            list.add(new InsnNode(Opcodes.ARETURN));
            methodNode.localVariables.add(new LocalVariableNode("this", "Lnet/minecraft/client/entity/EntityPlayerSP;",
                    null, startLabelNode, endLabelNode, 0));
            methodNode.localVariables
                    .add(new LocalVariableNode("partialTicks", "F", null, startLabelNode, endLabelNode, 1));
            methodNode.instructions.add(list);
            classNode.methods.add(methodNode);

            methodNode = new MethodNode(Opcodes.ASM5, Opcodes.ACC_PUBLIC, "func_174824_e",
                    "(F)Lnet/minecraft/util/math/Vec3d;", null, null);
            methodNode.visitMaxs(2, 2);
            list = new InsnList();
            startLabelNode = new LabelNode();
            endLabelNode = new LabelNode();
            list.add(startLabelNode);
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new VarInsnNode(Opcodes.FLOAD, 1));
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "mchhui/modularmovements/coremod/ModularMovementsHooks", "onGetPositionEyes",
                    "(Lnet/minecraft/entity/player/EntityPlayer;F)Lnet/minecraft/util/math/Vec3d;", false));
            list.add(endLabelNode);
            list.add(new InsnNode(Opcodes.ARETURN));
            methodNode.localVariables.add(new LocalVariableNode("this", "Lnet/minecraft/client/entity/EntityPlayerSP;",
                    null, startLabelNode, endLabelNode, 0));
            methodNode.localVariables
                    .add(new LocalVariableNode("partialTicks", "F", null, startLabelNode, endLabelNode, 1));
            methodNode.instructions.add(list);
            classNode.methods.add(methodNode);
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            FMLLog.log.warn("[Transformed:net.minecraft.client.entity.EntityPlayerSP]");
            return classWriter.toByteArray();
        }
        return basicClass;
    }

}
