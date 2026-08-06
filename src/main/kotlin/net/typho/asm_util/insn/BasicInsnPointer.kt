package net.typho.asm_util.insn

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import java.util.function.BiPredicate

class BasicInsnPointer internal constructor() : InsnPointer<InsnNode, BasicInsnPointer>(AbstractInsnNode.INSN) {
    private var opcode: Int? = null

    init {
        predicate = BiPredicate { self, node ->
            if (opcode != null && node.opcode != opcode) {
                if (self.debug) {
                    println("\t\tExpected opcode $opcode but got ${node.opcode}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun opcode(opcode: Int): BasicInsnPointer {
        this.opcode = opcode
        return self()
    }

    override fun toString(): String {
        return toString(
            "Insn",
            ordinal?.let { "ordinal=$it" },
            opcode?.let { "opcode=$it" },
        )
    }
}