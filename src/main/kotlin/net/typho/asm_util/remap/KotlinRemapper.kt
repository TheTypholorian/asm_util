package net.typho.asm_util.remap

import org.objectweb.asm.commons.Remapper
import kotlin.metadata.ClassName
import kotlin.metadata.KmType

interface KotlinRemapper {
    fun mapKotlinClassName(name: ClassName): ClassName {
        return name
    }

    fun mapKotlinPropertyName(owner: String, name: String, type: KmType): String {
        return name
    }

    data class Default(
        @JvmField
        val remapper: Remapper
    ) : KotlinRemapper {
    }
}