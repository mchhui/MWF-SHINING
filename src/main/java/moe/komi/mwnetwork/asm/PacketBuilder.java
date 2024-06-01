package moe.komi.mwnetwork.asm;

import com.modularwarfare.common.network.PacketBase;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public class PacketBuilder {
    private final ConcurrentHashMap<Byte, AsmPacketBuilderInterface> executor = new ConcurrentHashMap<>();

    public PacketBase build(byte packetId) {
        AsmPacketBuilderInterface asmPacketBuilderInterface = executor.get(packetId);
        if (asmPacketBuilderInterface == null) {
            return null;
        }
        return asmPacketBuilderInterface.newPacket();
    }

    public boolean register(byte packetId, Class<? extends PacketBase> clazz) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        String className = "moe/komi/mwnetwork/asm/AsmPacketBuilder" + (packetId & 0xff);
        writer.visit(V1_8, ACC_PUBLIC, className, null, Type.getInternalName(Object.class), new String[] {Type.getInternalName(AsmPacketBuilderInterface.class)});
        GeneratorAdapter methodGenerator = new GeneratorAdapter(writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null), ACC_PUBLIC, "<init>", "()V");
        methodGenerator.loadThis();
        methodGenerator.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        methodGenerator.returnValue();
        methodGenerator.endMethod();
        methodGenerator = new GeneratorAdapter(writer.visitMethod(ACC_PUBLIC, "newPacket", "()Lcom/modularwarfare/common/network/PacketBase;", null, null), ACC_PUBLIC, "newPacket", "()Lcom/modularwarfare/common/network/PacketBase;");
        methodGenerator.newInstance(Type.getType(clazz));
        methodGenerator.dup();
        methodGenerator.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(clazz), "<init>", "()V", false);
        methodGenerator.visitInsn(ARETURN);
        methodGenerator.endMethod();
        writer.visitEnd();
        byte[] classByteAry = writer.toByteArray();
        try {
            Class<?> definedClass = defineClass(classByteAry);
            executor.put(packetId, (AsmPacketBuilderInterface) definedClass.newInstance());
            return true;
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            return false;
        }
    }

    private static Class<?> defineClass(byte[] classByteAry) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ClassLoader cl = PacketBuilder.class.getClassLoader();
        Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
        defineClass.setAccessible(true);
        Class<?> result = (Class<?>) defineClass.invoke(cl, classByteAry, 0, classByteAry.length);
        Method resolveClass = ClassLoader.class.getDeclaredMethod("resolveClass", Class.class);
        resolveClass.setAccessible(true);
        resolveClass.invoke(cl, result);
        return result;
    }
}
