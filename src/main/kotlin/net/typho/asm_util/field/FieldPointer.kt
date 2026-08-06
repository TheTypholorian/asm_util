package net.typho.asm_util.field

import net.typho.asm_util.ASMPointer
import net.typho.asm_util.Modifier
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.util.Optional
import java.util.function.BiPredicate
import kotlin.collections.iterator

class FieldPointer private constructor() : ASMPointer<FieldNode, ClassNode, FieldPointer>() {
    private var name: String? = null
    private var desc: String? = null
    private var signature: String? = null
    private val modifiers = mutableMapOf<Modifier, Boolean>()

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

            for ((modifier, value) in modifiers) {
                if (((field.access and modifier.opcode) != 0) != value) {
                    if (self.debug) {
                        println("\t\tExpected modifier '${modifier.name.lowercase()}' to be $value but got ${!value}")
                    }

                    return@BiPredicate false
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

    fun modifier(modifier: Modifier, value: Boolean): FieldPointer {
        modifiers[modifier] = value
        return self()
    }

    fun modifier(opcode: Int, value: Boolean): FieldPointer {
        return modifier(Modifier.entries.first { it.opcode == opcode }, value)
    }

    override fun find(target: ClassNode): Optional<FieldNode> {
        if (debug) {
            println("Locating $this in $target")
        }

        for (field in target.fields) {
            if (debug) {
                println("\tTesting field $field")
            }

            if (predicate.test(this, field)) {
                return Optional.of(field)
            }
        }

        return Optional.empty()
    }

    override fun toString(): String {
        return toString(
            "Field",
            name?.let { "name=$it" },
            desc?.let { "desc=$it" },
            signature?.let { "signature=$it" },
            if (modifiers.isEmpty()) null else toString(
                "modifiers=",
                *modifiers.map { (modifier, value) -> "${modifier.name.lowercase()}=$value" }.toTypedArray()
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