package net.typho.asm_util

import net.typho.asm_util.ASMUtil.mapIterator
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import kotlin.metadata.*

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

            mapIterator().forEach { (name, value) ->
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
}