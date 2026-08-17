package net.typho.asm_util.insn

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LabelNode
import java.util.function.BiPredicate

class LabelInsnPointer internal constructor() : InsnPointer<LabelNode, LabelInsnPointer>(AbstractInsnNode.LABEL) {
    init {
        predicate = BiPredicate { self, node -> true }
    }

    override fun toString(): String {
        return toString(
            "Label",
            ordinal?.let { "ordinal=$it" }
        )
    }
}