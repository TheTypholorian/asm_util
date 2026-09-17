package net.typho.asm_util

import net.typho.asm_util.ASMUtil.iterator
import net.typho.asm_util.remap.KotlinRemapper
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import kotlin.metadata.*
import kotlin.metadata.jvm.*

/**
 * All methods here that return a new value assume that the input will be discarded.
 *
 * For example, the [mapKotlinTypeProjection] method does not mutate the `variance` field of the input, but does mutate the `type` field.
 */
@OptIn(ExperimentalContextReceivers::class, ExperimentalAnnotationsInMetadata::class)
object KotlinUtil {
    @JvmStatic
    val ClassNode.kotlinMetadata: Metadata?
        get() = visibleAnnotations?.firstNotNullOfOrNull { it.kotlinMetadata }

    @Suppress("UNCHECKED_CAST")
    @get:JvmName("readKotlinMetadata")
    @JvmStatic
    val AnnotationNode.kotlinMetadata: Metadata?
        get() {
            if (desc != "Lkotlin/Metadata;") {
                return null
            }

            var kind = 1
            var metadataVersion = intArrayOf()
            var bytecodeVersion = intArrayOf(1, 0, 3)
            var data1 = arrayOf<String>()
            var data2 = arrayOf<String>()
            var extraString = ""
            var packageName = ""
            var extraInt = 0

            iterator().forEach { (name, value) ->
                when (name) {
                    "k" -> kind = value as Int
                    "mv" -> metadataVersion = (value as List<Int>).toIntArray()
                    "bv" -> bytecodeVersion = (value as List<Int>).toIntArray()
                    "d1" -> data1 = (value as List<String>).toTypedArray()
                    "d2" -> data2 = (value as List<String>).toTypedArray()
                    "xs" -> extraString = value as String
                    "pn" -> packageName = value as String
                    "xi" -> extraInt = value as Int
                }
            }

            return Metadata(kind, metadataVersion, bytecodeVersion, data1, data2, extraString, packageName, extraInt)
        }

    @JvmStatic
    fun AnnotationVisitor.visitKotlinMetadata(metadata: Metadata) {
        visit("k", metadata.kind)
        visitArray("mv").apply { metadata.metadataVersion.forEach { visit(null, it) } }
        visitArray("bv").apply { metadata.bytecodeVersion.forEach { visit(null, it) } }
        visitArray("d1").apply { metadata.data1.forEach { visit(null, it) } }
        visitArray("d2").apply { metadata.data2.forEach { visit(null, it) } }
        visit("xs", metadata.extraString)
        visit("pn", metadata.packageName)
        visit("xi", metadata.extraInt)
    }

    @JvmStatic
    fun Remapper.mapKotlinClassMetadata(className: String, metadata: KotlinClassMetadata): KotlinClassMetadata {
        return when (metadata) {
            is KotlinClassMetadata.Class -> KotlinClassMetadata.Class(
                metadata.kmClass.also { mapKotlinClass(it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.FileFacade -> KotlinClassMetadata.FileFacade(
                metadata.kmPackage.also { mapKotlinPackage(className, it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.SyntheticClass -> KotlinClassMetadata.SyntheticClass(
                metadata.kmLambda?.also { mapKotlinLambda(className, it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.MultiFileClassPart -> KotlinClassMetadata.MultiFileClassPart(
                metadata.kmPackage.also { mapKotlinPackage(className, it) },
                map(metadata.facadeClassName),
                metadata.version,
                metadata.flags
            )
            else -> metadata
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun Remapper.mapKotlinClass(cls: KmClass): KmClass {
        val new = KmClass()

        val oldName = cls.name.replace('.', '$')
        new.name = mapKotlinClassName(cls.name)
        new.typeParameters = cls.typeParameters.map { mapKotlinTypeParameter(it) }
        new.supertypes = cls.supertypes.map { mapKotlinType(it) }
        new.functions = cls.functions.map { mapKotlinFunction(oldName, it) }
        new.properties = cls.properties.map { mapKotlinProperty(oldName, it) }
        new.typeAliases = cls.typeAliases.map { mapKotlinTypeAlias(it) }
        new.constructors = cls.constructors.map { mapKotlinConstructor(it) }
        new.companionObject = cls.companionObject?.let { mapInnerClassName("$oldName$$it", oldName, it) }
        new.nestedClasses = cls.nestedClasses.map { mapInnerClassName("$oldName$$it", oldName, it) }
        new.enumEntries = cls.enumEntries.map { mapFieldName(oldName, it, "L$oldName;") }
        new.kmEnumEntries = cls.kmEnumEntries.map { mapKotlinEnumEntry(oldName, it) }
        new.sealedSubclasses = cls.sealedSubclasses.map { mapInnerClassName("$oldName$$it", oldName, it) }

        if (cls.isValue) {
            val name = cls.inlineClassUnderlyingPropertyName!!
            val type = cls.inlineClassUnderlyingType!!

            new.inlineClassUnderlyingPropertyName = if (this is KotlinRemapper) mapKotlinPropertyName(oldName, name, type) else name
            new.inlineClassUnderlyingType = cls.inlineClassUnderlyingType?.let { mapKotlinType(it) }
        }

        new.annotations.replaceAll { mapKotlinAnnotation(it) }
        new.contextReceiverTypes = cls.contextReceiverTypes.map { mapKotlinType(it) }
        new.localDelegatedProperties = cls.localDelegatedProperties.map { mapKotlinProperty(oldName, it) }
        new.anonymousObjectOriginName = cls.anonymousObjectOriginName?.let { map(it) }

        return new
    }

    @JvmStatic
    fun Remapper.mapKotlinPackage(owner: String, pkg: KmPackage): KmPackage {
        val new = KmPackage()
        new.functions = pkg.functions.map { mapKotlinFunction(owner, it) }
        new.properties = pkg.properties.map { mapKotlinProperty(owner, it) }
        new.typeAliases = pkg.typeAliases.map { mapKotlinTypeAlias(it) }
        new.localDelegatedProperties = pkg.localDelegatedProperties.map { mapKotlinProperty(owner, it) }
        return new
    }

    @JvmStatic
    fun Remapper.mapKotlinLambda(owner: String, lambda: KmLambda): KmLambda {
        val new = KmLambda()
        new.function = mapKotlinFunction(owner, lambda.function)
        return new
    }

    @JvmStatic
    fun Remapper.mapKotlinConstructor(constructor: KmConstructor): KmConstructor {
        val new = KmConstructor()
        new.valueParameters = constructor.valueParameters.map { mapKotlinValueParameter(it) }
        new.annotations = constructor.annotations.map { mapKotlinAnnotation(it) }
        new.signature = constructor.signature?.let { mapKotlinMethodSignature(it) }
        return new
    }

    @JvmStatic
    fun Remapper.mapKotlinFunction(owner: String, func: KmFunction): KmFunction {
        val new = KmFunction(mapMethodName(owner, func.name, func.signature?.descriptor ?: "()V"))
        new.typeParameters = func.typeParameters.map { mapKotlinTypeParameter(it) }
        new.receiverParameterType = func.receiverParameterType?.let { mapKotlinType(it) }
        new.extensionReceiverParameterAnnotations = func.extensionReceiverParameterAnnotations.map { mapKotlinAnnotation(it) }
        new.contextReceiverTypes = func.contextReceiverTypes.map { mapKotlinType(it) }
        new.valueParameters = func.valueParameters.map { mapKotlinValueParameter(it) }
        new.returnType = mapKotlinType(func.returnType)
        new.annotations = func.annotations.map { mapKotlinAnnotation(it) }
        new.signature = func.signature?.let { mapKotlinMethodSignature(it) }
        new.lambdaClassOriginName = func.lambdaClassOriginName?.let { map(it) }
        return new
    }

    @JvmStatic
    fun Remapper.mapKotlinProperty(owner: String, property: KmProperty): KmProperty {
        val new = KmProperty(if (this is KotlinRemapper) mapKotlinPropertyName(owner, property.name, property.returnType) else property.fieldSignature?.let { mapFieldName(owner, property.name, it.descriptor) } ?: property.name)
        property.getter.annotations.replaceAll { mapKotlinAnnotation(it) }
        property.setter?.annotations?.replaceAll { mapKotlinAnnotation(it) }
        property.typeParameters.forEach { mapKotlinTypeParameter(it) }
        property.receiverParameterType?.let { mapKotlinType(it) }
        property.extensionReceiverParameterAnnotations.replaceAll { mapKotlinAnnotation(it) }
        property.contextReceiverTypes.forEach { mapKotlinType(it) }
        property.setterParameter?.let { mapKotlinValueParameter(it) }
        mapKotlinType(property.returnType)
        property.annotations.replaceAll { mapKotlinAnnotation(it) }
        property.backingFieldAnnotations.replaceAll { mapKotlinAnnotation(it) }
        property.delegateFieldAnnotations.replaceAll { mapKotlinAnnotation(it) }
        property.fieldSignature = property.fieldSignature?.let { mapKotlinFieldSignature(it) }
        property.getterSignature = property.getterSignature?.let { mapKotlinMethodSignature(it) }
        property.setterSignature = property.setterSignature?.let { mapKotlinMethodSignature(it) }
        property.syntheticMethodForAnnotations = property.syntheticMethodForAnnotations?.let { mapKotlinMethodSignature(it) }
        property.syntheticMethodForDelegate = property.syntheticMethodForDelegate?.let { mapKotlinMethodSignature(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinTypeAlias(alias: KmTypeAlias) {
        alias.typeParameters.forEach { mapKotlinTypeParameter(it) }
        mapKotlinType(alias.underlyingType)
        mapKotlinType(alias.expandedType)
        alias.annotations.replaceAll { mapKotlinAnnotation(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinValueParameter(param: KmValueParameter) {
        mapKotlinType(param.type)
        param.varargElementType?.let { mapKotlinType(it) }
        param.annotationParameterDefaultValue = param.annotationParameterDefaultValue?.let { mapKotlinAnnotationArgument(it) }
        param.annotations.replaceAll { mapKotlinAnnotation(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinTypeParameter(typeParam: KmTypeParameter) {
        typeParam.name = mapKotlinClassName(typeParam.name)
        typeParam.upperBounds.forEach { mapKotlinType(it) }
        typeParam.annotations.replaceAll { mapKotlinAnnotation(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinEnumEntry(owner: String, entry: KmEnumEntry) {
        entry.name = mapFieldName(owner, entry.name, "L$owner;")
        entry.annotations.replaceAll { mapKotlinAnnotation(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinType(type: KmType) {
        type.classifier = when (val classifier = type.classifier) {
            is KmClassifier.Class -> KmClassifier.Class(mapKotlinClassName(classifier.name))
            is KmClassifier.TypeAlias -> KmClassifier.TypeAlias(mapKotlinClassName(classifier.name))
            else -> classifier
        }
        type.arguments.replaceAll { mapKotlinTypeProjection(it) }
        type.abbreviatedType?.let { mapKotlinType(it) }
        type.outerType?.let { mapKotlinType(it) }
        type.flexibleTypeUpperBound = type.flexibleTypeUpperBound?.let { mapKotlinFlexibleTypeUpperBound(it) }
        type.annotations.replaceAll { mapKotlinAnnotation(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinTypeProjection(typeProjection: KmTypeProjection) = KmTypeProjection(typeProjection.variance, typeProjection.type?.also { mapKotlinType(it) })

    @JvmStatic
    fun Remapper.mapKotlinFlexibleTypeUpperBound(bound: KmFlexibleTypeUpperBound) = KmFlexibleTypeUpperBound(bound.type.also { mapKotlinType(it) }, bound.typeFlexibilityId)

    @JvmStatic
    fun Remapper.mapKotlinAnnotation(annotation: KmAnnotation) = KmAnnotation(
        mapKotlinClassName(annotation.className),
        annotation.arguments
            .mapKeys { (name, value) ->
                mapAnnotationAttributeName("L${annotation.className.replace('.', '$')};", name)
            }
            .mapValues { (name, value) ->
                mapKotlinAnnotationArgument(value)
            }
    )

    @JvmStatic
    fun Remapper.mapKotlinAnnotationArgument(arg: KmAnnotationArgument): KmAnnotationArgument {
        return when (arg) {
            is KmAnnotationArgument.EnumValue -> {
                val owner = arg.enumClassName.replace('.', '$')
                KmAnnotationArgument.EnumValue(mapKotlinClassName(arg.enumClassName), mapFieldName(owner, arg.enumEntryName, "L$owner;"))
            }
            is KmAnnotationArgument.AnnotationValue -> KmAnnotationArgument.AnnotationValue(mapKotlinAnnotation(arg.annotation))
            is KmAnnotationArgument.ArrayValue -> KmAnnotationArgument.ArrayValue(arg.elements.map { mapKotlinAnnotationArgument(it) })
            is KmAnnotationArgument.KClassValue -> KmAnnotationArgument.KClassValue(mapKotlinClassName(arg.className))
            is KmAnnotationArgument.ArrayKClassValue -> KmAnnotationArgument.ArrayKClassValue(mapKotlinClassName(arg.className), arg.arrayDimensionCount)
            else -> arg
        }
    }

    @JvmStatic
    fun Remapper.mapKotlinClassName(name: ClassName): ClassName {
        if (this is KotlinRemapper) {
            return mapKotlinClassName(name)
        }

        val remapped = map(name.replace('.', '$')).replace('$', '.')
        return if (name.isLocalClassName()) ".$remapped" else remapped
    }

    @JvmStatic
    fun Remapper.mapKotlinMethodSignature(signature: JvmMethodSignature) = JvmMethodSignature(signature.name, mapMethodDesc(signature.descriptor))

    @JvmStatic
    fun Remapper.mapKotlinFieldSignature(signature: JvmFieldSignature) = JvmFieldSignature(signature.name, mapDesc(signature.descriptor))
}