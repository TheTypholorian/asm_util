package net.typho.asm_util.cfg

data class BasicBlock(
    @JvmField
    val index: Int,
    @JvmField
    val start: Int,
    @JvmField
    val end: Int,
    @JvmField
    val previous: MutableList<Int>,
    @JvmField
    val next: MutableList<Int>
)
