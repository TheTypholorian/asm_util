package net.typho.asm_util.cfg

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode

object ControlFlowGraph {
    @JvmOverloads
    @JvmStatic
    fun build(insns: InsnList, tryCatchBlocks: List<TryCatchBlockNode>? = null): List<BasicBlock> {
        val insns = insns.toArray()
        val indices = insns.mapIndexed { index, node -> node to index }.toMap(mutableMapOf())
        val leaders = mutableSetOf<Int>()
        leaders.add(0)

        fun addLeader(insn: AbstractInsnNode?) {
            insn?.let { leaders.add(indices[insn]!!) }
        }

        insns.forEachIndexed { index, insn ->
            when (insn) {
                is JumpInsnNode -> {
                    addLeader(insn.label)

                    if (insn.opcode != Opcodes.GOTO) {
                        addLeader(insns.getOrNull(index + 1))
                    }
                }
                is TableSwitchInsnNode -> {
                    addLeader(insn.dflt)
                    insn.labels.forEach { addLeader(it) }
                    addLeader(insns.getOrNull(index + 1))
                }
                is LookupSwitchInsnNode -> {
                    addLeader(insn.dflt)
                    insn.labels.forEach { addLeader(it) }
                    addLeader(insns.getOrNull(index + 1))
                }
            }
        }

        tryCatchBlocks?.forEach {
            addLeader(it.start)
            addLeader(it.end)
            addLeader(it.handler)
        }

        val starts = leaders.sorted()
        val blocks = mutableListOf<BasicBlock>()
        val blocksByInsn = mutableMapOf<AbstractInsnNode, Int>()
        val blocksByStart = mutableMapOf<Int, Int>()

        starts.forEachIndexed { blockIndex, start ->
            val end = starts.getOrNull(blockIndex + 1) ?: insns.size
            blocks.add(BasicBlock(blockIndex, start, end))
            blocksByStart[start] = blockIndex

            for (index in start until end) {
                blocksByInsn[insns[index]] = blockIndex
            }
        }

        fun connect(from: Int, to: Int) {
            if (from != to) {
                val source = blocks[from]
                val target = blocks[to]

                if (!source.next.contains(to)) {
                    source.next.add(to)
                }

                if (!target.previous.contains(from)) {
                    target.previous.add(from)
                }
            }
        }

        fun blockOf(insn: AbstractInsnNode?) = insn?.let { blocksByInsn[it] }

        fun nextBlock(block: BasicBlock) = blocksByStart[block.end]

        fun isTerminal(opcode: Int) = opcode == Opcodes.IRETURN ||
                opcode == Opcodes.LRETURN ||
                opcode == Opcodes.FRETURN ||
                opcode == Opcodes.DRETURN ||
                opcode == Opcodes.ARETURN ||
                opcode == Opcodes.RETURN ||
                opcode == Opcodes.ATHROW

        for (block in blocks) {
            when (val last = insns[block.end - 1]) {
                is JumpInsnNode -> {
                    blockOf(last.label)?.let { connect(block.index, it) }

                    if (last.opcode != Opcodes.GOTO) {
                        nextBlock(block)?.let { connect(block.index, it) }
                    }
                }
                is TableSwitchInsnNode -> {
                    blockOf(last.dflt)?.let { connect(block.index, it) }

                    for (label in last.labels) {
                        blockOf(label)?.let { connect(block.index, it) }
                    }
                }
                is LookupSwitchInsnNode -> {
                    blockOf(last.dflt)?.let { connect(block.index, it) }

                    for (label in last.labels) {
                        blockOf(label)?.let { connect(block.index, it) }
                    }
                }
                else -> {
                    if (!isTerminal(last.opcode)) {
                        nextBlock(block)?.let { connect(block.index, it) }
                    }
                }
            }
        }

        tryCatchBlocks?.forEach {
            val start = indices[it.start]!!
            val end = indices[it.end]!!
            val handler = indices[it.handler]!!

            for (block in blocks) {
                if (block.start < end && block.end > start) {
                    connect(block.index, handler)
                }
            }
        }

        return blocks
    }
}