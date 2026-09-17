package net.typho.asm_util.insn

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.JumpInsnNode
import java.util.function.BiPredicate

class JumpInsnPointer internal constructor() : InsnPointer<JumpInsnNode, JumpInsnPointer>(AbstractInsnNode.JUMP_INSN) {
    private var opcode: Int? = null
    private var label: LabelNode? = null

    init {
        predicate = BiPredicate { self, node ->
            if (opcode != null && node.opcode != opcode) {
                if (self.debug) {
                    println("\t\tExpected opcode $opcode but got ${node.opcode}")
                }

                return@BiPredicate false
            }

            if (label != null && node.label != label) {
                if (self.debug) {
                    println("\t\tExpected label $label but got ${node.label}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun opcode(opcode: Int): JumpInsnPointer {
        this.opcode = opcode
        return self()
    }

    fun label(label: LabelNode): JumpInsnPointer {
        this.label = label
        return self()
    }

    override fun toString(): String {
        return toString(
            "Jump",
            ordinal?.let { "ordinal=$it" },
            opcode?.let { "opcode=$it" },
            label?.let { "label=$label" },
        )
    }
}