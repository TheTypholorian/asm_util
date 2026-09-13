package net.typho.asm_util

import net.typho.asm_util.insn.InsnPointer
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.util.function.BiConsumer
import java.util.function.Function

object ASMUtil {
    @JvmStatic
    fun createSysOut(message: String): InsnList {
        return InsnList().apply {
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
    }

    @JvmStatic
    @JvmOverloads
    fun ClassNode.copyTo(other: ClassNode, output: Function<ClassNode, ClassVisitor> = { it }) {
        other.version = 0
        other.access = 0
        other.name = null
        other.signature = null
        other.superName = null
        other.sourceFile = null
        other.sourceDebug = null
        other.outerClass = null
        other.outerMethod = null
        other.outerMethodDesc = null
        other.nestHostClass = null
        other.module = null
        other.nestMembers = null
        other.permittedSubclasses = null

        other.interfaces.clear()
        other.fields.clear()
        other.methods.clear()
        other.innerClasses.clear()
        other.recordComponents = null

        other.visibleAnnotations = null
        other.invisibleAnnotations = null
        other.visibleTypeAnnotations = null
        other.invisibleTypeAnnotations = null
        other.attrs = null

        accept(output.apply(other))
    }

    @JvmStatic
    fun AnnotationNode.forEach(out: BiConsumer<String?, Any?>) {
        values?.let {
            val iterator = it.iterator()

            while (iterator.hasNext()) {
                out.accept(iterator.next() as String?, iterator.next())
            }
        }
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
        replacement: InsnList
    ) {
        val at = at.findOrThrow(this)

        insertBefore(at, replacement)
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
        replacement: InsnList
    ) {
        val from = from.findOrThrow(this)
        val to = to.findOrThrow(this)

        insertBefore(from, replacement)

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
    fun InsnList.iterateSlice(
        after: Set<AbstractInsnNode>,
        before: Set<AbstractInsnNode>
    ): Iterable<AbstractInsnNode> {
        if (after.isEmpty() && before.isEmpty()) {
            return this
        }

        val result = mutableListOf<AbstractInsnNode>()
        val found = mutableSetOf<AbstractInsnNode>()
        var valid = after.isEmpty()

        for (node in this) {
            if (before.contains(node)) {
                break
            }

            if (valid) {
                result.add(node)
            } else if (after.contains(node)) {
                found.add(node)

                if (after.size == found.size) {
                    valid = true
                }
            }
        }

        return result
    }
}