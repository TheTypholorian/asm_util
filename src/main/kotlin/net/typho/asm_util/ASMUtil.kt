package net.typho.asm_util

import net.typho.asm_util.insn.InsnPointer
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
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
    fun AnnotationNode.forEach(out: (name: String, value: Any) -> Unit) {
        val iterator = values.iterator()

        while (iterator.hasNext()) {
            out(iterator.next() as String, iterator.next())
        }
    }

    @Suppress("UNCHECKED_CAST")
    @get:JvmName("readKotlinMetadata")
    @JvmStatic
    val AnnotationNode.kotlinMetadata: Metadata?
        get() {
            if (desc != "Lkotlin/Metadata;") {
                return null
            }

            var kind = 1
            var metadataVersion = intArrayOf()
            var bytecodeVersion = intArrayOf(1, 0, 3)
            var data1 = arrayOf<String>()
            var data2 = arrayOf<String>()
            var extraString = ""
            var packageName = ""
            var extraInt = 0

            forEach { name, value ->
                when (name) {
                    "k" -> kind = value as Int
                    "mv" -> metadataVersion = (value as List<Int>).toIntArray()
                    "bv" -> bytecodeVersion = (value as List<Int>).toIntArray()
                    "d1" -> data1 = (value as List<String>).toTypedArray()
                    "d2" -> data2 = (value as List<String>).toTypedArray()
                    "xs" -> extraString = value as String
                    "pn" -> packageName = value as String
                    "xi" -> extraInt = value as Int
                }
            }

            return Metadata(kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt)
        }

    @JvmStatic
    fun AnnotationVisitor.visitKotlinMetadata(metadata: Metadata) {
        visit("k", metadata.kind)
        visitArray("mv").apply { metadata.metadataVersion.forEach { visit(null, it) } }
        visitArray("bv").apply { metadata.bytecodeVersion.forEach { visit(null, it) } }
        visitArray("d1").apply { metadata.data1.forEach { visit(null, it) } }
        visitArray("d2").apply { metadata.data2.forEach { visit(null, it) } }
        visit("xs", metadata.extraString)
        visit("pn", metadata.packageName)
        visit("xi", metadata.extraInt)
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
        replacement: InsnList.() -> Unit
    ) {
        val at = at.findOrThrow(this)

        val insns = InsnList()
        replacement(insns)

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
        replacement: InsnList.() -> Unit
    ) {
        val from = from.findOrThrow(this)
        val to = to.findOrThrow(this)

        val insns = InsnList()
        replacement(insns)

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