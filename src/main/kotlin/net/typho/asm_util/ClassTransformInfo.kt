package net.typho.asm_util

import net.typho.asm_util.error.ClassVisitException
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

open class ClassTransformInfo {
    @JvmField
    val node: ClassNode
    @JvmField
    val reader: ClassReader
    @JvmField
    protected var changed = false
    @JvmField
    protected var writerFlags = 0
    @JvmField
    protected var writerFactory: ((node: ClassNode, reader: ClassReader, flags: Int) -> ClassWriter)? = null
    @JvmField
    protected val errors = mutableListOf<String>()

    constructor(bytes: ByteArray) : this(ClassNode(), ClassReader(bytes)) {
        reader.accept(node, 0)
    }

    constructor(node: ClassNode, reader: ClassReader) {
        this.node = node
        this.reader = reader
    }

    open fun markChanged() {
        changed = true
    }

    open fun writerFactory(factory: (node: ClassNode, reader: ClassReader, flags: Int) -> ClassWriter) {
        if (this.writerFactory != null) {
            throw NullPointerException("Cannot set ClassOutputInfo factory more than once")
        }

        this.writerFactory = factory
    }

    open fun computeMaxStacks() {
        writerFlags = writerFlags or ClassWriter.COMPUTE_MAXS
    }

    open fun computeFrames() {
        writerFlags = writerFlags or ClassWriter.COMPUTE_FRAMES
    }

    open fun error(error: String) {
        if (!errors.contains(error)) {
            errors.add(error)
        }
    }

    open fun end(): ClassWriter? {
        if (!errors.isEmpty()) {
            throw ClassVisitException((if (errors.size == 1) "Error" else "Errors") + " while transforming class ${node.name}:\n${errors.joinToString(separator = "\n")}")
        }

        return if (changed) writerFactory?.invoke(node, reader, writerFlags) ?: ClassWriter(reader, writerFlags) else null
    }
}