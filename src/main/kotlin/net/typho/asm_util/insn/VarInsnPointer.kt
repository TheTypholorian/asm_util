package net.typho.asm_util.insn

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.util.function.BiPredicate

class VarInsnPointer internal constructor() : InsnPointer<VarInsnNode, VarInsnPointer>(AbstractInsnNode.VAR_INSN) {
    private var opcode: Int? = null
    private var id: Int? = null

    init {
        predicate = BiPredicate { self, node ->
            if (opcode != null && node.opcode != opcode) {
                if (self.debug) {
                    println("\t\tExpected opcode $opcode but got ${node.opcode}")
                }

                return@BiPredicate false
            }

            if (id != null && node.`var` != id) {
                if (self.debug) {
                    println("\t\tExpected id $id but got ${node.`var`}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun opcode(opcode: Int): VarInsnPointer {
        this.opcode = opcode
        return self()
    }

    fun id(id: Int): VarInsnPointer {
        this.id = id
        return self()
    }

    override fun toString(): String {
        return toString(
            "Var",
            ordinal?.let { "ordinal=$it" },
            opcode?.let { "opcode=$it" },
            id?.let { "id=$it" },
        )
    }
}