package net.typho.asm_util.method

import net.typho.asm_util.ASMPointer
import net.typho.asm_util.AccessUtil.toAccessString
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.util.function.BiPredicate

class MethodPointer private constructor() : ASMPointer<MethodNode, ClassNode, MethodPointer>() {
    private var name: String? = null
    private var desc: String? = null
    private var signature: String? = null
    private val modifiers = Array<Boolean?>(32) { null }

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

            modifiers.forEachIndexed { index, value ->
                if (value != null) {
                    val opcode = 1 shl index

                    if (((method.access and opcode) != 0) != value) {
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

    fun modifier(modifier: Int, value: Boolean): MethodPointer {
        modifiers[Integer.numberOfTrailingZeros(modifier)] = value
        return self()
    }

    override fun find(target: ClassNode): List<MethodNode> {
        if (debug) {
            println("Locating $this in $target")
        }

        return target.methods.filter { method ->
            if (debug) {
                println("\tTesting method $method")
            }

            predicate.test(this, method)
        }
    }

    override fun toString(): String {
        return toString(
            "Method",
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
        fun method(): MethodPointer {
            return MethodPointer()
        }
    }
}