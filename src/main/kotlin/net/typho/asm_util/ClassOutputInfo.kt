package net.typho.asm_util

import net.typho.asm_util.error.ClassVisitException
import org.objectweb.asm.ClassWriter

open class ClassOutputInfo {
    @JvmField
    var className: String? = null

    protected var changed = false
    protected var writerFlags = 0
    protected val errors = mutableListOf<String>()

    constructor()

    constructor(className: String) {
        this.className = className
    }

    open fun markChanged() {
        changed = true
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
            throw ClassVisitException((if (errors.size == 1) "Error" else "Errors") + " while transforming class $className:\n${errors.joinToString(separator = "\n")}")
        }

        return if (changed) ClassWriter(writerFlags) else null
    }
}