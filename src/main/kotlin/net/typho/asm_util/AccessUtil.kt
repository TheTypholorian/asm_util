package net.typho.asm_util

import org.objectweb.asm.Opcodes.*

object AccessUtil {
    fun Int.toAccessString(): String {
        return buildList {
            if (isPublic()) add("public")
            if (isPrivate()) add("private")
            if (isProtected()) add("protected")
            if (isStatic()) add("static")
            if (isFinal()) add("final")
            if (isSuper()) add("super")
            if (isSynchronized()) add("synchronized")
            if (isOpenModule()) add("open")
            if (isTransitiveModule()) add("transitive")
            if (isVolatile()) add("volatile")
            if (isBridge()) add("bridge")
            if (isStaticPhaseModule()) add("static_phase")
            if (isVarargs()) add("varargs")
            if (isTransient()) add("transient")
            if (isNative()) add("native")
            if (isInterface()) add("interface")
            if (isAbstract()) add("abstract")
            if (isStrict()) add("strict")
            if (isSynthetic()) add("synthetic")
            if (isAnnotation()) add("annotation")
            if (isEnum()) add("enum")
            if (isMandated()) add("mandated")
            if (isModule()) add("module")
        }.joinToString(separator = " ")
    }

    fun Int.isPublic() = (this and ACC_PUBLIC) != 0

    fun Int.isPrivate() = (this and ACC_PRIVATE) != 0

    fun Int.isProtected() = (this and ACC_PROTECTED) != 0

    fun Int.isStatic() = (this and ACC_STATIC) != 0

    fun Int.isFinal() = (this and ACC_FINAL) != 0

    fun Int.isSuper() = (this and ACC_SUPER) != 0

    fun Int.isSynchronized() = (this and ACC_SYNCHRONIZED) != 0

    fun Int.isOpenModule() = (this and ACC_OPEN) != 0

    fun Int.isTransitiveModule() = (this and ACC_TRANSITIVE) != 0

    fun Int.isVolatile() = (this and ACC_VOLATILE) != 0

    fun Int.isBridge() = (this and ACC_BRIDGE) != 0

    fun Int.isStaticPhaseModule() = (this and ACC_STATIC_PHASE) != 0

    fun Int.isVarargs() = (this and ACC_VARARGS) != 0

    fun Int.isTransient() = (this and ACC_TRANSIENT) != 0

    fun Int.isNative() = (this and ACC_NATIVE) != 0

    fun Int.isInterface() = (this and ACC_INTERFACE) != 0

    fun Int.isAbstract() = (this and ACC_ABSTRACT) != 0

    fun Int.isStrict() = (this and ACC_STRICT) != 0

    fun Int.isSynthetic() = (this and ACC_SYNTHETIC) != 0

    fun Int.isAnnotation() = (this and ACC_ANNOTATION) != 0

    fun Int.isEnum() = (this and ACC_ENUM) != 0

    fun Int.isMandated() = (this and ACC_MANDATED) != 0

    fun Int.isModule() = (this and ACC_MODULE) != 0
}