package net.typho.asm_util.insn

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import java.util.function.BiPredicate
import kotlin.reflect.KClass

class TypeInsnPointer internal constructor() : InsnPointer<TypeInsnNode, TypeInsnPointer>(AbstractInsnNode.TYPE_INSN) {
    private var desc: String? = null

    init {
        predicate = BiPredicate { self, node ->
            if (desc != null && node.desc != desc) {
                if (self.debug) {
                    println("\t\tExpected desc $desc but got ${node.desc}")
                }

                return@BiPredicate false
            }

            return@BiPredicate true
        }
    }

    fun desc(desc: String): TypeInsnPointer {
        this.desc = desc
        return self()
    }

    fun desc(desc: Type): TypeInsnPointer {
        return desc(desc.internalName)
    }

    fun desc(desc: Class<*>): TypeInsnPointer {
        return desc(Type.getType(desc))
    }

    fun desc(desc: KClass<*>): TypeInsnPointer {
        return desc(desc.java)
    }

    override fun toString(): String {
        return toString(
            "Type",
            ordinal?.let { "ordinal=$it" },
            desc?.let { "desc=$it" },
        )
    }
}