package net.typho.asm_util.insn

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import java.util.function.BiPredicate

class MethodInsnPointer internal constructor() : InsnPointer<MethodInsnNode, MethodInsnPointer>(AbstractInsnNode.METHOD_INSN) {
    private var opcode: Int? = null
    private var owner: String? = null
    private var name: String? = null
    private var desc: String? = null

    init {
        predicate = BiPredicate { self, node ->
            if (opcode != null && node.opcode != opcode) {
                if (self.debug) {
                    println("\t\tExpected opcode $opcode but got ${node.opcode}")
                }

                return@BiPredicate false
            }

            if (owner != null && node.owner != owner) {
                if (self.debug) {
                    println("\t\tExpected owner $owner but got ${node.owner}")
                }

                return@BiPredicate false
            }

            if (desc != null && node.desc != desc) {
                if (self.debug) {
                    println("\t\tExpected desc $desc but got ${node.desc}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun opcode(opcode: Int): MethodInsnPointer {
        this.opcode = opcode
        return self()
    }

    fun owner(owner: String): MethodInsnPointer {
        this.owner = owner
        return self()
    }

    fun name(name: String): MethodInsnPointer {
        this.name = name
        return self()
    }

    fun desc(desc: String): MethodInsnPointer {
        this.desc = desc
        return self()
    }

    override fun toString(): String {
        return toString(
            "Method",
            ordinal?.let { "ordinal=$it" },
            opcode?.let { "opcode=$it" },
            owner?.let { "owner=$it" },
            name?.let { "name=$it" },
            desc?.let { "desc=$it" },
        )
    }
}