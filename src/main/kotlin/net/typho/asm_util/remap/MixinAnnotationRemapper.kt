package net.typho.asm_util.remap

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Type
import org.objectweb.asm.commons.AnnotationRemapper
import org.objectweb.asm.commons.Remapper

open class MixinAnnotationRemapper : AnnotationRemapper {
    companion object {
        @JvmField
        val ACCESSOR_TYPES = mutableMapOf<String?, (remapper: Remapper, owner: String, name: String, targetDescriptor: String) -> String>(
            "Lorg/spongepowered/asm/mixin/gen/Accessor;" to { remapper, owner, name, targetDescriptor ->
                remapper.mapFieldName(owner, name, Type.getMethodType(targetDescriptor).returnType.descriptor)
            },
            "Lorg/spongepowered/asm/mixin/gen/Invoker;" to { remapper, owner, name, targetDescriptor ->
                remapper.mapMethodName(owner, name, targetDescriptor)
            }
        )
    }

    @JvmField
    val mixinTargets: MutableSet<Type>
    /**
     * Null for classes, not null for fields and methods
     */
    @JvmField
    var targetDescriptor: String? = null

    constructor(
        descriptor: String?,
        annotationVisitor: AnnotationVisitor,
        remapper: Remapper,
        mixinTargets: MutableSet<Type>,
        targetDescriptor: String?
    ) : super(descriptor, annotationVisitor, remapper) {
        this.mixinTargets = mixinTargets
        this.targetDescriptor = targetDescriptor
    }

    constructor(
        api: Int,
        descriptor: String?,
        annotationVisitor: AnnotationVisitor,
        remapper: Remapper,
        mixinTargets: MutableSet<Type>,
        targetDescriptor: String?
    ) : super(api, descriptor, annotationVisitor, remapper) {
        this.mixinTargets = mixinTargets
        this.targetDescriptor = targetDescriptor
    }

    override fun visit(name: String?, value: Any?) {
        var value = value

        if (value is String) {
            if (targetDescriptor == null) {
                if (value.startsWith('L') && value.endsWith(';')) {
                    value = remapper.mapValue(Type.getType(value))
                }
            } else if (mixinTargets.isNotEmpty()) {
                if (ACCESSOR_TYPES.contains(descriptor)) {
                    val owner = mixinTargets.first().internalName
                    value = ACCESSOR_TYPES[descriptor]!!.invoke(remapper, owner, value, targetDescriptor!!)
                } else if (value.contains("(")) { // method
                    val index = value.indexOf('(')
                    var methodName = value.substring(0, index)
                    val methodDesc = value.substring(index)

                    if (methodName.isEmpty()) { // (LArgumentClass;)
                        value = remapper.mapMethodDesc(methodDesc)
                    } else if (methodName.contains(';')) { // LOwnerClass;methodName(LArgumentClass;)
                        val index1 = methodName.indexOf(';') + 1

                        val methodOwner = Type.getType(methodName.substring(0, index1))
                        methodName = methodName.substring(index1)

                        value = remapper.mapValue(methodOwner).toString() + remapper.mapMethodName(methodOwner.internalName, methodName, methodDesc) + remapper.mapMethodDesc(methodDesc)
                    } else { // methodName(LArgumentClass;)
                        val owner = mixinTargets.first().internalName
                        value = remapper.mapMethodName(owner, methodName, methodDesc) + remapper.mapMethodDesc(methodDesc)
                    }
                } else if (value.contains(':')) { // field
                    val index = value.indexOf(':')
                    val fieldName = value.substring(0, index)
                    val fieldDesc = value.substring(index + 1)

                    if (fieldName.contains(';') && fieldName.indexOf(';') < index) { // LOwnerClass;fieldName:FieldType
                        val index1 = fieldName.indexOf(';')
                        val owner = Type.getType(fieldName.substring(0, index1 + 1))
                        value = remapper.mapValue(owner).toString() + remapper.mapFieldName(owner.internalName, fieldName.substring(index1 + 1), fieldDesc) + ":" + remapper.mapValue(Type.getType(fieldDesc))
                    } else { // fieldName:FieldType
                        val owner = mixinTargets.first().internalName
                        value = remapper.mapFieldName(owner, fieldName, fieldDesc) + ":" + remapper.map(fieldDesc)
                    }
                }
            }
        }

        super.visit(name, value)
    }

    @Deprecated("Deprecated in Java")
    override fun createAnnotationRemapper(parent: AnnotationVisitor): AnnotationVisitor {
        return MixinAnnotationRemapper(api, null, parent, remapper, mixinTargets, targetDescriptor)
    }

    override fun createAnnotationRemapper(descriptor: String?, parent: AnnotationVisitor): AnnotationVisitor {
        return MixinAnnotationRemapper(api, descriptor, parent, remapper, mixinTargets, targetDescriptor)
    }
}