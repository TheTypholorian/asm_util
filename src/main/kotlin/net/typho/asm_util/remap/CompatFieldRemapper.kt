package net.typho.asm_util.remap

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.FieldRemapper
import org.objectweb.asm.commons.Remapper

open class CompatFieldRemapper : FieldRemapper {
    @JvmField
    val mixinTargets: MutableSet<Type>
    @JvmField
    val remapMixins: Boolean
    lateinit var desc: String

    constructor(
        fieldVisitor: FieldVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>,
        remapMixins: Boolean
    ) : super(fieldVisitor, remapper) {
        this.mixinTargets = mixinTargets
        this.remapMixins = remapMixins
    }

    constructor(
        api: Int,
        fieldVisitor: FieldVisitor?,
        remapper: Remapper?,
        mixinTargets: MutableSet<Type>,
        remapMixins: Boolean
    ) : super(api, fieldVisitor, remapper) {
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