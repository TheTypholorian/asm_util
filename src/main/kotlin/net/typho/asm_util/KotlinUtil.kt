package net.typho.asm_util

import net.typho.asm_util.ASMUtil.forEach
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import kotlin.metadata.ClassName
import kotlin.metadata.ExperimentalAnnotationsInMetadata
import kotlin.metadata.ExperimentalContextReceivers
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmConstructor
import kotlin.metadata.KmEnumEntry
import kotlin.metadata.KmFlexibleTypeUpperBound
import kotlin.metadata.KmFunction
import kotlin.metadata.KmLambda
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.KmType
import kotlin.metadata.KmTypeAlias
import kotlin.metadata.KmTypeParameter
import kotlin.metadata.KmTypeProjection
import kotlin.metadata.KmValueParameter
import kotlin.metadata.isLocalClassName
import kotlin.metadata.jvm.JvmFieldSignature
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.metadata.jvm.KotlinClassMetadata
import kotlin.metadata.jvm.annotations
import kotlin.metadata.jvm.anonymousObjectOriginName
import kotlin.metadata.jvm.fieldSignature
import kotlin.metadata.jvm.getterSignature
import kotlin.metadata.jvm.lambdaClassOriginName
import kotlin.metadata.jvm.localDelegatedProperties
import kotlin.metadata.jvm.setterSignature
import kotlin.metadata.jvm.signature
import kotlin.metadata.jvm.syntheticMethodForAnnotations
import kotlin.metadata.jvm.syntheticMethodForDelegate

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

            forEach { name, value ->
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
    fun Remapper.mapKotlinClass(cls: KmClass) {
        val oldName = cls.name.replace('.', '$')
        cls.name = mapKotlinClassName(cls.name)
        cls.typeParameters.forEach { mapKotlinTypeParameter(it) }
        cls.supertypes.forEach { mapKotlinType(it) }
        cls.functions.forEach { mapKotlinFunction(oldName, it) }
        cls.properties.forEach { mapKotlinProperty(oldName, it) }
        cls.typeAliases.forEach { mapKotlinTypeAlias(it) }
        cls.constructors.forEach { mapKotlinConstructor(it) }
        cls.companionObject = cls.companionObject?.let { mapInnerClassName("$oldName$$it", oldName, it) }
        cls.nestedClasses.forEach { mapInnerClassName("$oldName$$it", oldName, it) }
        cls.enumEntries.replaceAll { mapFieldName(oldName, it, "L$oldName;") }
        cls.kmEnumEntries.forEach { mapKotlinEnumEntry(oldName, it) }
        cls.sealedSubclasses.forEach { mapInnerClassName("$oldName$$it", oldName, it) }
        cls.inlineClassUnderlyingType?.let { mapKotlinType(it) }
        cls.annotations.replaceAll { mapKotlinAnnotation(it) }
        cls.contextReceiverTypes.forEach { mapKotlinType(it) }
        cls.localDelegatedProperties.forEach { mapKotlinProperty(oldName, it) }
        cls.anonymousObjectOriginName = cls.anonymousObjectOriginName?.let { map(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinPackage(owner: String, pkg: KmPackage) {
        pkg.functions.forEach { mapKotlinFunction(owner, it) }
        pkg.properties.forEach { mapKotlinProperty(owner, it) }
        pkg.typeAliases.forEach { mapKotlinTypeAlias(it) }
        pkg.localDelegatedProperties.forEach { mapKotlinProperty(owner, it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinLambda(owner: String, lambda: KmLambda) {
        mapKotlinFunction(owner, lambda.function)
    }

    @JvmStatic
    fun Remapper.mapKotlinConstructor(constructor: KmConstructor) {
        constructor.valueParameters.forEach { mapKotlinValueParameter(it) }
        constructor.annotations.replaceAll { mapKotlinAnnotation(it) }
        constructor.signature = constructor.signature?.let { mapKotlinMethodSignature(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinFunction(owner: String, func: KmFunction) {
        func.name = mapMethodName(owner, func.name, func.signature?.descriptor ?: "()V")
        func.typeParameters.forEach { mapKotlinTypeParameter(it) }
        func.receiverParameterType?.let { mapKotlinType(it) }
        func.extensionReceiverParameterAnnotations.replaceAll { mapKotlinAnnotation(it) }
        func.contextReceiverTypes.forEach { mapKotlinType(it) }
        func.valueParameters.forEach { mapKotlinValueParameter(it) }
        mapKotlinType(func.returnType)
        func.annotations.forEach { mapKotlinAnnotation(it) }
        func.signature = func.signature?.let { mapKotlinMethodSignature(it) }
        func.lambdaClassOriginName = func.lambdaClassOriginName?.let { map(it) }
    }

    @JvmStatic
    fun Remapper.mapKotlinProperty(owner: String, property: KmProperty) {
        property.fieldSignature?.let {
            property.name = mapFieldName(owner, property.name, it.descriptor)
        } ?: property.getterSignature?.let {
            property.name = mapMethodName(owner, it.name, it.descriptor)
        } ?: property.setterSignature?.let {
            property.name = mapMethodName(owner, it.name, it.descriptor)
        }
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
        val remapped = map(name.replace('.', '$')).replace('$', '.')
        return if (name.isLocalClassName()) ".$remapped" else remapped
    }

    @JvmStatic
    fun Remapper.mapKotlinMethodSignature(signature: JvmMethodSignature) = JvmMethodSignature(signature.name, mapMethodDesc(signature.descriptor))

    @JvmStatic
    fun Remapper.mapKotlinFieldSignature(signature: JvmFieldSignature) = JvmFieldSignature(signature.name, mapDesc(signature.descriptor))
}