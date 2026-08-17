package net.typho.asm_util

import net.typho.asm_util.ASMUtil.copyTo
import net.typho.asm_util.error.ClassVisitException
import net.typho.asm_util.field.FieldPointer.Companion.field
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.util.function.BiConsumer
import java.util.function.BiFunction

interface ClassTransformInfo {
    var node: ClassNode
    var fallbackErrorSource: Any?

    fun error(error: String) = error(error, null)

    fun error(error: String, source: Any?)

    fun checkErrors()

    fun markChanged() {
    }

    fun computeMaxStacks() {
    }

    fun computeFrames() {
    }

    companion object {
        @JvmStatic
        fun checkErrors(errors: List<Pair<String, Any?>>, className: () -> String) {
            if (!errors.isEmpty()) {
                throw ClassVisitException((if (errors.size == 1) "Error" else "Errors") + " while transforming class ${className()}:\n${errors.joinToString(separator = "\n", transform = { (error, source) -> "$error (caused by '${source ?: "unknown"}')" })}")
            }
        }
    }

    open class AgentTransform(
        bytes: ByteArray
    ) : ClassTransformInfo {
        @JvmField
        protected val lazyNode = LazyClassNode(bytes)
        override var node: ClassNode by lazyNode::node
        var writerFactory: BiFunction<ClassReader?, Int, ClassWriter>? = null
            set(value) {
                if (field != null) {
                    throw IllegalStateException("Cannot set ClassOutputInfo factory more than once")
                }

                field = value
            }
        @JvmField
        protected var changed = false
        @JvmField
        protected var writerFlags = 0
        @JvmField
        protected val errors = mutableListOf<Pair<String, Any?>>()
        override var fallbackErrorSource: Any? = null

        override fun markChanged() {
            changed = true
        }

        override fun computeMaxStacks() {
            writerFlags = writerFlags or ClassWriter.COMPUTE_MAXS
        }

        override fun computeFrames() {
            writerFlags = writerFlags or ClassWriter.COMPUTE_FRAMES
        }

        override fun error(error: String, source: Any?) {
            errors.add(error to (source ?: fallbackErrorSource))
        }

        override fun checkErrors() {
            checkErrors(errors) { node.name }
        }

        fun createWriter(): ClassWriter? {
            val reader = lazyNode.reader ?: return null

            return if (changed) writerFactory?.apply(reader, writerFlags) ?: ClassWriter(reader, writerFlags) else null
        }

        @JvmOverloads
        fun compile(debugOut: BiConsumer<String, ByteArray>? = null): ByteArray? {
            val bytes = createWriter()?.let {
                node.accept(it)
                val bytes = it.toByteArray()
                debugOut?.accept(node.name, bytes)
                bytes
            }
            checkErrors()
            return bytes
        }
    }

    open class Wrapper(
        node: ClassNode
    ) : ClassTransformInfo {
        @Suppress("SetterBackingFieldAssignment")
        override var node: ClassNode = node
            set(value) {
                value.copyTo(node)
            }
        @JvmField
        protected val errors = mutableListOf<Pair<String, Any?>>()
        @JvmField
        var changed = false
        override var fallbackErrorSource: Any? = null

        override fun markChanged() {
            changed = true
        }

        override fun error(error: String, source: Any?) {
            errors.add(error to (source ?: fallbackErrorSource))
        }

        override fun checkErrors() {
            checkErrors(errors) { node.name }
        }
    }

    open class MutableWrapper(
        override var node: ClassNode
    ) : Wrapper(node)
}