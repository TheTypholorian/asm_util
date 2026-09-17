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
import java.util.function.Function

object ASMUtil {
    const val ACC_ALL = Opcodes.ACC_PUBLIC or Opcodes.ACC_PRIVATE or Opcodes.ACC_PROTECTED

    @JvmStatic
    fun accessPublic(modifiers: Int): Int {
        return if (modifiers and Opcodes.ACC_PUBLIC == 0) {
            (modifiers and ACC_ALL.inv()) or Opcodes.ACC_PUBLIC
        } else modifiers
    }

    @JvmStatic
    fun accessProtected(modifiers: Int): Int {
        return if (modifiers and Opcodes.ACC_PUBLIC == 0 && modifiers and Opcodes.ACC_PROTECTED == 0) {
            (modifiers and ACC_ALL.inv()) or Opcodes.ACC_PROTECTED
        } else modifiers
    }

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
    fun AnnotationNode.mapIterator(): MutableListIterator<Pair<String?, Any?>> {
        val iterator = (values ?: mutableListOf<Any?>().also { values = it }).listIterator()

        return object : MutableListIterator<Pair<String?, Any?>> {
            override fun add(element: Pair<String?, Any?>) {
                iterator.add(element.first)
                iterator.add(element.second)
            }

            override fun hasNext(): Boolean {
                return iterator.hasNext()
            }

            override fun next(): Pair<String?, Any?> {
                return iterator.next() as String? to iterator.next()
            }

            override fun remove() {
                iterator.remove()
                iterator.previous()
                iterator.remove()
            }

            override fun set(element: Pair<String?, Any?>) {
                iterator.set(element.second)
                iterator.previous()
                iterator.set(element.first)
                iterator.next()
            }

            override fun hasPrevious(): Boolean {
                return iterator.hasPrevious()
            }

            override fun previous(): Pair<String?, Any?> {
                val value = iterator.previous()
                return iterator.previous() as String? to value
            }

            override fun nextIndex(): Int {
                return iterator.nextIndex()
            }

            override fun previousIndex(): Int {
                return iterator.previousIndex()
            }
        }
    }

    @JvmStatic
    operator fun AnnotationNode.get(key: String): Any? {
        mapIterator().forEach { (key1, value) ->
            if (key == key1) {
                return value
            }
        }

        return null
    }

    @JvmStatic
    operator fun AnnotationNode.set(key: String, value: Any?) {
        val it = mapIterator()

        while (it.hasNext()) {
            val (key1, value1) = it.next()

            if (key == key1) {
                it.set(key to value)
                break
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