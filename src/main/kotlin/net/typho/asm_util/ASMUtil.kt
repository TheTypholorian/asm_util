package net.typho.asm_util

import net.typho.asm_util.insn.InsnPointer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.util.function.Consumer

object ASMUtil {
    @JvmStatic
    fun InsnList.addSysOut(message: String) {
        add(FieldInsnNode(
            Opcodes.GETSTATIC,
            "java/lang/System",
            "out",
            "Ljava/io/PrintStream;"
        ))
        add(LdcInsnNode(message))
        add(MethodInsnNode(
            Opcodes.INVOKEVIRTUAL,
            "java/io/PrintStream",
            "println",
            "(Ljava/lang/String;)V"
        ))
    }

    @JvmStatic
    fun InsnList.splice(
        at: InsnPointer<*, *>
    ) {
        remove(at.findOrThrow(this))
    }

    @JvmStatic
    fun InsnList.splice(
        at: InsnPointer<*, *>,
        replacement: Consumer<InsnList>
    ) {
        val at = at.findOrThrow(this)

        val insns = InsnList()
        replacement.accept(insns)

        insertBefore(at, insns)
        remove(at)
    }

    @JvmStatic
    fun InsnList.splice(
        from: InsnPointer<*, *>,
        to: InsnPointer<*, *>
    ) {
        val from = from.findOrThrow(this)
        val to = to.findOrThrow(this)

        if (from === to) {
            remove(from)
        } else {
            while (from.next !== to) {
                remove(from.next ?: throw IllegalStateException("Splice ran off the edge of the InsnList"))
            }

            remove(from)
            remove(to)
        }
    }

    @JvmStatic
    fun InsnList.splice(
        from: InsnPointer<*, *>,
        to: InsnPointer<*, *>,
        replacement: Consumer<InsnList>
    ) {
        val from = from.findOrThrow(this)
        val to = to.findOrThrow(this)

        val insns = InsnList()
        replacement.accept(insns)

        insertBefore(from, insns)

        if (from === to) {
            remove(from)
        } else {
            while (from.next !== to) {
                remove(from.next ?: throw IllegalStateException("Splice ran off the edge of the InsnList"))
            }

            remove(from)
            remove(to)
        }
    }
}