package net.typho.asm_util.remap

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper

open class CompatMethodRemapper : MethodRemapper {
    @JvmField
    val mixinTargets: MutableSet<Type>
    @JvmField
    val remapMixins: Boolean
    lateinit var desc: String

    constructor(
        methodVisitor: MethodVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>,
        remapMixins: Boolean
    ) : super(methodVisitor, remapper) {
        this.mixinTargets = mixinTargets
        this.remapMixins = remapMixins
    }

    constructor(
        api: Int,
        methodVisitor: MethodVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>,
        remapMixins: Boolean
    ) : super(api, methodVisitor, remapper) {
        this.mixinTargets = mixinTargets
        this.remapMixins = remapMixins
    }


    @Deprecated("Deprecated in Java")
    override fun createAnnotationRemapper(parent: AnnotationVisitor): AnnotationVisitor {
        return CompatAnnotationRemapper(api, null, parent, remapper, mixinTargets, remapMixins, desc)
    }

    override fun createAnnotationRemapper(descriptor: String?, parent: AnnotationVisitor): AnnotationVisitor {
        return CompatAnnotationRemapper(api, descriptor, parent, remapper, mixinTargets, remapMixins, desc)
    }
}