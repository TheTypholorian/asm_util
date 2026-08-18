package net.typho.asm_util.insn

import net.typho.asm_util.ASMPointer
import net.typho.asm_util.ASMUtil.iterateSlice
import org.objectweb.asm.ConstantDynamic
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import java.util.*

@Suppress("unused", "UNCHECKED_CAST")
abstract class InsnPointer<T : AbstractInsnNode, S : InsnPointer<T, S>> protected constructor(
    @JvmField
    protected val type: Int
) : ASMPointer<T, InsnList, S>() {
    @JvmField
    protected var ordinal: Int? = null
    @JvmField
    protected val after = mutableSetOf<AbstractInsnNode>()
    @JvmField
    protected val before = mutableSetOf<AbstractInsnNode>()

    fun ordinal(ordinal: Int): S {
        this.ordinal = ordinal
        return self()
    }

    fun lastOrdinal(): S {
        return ordinal(Int.MAX_VALUE)
    }

    fun after(insn: AbstractInsnNode): S {
        after.add(insn)
        return self()
    }

    fun before(insn: AbstractInsnNode): S {
        before.add(insn)
        return self()
    }

    private fun test(insn: AbstractInsnNode): Boolean {
        if (insn.type == type) {
            if (predicate.test(self(), insn as T)) {
                return true
            } else if (debug) {
                println("\t\tFailed predicate")
            }
        } else if (debug) {
            println("\t\tWrong type, expected $type but got ${insn.type}")
        }

        return false
    }

    override fun find(target: InsnList): List<T> {
        if (debug) {
            println("Locating $this in $target")
        }

        if (ordinal == Int.MAX_VALUE) {
            target.iterateSlice(after, before).forEach { insn ->
                if (debug) {
                    println("\tTesting opcode #${insn.opcode} $insn")
                }

                if (test(insn)) {
                    return listOf(insn as T)
                }
            }

            return listOf()
        } else {
            var i = 0
            val matches = mutableListOf<T>()

            for (insn in target.iterateSlice(after, before)) {
                if (debug) {
                    println("\tTesting opcode #${insn.opcode} $insn")
                }

                if (test(insn)) {
                    if (i == ordinal || ordinal == null) {
                        if (debug) {
                            println("\t\tFound a match!")
                        }

                        matches.add(insn as T)

                        if (ordinal != null) {
                            return matches
                        }
                    } else if (debug) {
                        println("\t\tFailed ordinal test, expected $ordinal but got $i")
                    }

                    i++
                }
            }

            return matches
        }
    }

    companion object {
        /**
         * Method call
         */
        @JvmStatic
        fun methodCall(): MethodInsnPointer {
            return MethodInsnPointer()
        }

        /**
         * Non-final instance method call
         */
        @JvmStatic
        fun methodCallInherited(): MethodInsnPointer {
            return MethodInsnPointer().opcode(Opcodes.INVOKEVIRTUAL)
        }

        /**
         * Final instance method call or constructor call
         */
        @JvmStatic
        fun methodCallDirect(): MethodInsnPointer {
            return MethodInsnPointer().opcode(Opcodes.INVOKESPECIAL)
        }

        /**
         * Static method call
         */
        @JvmStatic
        fun methodCallStatic(): MethodInsnPointer {
            return MethodInsnPointer().opcode(Opcodes.INVOKESTATIC)
        }

        /**
         * Interface method call
         */
        @JvmStatic
        fun methodCallInterface(): MethodInsnPointer {
            return MethodInsnPointer().opcode(Opcodes.INVOKEINTERFACE)
        }

        /**
         * I have no clue what this one does
         */
        @JvmStatic
        fun methodCallDynamic(): MethodInsnPointer {
            return MethodInsnPointer().opcode(Opcodes.INVOKEDYNAMIC)
        }

        /**
         * Static or instance field get or set
         */
        @JvmStatic
        fun fieldOperation(): FieldInsnPointer {
            return FieldInsnPointer()
        }

        /**
         * Instance field get
         */
        @JvmStatic
        fun fieldGet(): FieldInsnPointer {
            return fieldOperation().opcode(Opcodes.GETFIELD)
        }

        /**
         * Static field get
         */
        @JvmStatic
        fun fieldGetStatic(): FieldInsnPointer {
            return fieldOperation().opcode(Opcodes.GETSTATIC)
        }

        /**
         * Instance field set
         */
        @JvmStatic
        fun fieldSet(): FieldInsnPointer {
            return fieldOperation().opcode(Opcodes.PUTFIELD)
        }

        /**
         * Static field set
         */
        @JvmStatic
        fun fieldSetStatic(): FieldInsnPointer {
            return fieldOperation().opcode(Opcodes.PUTSTATIC)
        }

        /**
         * Constant value
         */
        @JvmStatic
        fun constant(): ConstantInsnPointer {
            return ConstantInsnPointer()
        }

        /**
         * Constant boolean, byte, short, or integer value
         */
        @JvmStatic
        fun constant(value: Int): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Constant long value
         */
        @JvmStatic
        fun constant(value: Long): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Constant float value
         */
        @JvmStatic
        fun constant(value: Float): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Constant double value
         */
        @JvmStatic
        fun constant(value: Double): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Constant string value
         */
        @JvmStatic
        fun constant(value: String): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Constant type value
         */
        @JvmStatic
        fun constant(value: Type): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * Handles
         */
        @JvmStatic
        fun constant(value: Handle): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        /**
         * ConstantDynamics
         */
        @JvmStatic
        fun constant(value: ConstantDynamic): ConstantInsnPointer {
            return ConstantInsnPointer().value(value)
        }

        @JvmStatic
        fun type(): TypeInsnPointer {
            return TypeInsnPointer()
        }

        @JvmStatic
        fun type(desc: String): TypeInsnPointer {
            return TypeInsnPointer().desc(desc)
        }

        @JvmStatic
        fun type(type: Type): TypeInsnPointer {
            return TypeInsnPointer().desc(type)
        }

        @JvmStatic
        fun type(cls: Class<*>): TypeInsnPointer {
            return TypeInsnPointer().desc(cls)
        }

        /**
         * Local variable operation
         */
        @JvmStatic
        fun localOperation(): VarInsnPointer {
            return VarInsnPointer()
        }

        /**
         * No-argument instruction (constants, returns, pops, etc.)
         */
        @JvmStatic
        fun simple(): BasicInsnPointer {
            return BasicInsnPointer()
        }

        @JvmStatic
        fun label(): LabelInsnPointer {
            return LabelInsnPointer()
        }
    }
}