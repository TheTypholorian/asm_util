package net.typho.asm_util.cfg

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode

class ControlFlowGraph(
    @JvmField
    val blocks: List<BasicBlock>,
    @JvmField
    val blocksByIndex: Map<Int, BasicBlock>,
    @JvmField
    val blocksByInsn: Map<AbstractInsnNode, Int>,
    @JvmField
    val blocksByInsnIndex: Map<Int, Int>
) {
    private var dominatorTree: DominatorTree? = null

    fun getCommonParent(blocks: List<BasicBlock>): BasicBlock? {
        if (blocks.isEmpty()) {
            return null
        }

        val tree = (dominatorTree ?: DominatorTree().also { dominatorTree = it })

        var common = tree.dominators[blocks.first().index]?.toMutableSet() ?: return null

        for (block in blocks.drop(1)) {
            common.retainAll(tree.dominators[block.index] ?: return null)
        }

        return common.maxByOrNull { tree.dominators[it]?.size ?: 0 }?.let { blocks[it] }
    }

    private inner class DominatorTree {
        @JvmField
        val dominators = mutableMapOf<Int, MutableSet<Int>>()
        @JvmField
        val idoms = mutableMapOf<Int, Int>()
        @JvmField
        val children = mutableMapOf<Int, MutableList<Int>>()

        init {
            val entry = blocks.first()

            for (block in blocks) {
                dominators[block.index] = if (block == entry) mutableSetOf(block.index) else blocks.mapTo(mutableSetOf()) { it.index }
                children[block.index] = mutableListOf()
            }

            var changed = true

            while (changed) {
                changed = false

                for (block in blocks) {
                    if (block == entry || block.previous.isEmpty()) {
                        continue
                    }

                    val common = dominators[block.previous.first()]!!.toMutableSet()

                    for (parent in block.previous.drop(1)) {
                        common.retainAll(dominators[parent]!!)
                    }

                    common.add(block.index)

                    if (common != dominators[block.index]) {
                        dominators[block.index] = common
                        changed = true
                    }
                }
            }

            for (block in blocks) {
                if (block == entry) {
                    continue
                }

                val candidates = dominators[block.index]!!.filter { it != block.index }
                val idom = candidates.firstOrNull { candidate -> candidates.none { other -> other != candidate && dominators[other]!!.contains(candidate) } }

                if (idom != null) {
                    idoms[block.index] = idom
                    children[idom]!!.add(block.index)
                }
            }
        }
    }

    companion object {
        @JvmOverloads
        @JvmStatic
        fun build(insns: InsnList, tryCatchBlocks: List<TryCatchBlockNode>? = null): ControlFlowGraph {
            val insns = insns.toArray()
            val indices = insns.mapIndexed { index, node -> node to index }.toMap(mutableMapOf())
            val leaders = mutableSetOf<Int>()
            leaders.add(0)

            fun addLeader(insn: AbstractInsnNode?) {
                insn?.let { leaders.add(indices[it]!!) }
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
            val blocksByIndex = mutableMapOf<Int, Int>()

            starts.forEachIndexed { blockIndex, start ->
                val end = starts.getOrNull(blockIndex + 1) ?: insns.size
                blocks.add(BasicBlock(blockIndex, start, end))
                blocksByIndex[start] = blockIndex

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

            for (block in blocks) {
                when (val last = insns[block.end - 1]) {
                    is JumpInsnNode -> {
                        blocksByInsn[last.label]?.let { connect(block.index, it) }

                        if (last.opcode != Opcodes.GOTO) {
                            blocksByIndex[block.end]?.let { connect(block.index, it) }
                        }
                    }
                    is TableSwitchInsnNode -> {
                        blocksByInsn[last.dflt]?.let { connect(block.index, it) }

                        for (label in last.labels) {
                            blocksByInsn[label]?.let { connect(block.index, it) }
                        }
                    }
                    is LookupSwitchInsnNode -> {
                        blocksByInsn[last.dflt]?.let { connect(block.index, it) }

                        for (label in last.labels) {
                            blocksByInsn[label]?.let { connect(block.index, it) }
                        }
                    }
                    else -> {
                        if (!(last.opcode == Opcodes.IRETURN ||
                                    last.opcode == Opcodes.LRETURN ||
                                    last.opcode == Opcodes.FRETURN ||
                                    last.opcode == Opcodes.DRETURN ||
                                    last.opcode == Opcodes.ARETURN ||
                                    last.opcode == Opcodes.RETURN ||
                                    last.opcode == Opcodes.ATHROW)
                        ) {
                            blocksByIndex[block.end]?.let { connect(block.index, it) }
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

            return ControlFlowGraph(blocks, blocks.associateBy { it.index }, blocksByInsn, blocksByIndex)
        }
    }
}