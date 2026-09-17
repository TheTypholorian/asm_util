package net.typho.asm_util.remap

import net.typho.asm_util.KotlinUtil.kotlinMetadata
import net.typho.asm_util.KotlinUtil.visitKotlinMetadata
import net.typho.asm_util.remap.KotlinMetadataRemapper.Companion.kotlinMetadataRemapper
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import kotlin.metadata.jvm.KotlinClassMetadata

class CompatClassRemapper : ClassRemapper {
    companion object {
        @JvmField
        val MIXIN_ANNOTATIONS = mutableSetOf(
            "Lorg/spongepowered/asm/mixin/Mixin;"
        )
    }

    @JvmField
    val mixinTargets = mutableSetOf<Type>()
    @JvmField
    var remapMixins: Boolean

    constructor(classVisitor: ClassVisitor?, remapper: Remapper?) : this(classVisitor, remapper, true)

    constructor(api: Int, classVisitor: ClassVisitor?, remapper: Remapper?) : this(api, classVisitor, remapper, true)

    constructor(classVisitor: ClassVisitor?, remapper: Remapper?, remapMixins: Boolean) : super(classVisitor, remapper) {
        this.remapMixins = remapMixins
    }

    constructor(api: Int, classVisitor: ClassVisitor?, remapper: Remapper?, remapMixins: Boolean) : super(api, classVisitor, remapper) {
        this.remapMixins = remapMixins
    }

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        if (MIXIN_ANNOTATIONS.contains(descriptor)) {
            return object : AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
                override fun visit(name: String, value: Any?) {
                    if (name == "remap") {
                        remapMixins = value as Boolean
                    }

                    super.visit(name, value)
                }

                override fun visitArray(name: String): AnnotationVisitor? {
                    return when (name) {
                        "value", "targets" -> object : AnnotationVisitor(api, super.visitArray(name)) {
                            override fun visit(name: String?, value: Any?) {
                                super.visit(name, value)

                                if (value is String) {
                                    mixinTargets.add(remapper.mapValue(Type.getType(value)) as Type)
                                } else if (value is Type) {
                                    mixinTargets.add(remapper.mapValue(value) as Type)
                                }
                            }
                        }
                        else -> super.visitArray(name)
                    }
                }
            }
        } else if (descriptor == "Lkotlin/Metadata;") {
            val inner = super.visitAnnotation(descriptor, visible)

            return object : AnnotationNode(api, descriptor) {
                override fun visitEnd() {
                    super.visitEnd()

                    inner.visitKotlinMetadata(
                        remapper.kotlinMetadataRemapper.mapKtClassMetadata(
                            className,
                            KotlinClassMetadata.readLenient(kotlinMetadata!!)
                        ).write()
                    )
                    inner.visitEnd()
                }
            }
        }

        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        val visitor = super.visitField(access, name, descriptor, signature, value)
        visitor?.let { (it as CompatFieldRemapper).desc = descriptor }
        return visitor
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<String>?
    ): MethodVisitor? {
        val visitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        visitor?.let { (it as CompatMethodRemapper).desc = descriptor }
        return visitor
    }

    override fun createFieldRemapper(fieldVisitor: FieldVisitor?): CompatFieldRemapper {
        return CompatFieldRemapper(api, fieldVisitor, remapper, mixinTargets, remapMixins)
    }

    override fun createMethodRemapper(methodVisitor: MethodVisitor?): CompatMethodRemapper {
        return CompatMethodRemapper(api, methodVisitor, remapper, mixinTargets, remapMixins)
    }

    @Deprecated("Deprecated in Java")
    override fun createAnnotationRemapper(
        annotationVisitor: AnnotationVisitor
    ): CompatAnnotationRemapper {
        return CompatAnnotationRemapper(api, null, annotationVisitor, remapper, mixinTargets, remapMixins, null)
    }

    override fun createAnnotationRemapper(
        descriptor: String,
        annotationVisitor: AnnotationVisitor
    ): CompatAnnotationRemapper {
        return CompatAnnotationRemapper(api, descriptor, annotationVisitor, remapper, mixinTargets, remapMixins, null)
    }
}