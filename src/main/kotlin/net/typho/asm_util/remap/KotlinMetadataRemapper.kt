package net.typho.asm_util.remap

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper
import kotlin.contracts.ExperimentalContracts
import kotlin.metadata.*
import kotlin.metadata.jvm.*

// TODO so far this is only used for the metadata annotation, need to make a custom remapping visitor to apply it to the rest of the class node
@OptIn(ExperimentalAnnotationsInMetadata::class, ExperimentalContextReceivers::class, ExperimentalContracts::class)
interface KotlinMetadataRemapper {
    val remapper: Remapper
        get() = if (this is Remapper) this else throw ClassCastException("KotlinMetadataRemapper subclass $this does not override 'remapper' nor extend Remapper")

    fun mapKtFacadeClassName(name: String): String {
        return remapper.map(name)
    }

    fun mapKtCompanionObjectName(owner: ClassName, name: String): String {
        val owner = owner.toJvmInternalName()
        return remapper.mapInnerClassName("$owner$$name", owner, name)
    }

    fun mapKtNestedClassName(owner: ClassName, name: String): String {
        val owner = owner.toJvmInternalName()
        return remapper.mapInnerClassName("$owner$$name", owner, name)
    }

    fun mapKtEnumEntryName(owner: ClassName, name: String): String {
        val owner = owner.toJvmInternalName().toJvmType()
        return remapper.mapFieldName(owner.internalName, name, owner.descriptor)
    }

    fun mapKtValueClassPropertyName(owner: ClassName, name: String, type: KmType): String {
        return remapper.mapFieldName(owner.toJvmInternalName(), name, type.toJvmType().descriptor)
    }

    fun mapKtModuleName(name: String): String {
        return remapper.mapModuleName(name)
    }

    fun mapKtAnonymousObjectOriginName(owner: ClassName, name: String) = name

    fun mapKtFunctionName(owner: ClassName, func: KmFunction): String {
        return remapper.mapMethodName(owner.toJvmInternalName(), func.name, Type.getMethodDescriptor(func.returnType.toJvmType(), *func.valueParameters.map { it.type.toJvmType() }.toTypedArray()))
    }

    fun mapKtPropertyName(owner: ClassName, property: KmProperty): String {
        return remapper.mapFieldName(owner.toJvmInternalName(), property.name, property.returnType.toJvmType().descriptor)
    }

    fun mapKtTypeAliasName(name: String) = name

    fun mapKtValueParameterName(param: KmValueParameter) = param.name

    fun mapKtTypeParameterName(param: KmTypeParameter) = param.name

    fun mapKtClassifier(classifier: KmClassifier): KmClassifier {
        return when (classifier) {
            is KmClassifier.Class -> KmClassifier.Class(mapKtClassName(classifier.name))
            is KmClassifier.TypeAlias -> KmClassifier.TypeAlias(mapKtTypeAliasName(classifier.name))
            else -> classifier
        }
    }

    fun mapKtAnnotationArgumentName(owner: ClassName, name: String): String {
        return remapper.mapAnnotationAttributeName(owner.toJvmType().descriptor, name)
    }

    fun mapKtClassName(name: ClassName): ClassName {
        val jvm = remapper.map(name.toJvmInternalName())
        return if (name.isLocalClassName()) ".$jvm" else jvm.replace('$', '.')
    }

    fun mapKtMethodSignature(owner: ClassName, signature: JvmMethodSignature): JvmMethodSignature {
        return JvmMethodSignature(
            remapper.mapMethodName(owner.toJvmInternalName(), signature.name, signature.descriptor),
            remapper.mapMethodDesc(signature.descriptor)
        )
    }

    fun mapKtFieldSignature(owner: ClassName, signature: JvmFieldSignature): JvmFieldSignature {
        return JvmFieldSignature(
            remapper.mapFieldName(owner.toJvmInternalName(), signature.name, signature.descriptor),
            remapper.mapType(signature.descriptor)
        )
    }

    fun mapKtClassMetadata(className: ClassName, metadata: KotlinClassMetadata): KotlinClassMetadata {
        return when (metadata) {
            is KotlinClassMetadata.Class -> KotlinClassMetadata.Class(
                metadata.kmClass.also { mapKtClass(it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.FileFacade -> KotlinClassMetadata.FileFacade(
                metadata.kmPackage.also { mapKtPackage(className, it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.SyntheticClass -> KotlinClassMetadata.SyntheticClass(
                metadata.kmLambda?.also { mapKtLambda(className, it) },
                metadata.version,
                metadata.flags
            )
            is KotlinClassMetadata.MultiFileClassPart -> KotlinClassMetadata.MultiFileClassPart(
                metadata.kmPackage.also { mapKtPackage(className, it) },
                mapKtFacadeClassName(metadata.facadeClassName),
                metadata.version,
                metadata.flags
            )
            else -> metadata
        }
    }

    fun mapKtClass(cls: KmClass): KmClass {
        val new = KmClass()
        new.name = mapKtClassName(cls.name)
        cls.typeParameters.mapTo(new.typeParameters) { mapKtTypeParameter(it) }
        cls.supertypes.mapTo(new.supertypes) { mapKtType(it) }
        cls.functions.mapTo(new.functions) { mapKtFunction(cls.name, it) }
        cls.properties.mapTo(new.properties) { mapKtProperty(cls.name, it) }
        cls.typeAliases.mapTo(new.typeAliases) { mapKtTypeAlias(it) }
        cls.constructors.mapTo(new.constructors) { mapKtConstructor(cls.name, it) }
        new.companionObject = cls.companionObject?.let { mapKtCompanionObjectName(cls.name, it) }
        cls.nestedClasses.mapTo(new.nestedClasses) { mapKtNestedClassName(cls.name, it) }
        @Suppress("DEPRECATION")
        cls.enumEntries.mapTo(new.enumEntries) { mapKtEnumEntryName(cls.name, it) }
        cls.kmEnumEntries.mapTo(new.kmEnumEntries) { mapKtEnumEntry(cls.name, it) }
        cls.sealedSubclasses.mapTo(new.sealedSubclasses) { mapKtClassName(it) }

        if (cls.isValue) {
            val name = cls.inlineClassUnderlyingPropertyName!!
            val type = cls.inlineClassUnderlyingType!!
            new.inlineClassUnderlyingPropertyName = mapKtValueClassPropertyName(cls.name, name, type)
            new.inlineClassUnderlyingType = mapKtType(type)
        }

        cls.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        cls.contextReceiverTypes.mapTo(new.contextReceiverTypes) { mapKtType(it) }
        new.versionRequirements.addAll(cls.versionRequirements)

        new.hasAnnotationsInBytecode = cls.hasAnnotationsInBytecode
        new.hasMethodBodiesInInterface = cls.hasMethodBodiesInInterface
        new.isCompiledInCompatibilityMode = cls.isCompiledInCompatibilityMode
        cls.localDelegatedProperties.mapTo(new.localDelegatedProperties) { mapKtProperty(cls.name, it) }
        new.moduleName = cls.moduleName?.let { mapKtModuleName(it) }
        new.anonymousObjectOriginName = cls.anonymousObjectOriginName?.let { mapKtAnonymousObjectOriginName(cls.name, it) }

        return new
    }

    fun mapKtPackage(file: ClassName, pkg: KmPackage): KmPackage {
        val new = KmPackage()
        pkg.functions.mapTo(new.functions) { mapKtFunction(file, it) }
        pkg.properties.mapTo(new.properties) { mapKtProperty(file, it) }
        pkg.typeAliases.mapTo(new.typeAliases) { mapKtTypeAlias(it) }
        pkg.localDelegatedProperties.mapTo(new.localDelegatedProperties) { mapKtProperty(file, it) }
        new.moduleName = pkg.moduleName?.let { mapKtModuleName(it) }
        return new
    }

    fun mapKtLambda(owner: ClassName, lambda: KmLambda): KmLambda {
        val new = KmLambda()
        new.function = mapKtFunction(owner, lambda.function)
        return new
    }

    fun mapKtConstructor(owner: ClassName, constructor: KmConstructor): KmConstructor {
        val new = KmConstructor()
        constructor.valueParameters.mapTo(new.valueParameters) { mapKtValueParameter(it) }
        new.versionRequirements.addAll(constructor.versionRequirements)
        constructor.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        new.signature = constructor.signature?.let { mapKtMethodSignature(owner, it) }
        return new
    }

    fun mapKtFunction(owner: ClassName, func: KmFunction): KmFunction {
        val new = KmFunction(mapKtFunctionName(owner, func))
        func.typeParameters.mapTo(new.typeParameters) { mapKtTypeParameter(it) }
        new.receiverParameterType = func.receiverParameterType?.let { mapKtType(it) }
        func.extensionReceiverParameterAnnotations.mapTo(new.extensionReceiverParameterAnnotations) { mapKtAnnotation(it) }
        func.contextReceiverTypes.mapTo(new.contextReceiverTypes) { mapKtType(it) }
        func.valueParameters.mapTo(new.valueParameters) { mapKtValueParameter(it) }
        new.returnType = mapKtType(func.returnType)
        new.versionRequirements.addAll(func.versionRequirements)
        new.contract = func.contract
        func.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        return new
    }

    fun mapKtProperty(owner: ClassName, property: KmProperty): KmProperty {
        val new = KmProperty(mapKtPropertyName(owner, property))
        property.getter.annotations.mapTo(new.getter.annotations) { mapKtAnnotation(it) }
        new.setter = property.setter?.let { setter ->
            val new = KmPropertyAccessorAttributes()
            setter.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
            new
        }
        property.typeParameters.mapTo(new.typeParameters) { mapKtTypeParameter(it) }
        new.receiverParameterType = property.receiverParameterType?.let { mapKtType(it) }
        property.extensionReceiverParameterAnnotations.mapTo(new.extensionReceiverParameterAnnotations) { mapKtAnnotation(it) }
        property.contextReceiverTypes.mapTo(new.contextReceiverTypes) { mapKtType(it) }
        new.returnType = mapKtType(property.returnType)
        new.versionRequirements.addAll(property.versionRequirements)
        property.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        property.backingFieldAnnotations.mapTo(new.backingFieldAnnotations) { mapKtAnnotation(it) }
        property.delegateFieldAnnotations.mapTo(new.delegateFieldAnnotations) { mapKtAnnotation(it) }
        new.fieldSignature = property.fieldSignature?.let { mapKtFieldSignature(owner, it) }
        new.getterSignature = property.getterSignature?.let { mapKtMethodSignature(owner, it) }
        new.setterSignature = property.setterSignature?.let { mapKtMethodSignature(owner, it) }
        new.syntheticMethodForAnnotations = property.syntheticMethodForAnnotations?.let { mapKtMethodSignature(owner, it) }
        new.syntheticMethodForDelegate = property.syntheticMethodForDelegate?.let { mapKtMethodSignature(owner, it) }
        return new
    }

    fun mapKtTypeAlias(alias: KmTypeAlias): KmTypeAlias {
        val new = KmTypeAlias(mapKtTypeAliasName(alias.name))
        alias.typeParameters.mapTo(new.typeParameters) { mapKtTypeParameter(it) }
        new.underlyingType = mapKtType(alias.underlyingType)
        new.expandedType = mapKtType(alias.expandedType)
        alias.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        new.versionRequirements.addAll(alias.versionRequirements)
        return new
    }

    fun mapKtValueParameter(param: KmValueParameter): KmValueParameter {
        val new = KmValueParameter(mapKtValueParameterName(param))
        new.type = mapKtType(param.type)
        new.varargElementType = param.varargElementType?.let { mapKtType(it) }
        new.annotationParameterDefaultValue = param.annotationParameterDefaultValue?.let { mapKtAnnotationArgument(it) }
        param.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        return new
    }

    fun mapKtTypeParameter(typeParam: KmTypeParameter): KmTypeParameter {
        val new = KmTypeParameter(mapKtTypeParameterName(typeParam), typeParam.id, typeParam.variance)
        typeParam.upperBounds.mapTo(new.upperBounds) { mapKtType(it) }
        typeParam.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        return new
    }

    fun mapKtEnumEntry(owner: ClassName, entry: KmEnumEntry): KmEnumEntry {
        val new = KmEnumEntry(mapKtEnumEntryName(owner, entry.name))
        entry.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        return new
    }

    fun mapKtType(type: KmType): KmType {
        val new = KmType()
        new.classifier = mapKtClassifier(type.classifier)
        type.arguments.mapTo(new.arguments) { mapKtTypeProjection(it) }
        new.abbreviatedType = type.abbreviatedType?.let { mapKtType(it) }
        new.outerType = type.outerType?.let { mapKtType(it) }
        new.flexibleTypeUpperBound = type.flexibleTypeUpperBound?.let { mapKtFlexibleTypeUpperBound(it) }
        new.isRaw = type.isRaw
        type.annotations.mapTo(new.annotations) { mapKtAnnotation(it) }
        return new
    }

    fun mapKtTypeProjection(typeProjection: KmTypeProjection): KmTypeProjection {
        return KmTypeProjection(typeProjection.variance, typeProjection.type?.let { mapKtType(it) })
    }

    fun mapKtFlexibleTypeUpperBound(bound: KmFlexibleTypeUpperBound): KmFlexibleTypeUpperBound {
        return KmFlexibleTypeUpperBound(mapKtType(bound.type), bound.typeFlexibilityId)
    }

    fun mapKtAnnotation(annotation: KmAnnotation): KmAnnotation {
        return KmAnnotation(
            mapKtClassName(annotation.className),
            annotation.arguments
                .mapKeys { (key, value) -> mapKtAnnotationArgumentName(annotation.className, key) }
                .mapValues { (key, value) -> mapKtAnnotationArgument(value) }
        )
    }

    fun mapKtAnnotationArgument(arg: KmAnnotationArgument): KmAnnotationArgument {
        return when (arg) {
            is KmAnnotationArgument.EnumValue -> KmAnnotationArgument.EnumValue(mapKtClassName(arg.enumClassName), mapKtEnumEntryName(arg.enumClassName, arg.enumEntryName))
            is KmAnnotationArgument.AnnotationValue -> KmAnnotationArgument.AnnotationValue(mapKtAnnotation(arg.annotation))
            is KmAnnotationArgument.ArrayValue -> KmAnnotationArgument.ArrayValue(arg.elements.map { mapKtAnnotationArgument(it) })
            is KmAnnotationArgument.KClassValue -> KmAnnotationArgument.KClassValue(mapKtClassName(arg.className))
            is KmAnnotationArgument.ArrayKClassValue -> KmAnnotationArgument.ArrayKClassValue(mapKtClassName(arg.className), arg.arrayDimensionCount)
            else -> arg
        }
    }

    companion object {
        @JvmStatic
        val Remapper.kotlinMetadataRemapper: KotlinMetadataRemapper
            get() = if (this is KotlinMetadataRemapper) this else Fallback(this)

        @JvmStatic
        fun KmType.toJvmType(): Type {
            val type = classifier.name.toJvmType()

            if (type.internalName == "kotlin/Array") {
                val inner = arguments.single()
                val type = inner.type

                return if (type == null || inner.variance == KmVariance.IN) Type.getType("[Ljava/lang/Object;") else Type.getType("[${type.toJvmType()}")
            }

            return type
        }

        @JvmStatic
        fun ClassName.toJvmType(): Type {
            return when (val name = toJvmInternalName()) {
                "kotlin/Unit" -> Type.VOID_TYPE
                "kotlin/Boolean" -> Type.BOOLEAN_TYPE
                "kotlin/Char" -> Type.CHAR_TYPE
                "kotlin/Byte" -> Type.BYTE_TYPE
                "kotlin/Short" -> Type.SHORT_TYPE
                "kotlin/Int" -> Type.INT_TYPE
                "kotlin/Float" -> Type.FLOAT_TYPE
                "kotlin/Long" -> Type.LONG_TYPE
                "kotlin/Double" -> Type.DOUBLE_TYPE

                "kotlin/BooleanArray" -> Type.getType("[Z")
                "kotlin/CharArray" -> Type.getType("[C")
                "kotlin/ByteArray" -> Type.getType("[B")
                "kotlin/ShortArray" -> Type.getType("[S")
                "kotlin/IntArray" -> Type.getType("[I")
                "kotlin/FloatArray" -> Type.getType("[F")
                "kotlin/LongArray" -> Type.getType("[J")
                "kotlin/DoubleArray" -> Type.getType("[D")

                else -> Type.getObjectType(name)
            }
        }

        @JvmStatic
        val KmClassifier.name: ClassName
            get() = when (this) {
                is KmClassifier.Class -> name
                is KmClassifier.TypeAlias -> name
                else -> "java/lang/Object"
            }
    }

    data class Fallback(
        override val remapper: Remapper
    ) : KotlinMetadataRemapper
}