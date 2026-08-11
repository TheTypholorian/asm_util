package net.typho.asm_util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class LazyClassNode(
    private val bytes: ByteArray
) {
    private var node: ClassNode? = null
    private var reader: ClassReader? = null

    fun getNode(): ClassNode {
        node?.let { return it }

        val node = ClassNode()
        val reader = ClassReader(bytes)
        reader.accept(node, 0)

        this.node = node
        this.reader = reader

        return node
    }

    fun getReader() = reader

    operator fun getValue(owner: Any?, property: Any?) = getNode()
}