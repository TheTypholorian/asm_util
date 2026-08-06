package net.typho.asm_util.remap

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper

class MixinMethodRemapper : MethodRemapper {
    @JvmField
    val mixinTargets: MutableSet<Type>
    lateinit var desc: String

    constructor(
        fieldVisitor: MethodVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>
    ) : super(fieldVisitor, remapper) {
        this.mixinTargets = mixinTargets
    }

    constructor(
        api: Int,
        fieldVisitor: MethodVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>
    ) : super(api, fieldVisitor, remapper) {
        this.mixinTargets = mixinTargets
    }

    @Deprecated("Deprecated in Java")
    override fun createAnnotationRemapper(parent: AnnotationVisitor): AnnotationVisitor {
        return MixinAnnotationRemapper(api, null, parent, remapper, mixinTargets, desc)
    }

    override fun createAnnotationRemapper(descriptor: String?, parent: AnnotationVisitor): AnnotationVisitor {
        return MixinAnnotationRemapper(api, descriptor, parent, remapper, mixinTargets, desc)
    }
}