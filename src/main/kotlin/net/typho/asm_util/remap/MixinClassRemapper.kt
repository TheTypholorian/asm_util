package net.typho.asm_util.remap

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

class MixinClassRemapper : ClassRemapper {
    companion object {
        @JvmField
        val MIXIN_ANNOTATIONS = mutableSetOf(
            "Lorg/spongepowered/asm/mixin/Mixin;"
        )
    }

    @JvmField
    val mixinTargets = mutableSetOf<Type>()

    constructor(classVisitor: ClassVisitor, remapper: Remapper) : super(classVisitor, remapper)

    constructor(api: Int, classVisitor: ClassVisitor, remapper: Remapper) : super(api, classVisitor, remapper)

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor {
        if (MIXIN_ANNOTATIONS.contains(descriptor)) {
            return object : AnnotationVisitor(api, super.visitAnnotation(descriptor, visible)) {
                override fun visitArray(name: String): AnnotationVisitor? {
                    return when (name) {
                        "value", "targets" -> object : AnnotationVisitor(api, super.visitArray(name)) {
                            override fun visit(name: String, value: Any?) {
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
        visitor?.let { (it as MixinFieldRemapper).desc = descriptor }
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
        visitor?.let { (it as MixinMethodRemapper).desc = descriptor }
        return visitor
    }

    override fun createFieldRemapper(fieldVisitor: FieldVisitor?): MixinFieldRemapper {
        return MixinFieldRemapper(api, fieldVisitor, remapper, mixinTargets)
    }

    override fun createMethodRemapper(methodVisitor: MethodVisitor?): MixinMethodRemapper {
        return MixinMethodRemapper(api, methodVisitor, remapper, mixinTargets)
    }

    @Deprecated("Deprecated in Java")
    override fun createAnnotationRemapper(
        annotationVisitor: AnnotationVisitor
    ): MixinAnnotationRemapper {
        return MixinAnnotationRemapper(api, null, annotationVisitor, remapper, mixinTargets, null)
    }

    override fun createAnnotationRemapper(
        descriptor: String,
        annotationVisitor: AnnotationVisitor
    ): MixinAnnotationRemapper {
        return MixinAnnotationRemapper(api, descriptor, annotationVisitor, remapper, mixinTargets, null)
    }
}