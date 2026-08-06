package net.typho.asm_util.insn

import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import java.util.function.BiPredicate
import kotlin.reflect.KClass

class ConstantInsnPointer internal constructor() : InsnPointer<LdcInsnNode, ConstantInsnPointer>(AbstractInsnNode.LDC_INSN) {
    private var value: Any? = null

    init {
        predicate = BiPredicate { self, node ->
            if (value != null && node.cst != value) {
                if (self.debug) {
                    println("\t\tExpected constant value $value but got ${node.cst}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun value(value: Int): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: Float): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: Long): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: Double): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: String): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: Type): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: Class<*>): ConstantInsnPointer {
        return value(Type.getType(value))
    }

    fun value(value: KClass<*>): ConstantInsnPointer {
        return value(value.java)
    }

    fun value(value: Handle): ConstantInsnPointer {
        this.value = value
        return self()
    }

    fun value(value: ConstantDynamic): ConstantInsnPointer {
        this.value = value
        return self()
    }

    override fun toString(): String {
        return toString(
            "Constant",
            ordinal?.let { "ordinal=$it" },
            value?.let { "value=$it" },
        )
    }
}