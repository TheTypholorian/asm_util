package net.typho.asm_util.method

import net.typho.asm_util.ASMPointer
import net.typho.asm_util.Modifier
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.Optional
import java.util.function.BiPredicate

class MethodPointer private constructor() : ASMPointer<MethodNode, ClassNode, MethodPointer>() {
    private var name: String? = null
    private var desc: String? = null
    private var signature: String? = null
    private val modifiers = mutableMapOf<Modifier, Boolean>()

    init {
        predicate = BiPredicate { self, method ->
            if (name != null && method.name != name) {
                if (self.debug) {
                    println("\t\tExpected name $name but got ${method.name}")
                }

                return@BiPredicate false
            }

            if (desc != null && method.desc != desc) {
                if (self.debug) {
                    println("\t\tExpected desc $desc but got ${method.desc}")
                }

                return@BiPredicate false
            }

            if (signature != null && method.signature != signature) {
                if (self.debug) {
                    println("\t\tExpected signature $signature but got ${method.signature}")
                }

                return@BiPredicate false
            }

            for ((modifier, value) in modifiers) {
                if (((method.access and modifier.opcode) != 0) != value) {
                    if (self.debug) {
                        println("\t\tExpected modifier '${modifier.name.lowercase()}' to be $value but got ${!value}")
                    }

                    return@BiPredicate false
                }
            }

            return@BiPredicate true
        }
    }

    fun name(name: String): MethodPointer {
        this.name = name
        return self()
    }

    fun desc(desc: String): MethodPointer {
        this.desc = desc
        return self()
    }

    fun signature(signature: String): MethodPointer {
        this.signature = signature
        return self()
    }

    fun modifier(modifier: Modifier, value: Boolean): MethodPointer {
        modifiers[modifier] = value
        return self()
    }

    fun modifier(opcode: Int, value: Boolean): MethodPointer {
        return modifier(Modifier.entries.first { it.opcode == opcode }, value)
    }

    override fun find(target: ClassNode): Optional<MethodNode> {
        if (debug) {
            println("Locating $this in $target")
        }

        for (method in target.methods) {
            if (debug) {
                println("\tTesting method $method")
            }

            if (predicate.test(this, method)) {
                return Optional.of(method)
            }
        }

        return Optional.empty()
    }

    override fun toString(): String {
        return toString(
            "Method",
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
        fun method(): MethodPointer {
            return MethodPointer()
        }
    }
}