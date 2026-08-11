package net.typho.asm_util

import net.typho.asm_util.error.ClassVisitException
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

interface ClassTransformInfo {
    val node: ClassNode
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
        override val node: ClassNode by lazyNode
        var writerFactory: ((reader: ClassReader?, flags: Int) -> ClassWriter)? = null
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
            val reader = lazyNode.getReader() ?: return null

            return if (changed) writerFactory?.invoke(reader, writerFlags) ?: ClassWriter(reader, writerFlags) else null
        }

        fun compile(debugOut: (name: String, bytes: ByteArray) -> Unit): ByteArray? {
            val bytes = createWriter()?.let {
                node.accept(it)
                val bytes = it.toByteArray()
                debugOut(node.name, bytes)
                bytes
            }
            checkErrors()
            return bytes
        }
    }

    open class Wrapper(
        override val node: ClassNode
    ) : ClassTransformInfo {
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
}