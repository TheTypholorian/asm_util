package net.typho.asm_util.field

import net.typho.asm_util.ASMPointer
import net.typho.asm_util.AccessUtil.toAccessString
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.util.function.BiPredicate

class FieldPointer private constructor() : ASMPointer<FieldNode, ClassNode, FieldPointer>() {
    private var name: String? = null
    private var desc: String? = null
    private var signature: String? = null
    private val modifiers = Array<Boolean?>(32) { null }

    init {
        predicate = BiPredicate { self, field ->
            if (name != null && field.name != name) {
                if (self.debug) {
                    println("\t\tExpected name $name but got ${field.name}")
                }

                return@BiPredicate false
            }

            if (desc != null && field.desc != desc) {
                if (self.debug) {
                    println("\t\tExpected desc $desc but got ${field.desc}")
                }

                return@BiPredicate false
            }

            if (signature != null && field.signature != signature) {
                if (self.debug) {
                    println("\t\tExpected signature $signature but got ${field.signature}")
                }

                return@BiPredicate false
            }

            modifiers.forEachIndexed { index, value ->
                if (value != null) {
                    val opcode = 1 shl index

                    if (((field.access and opcode) != 0) != value) {
                        if (self.debug) {
                            println("\t\tExpected modifier '${opcode.toAccessString()}' to be $value but got ${!value}")
                        }

                        return@BiPredicate false
                    }
                }
            }

            return@BiPredicate true
        }
    }

    fun name(name: String): FieldPointer {
        this.name = name
        return self()
    }

    fun desc(desc: String): FieldPointer {
        this.desc = desc
        return self()
    }

    fun signature(signature: String): FieldPointer {
        this.signature = signature
        return self()
    }

    fun modifier(modifier: Int, value: Boolean): FieldPointer {
        modifiers[Integer.numberOfTrailingZeros(modifier)] = value
        return self()
    }

    override fun find(target: ClassNode): List<FieldNode> {
        if (debug) {
            println("Locating $this in $target")
        }

        return target.fields.filter { field ->
            if (debug) {
                println("\tTesting field $field")
            }

            predicate.test(this, field)
        }
    }

    override fun toString(): String {
        return toString(
            "Field",
            name?.let { "name=$it" },
            desc?.let { "desc=$it" },
            signature?.let { "signature=$it" },
            if (modifiers.isEmpty()) null else toString(
                "modifiers=",
                *modifiers.mapIndexed { index, value -> "${(1 shl index).toAccessString()}=$value" }.toTypedArray()
            )
        )
    }

    companion object {
        @JvmStatic
        fun field(): FieldPointer {
            return FieldPointer()
        }
    }
}