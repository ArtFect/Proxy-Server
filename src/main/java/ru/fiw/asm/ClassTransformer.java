package ru.fiw.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

public class ClassTransformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (!transformedName.startsWith("net.minecraft.network.NetworkManager") && !transformedName.startsWith("net.minecraft.network.gw")) {
            return bytes;
        }

        ClassReader classReader = new ClassReader(bytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        for (MethodNode method : classNode.methods) {
            String mappedMethodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(classNode.name, method.name,
                    method.desc);
            if (mappedMethodName.equals("initChannel")) {
                AbstractInsnNode currentInsn = null;
                Iterator<AbstractInsnNode> insnIterator = method.instructions.iterator();
                while (insnIterator.hasNext()) {
                    currentInsn = insnIterator.next();
                    if (currentInsn.getOpcode() == Opcodes.RETURN) {
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "ru/fiw/proxyserver/ProxyServer",
                                "hook", "(Lio/netty/channel/Channel;)V", false));
                        method.instructions.insertBefore(currentInsn, insnList);
                        break;
                    }
                }
                break;
            }
        }
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }
}