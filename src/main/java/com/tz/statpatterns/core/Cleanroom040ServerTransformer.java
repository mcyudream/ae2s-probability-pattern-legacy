package com.tz.statpatterns.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

/** Registered exclusively on the official CR 0.4.0 dedicated-server release with this discovery bug. */
public final class Cleanroom040ServerTransformer implements IClassTransformer {
    @Override public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraftforge.fml.common.Loader".equals(transformedName)) return bytes;
        ClassNode loader = new ClassNode();
        new ClassReader(bytes).accept(loader, 0);
        int removed = 0;
        for (var method : loader.methods) {
            if (!method.name.equals("identifyMods")
                || !method.desc.equals("(Ljava/util/List;)Lnet/minecraftforge/fml/common/discovery/ModDiscoverer;")) continue;
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC
                    && call.owner.equals("com/cleanroommc/kirino/KirinoCore")
                    && call.name.equals("identifyMods") && call.desc.equals("(Ljava/util/List;)V")) {
                    // Preserve the consumed List and all existing stack-map frames.
                    method.instructions.set(call, new InsnNode(Opcodes.POP));
                    removed++;
                }
            }
        }
        if (removed == 0) return bytes; // Another compatibility mod may already have removed it.
        if (removed != 1) throw new IllegalStateException("Unexpected CR 0.4.0 Kirino discovery shape: " + removed);
        ClassWriter writer = new ClassWriter(0);
        loader.accept(writer);
        System.out.println("[Probability Pattern compatibility] Isolated client Kirino discovery from the Cleanroom "
            + com.cleanroommc.common.CleanroomVersion.getVersion() + " dedicated server");
        return writer.toByteArray();
    }
}
