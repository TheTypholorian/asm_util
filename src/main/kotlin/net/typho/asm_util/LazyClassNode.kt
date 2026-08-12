package net.typho.asm_util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

class LazyClassNode(
    private val bytes: ByteArray
) {
    private var nodeCache: ClassNode? = null
    var node: ClassNode
        get() {
            nodeCache?.let { return it }

            val node = ClassNode()
            val reader = ClassReader(bytes)
            reader.accept(node, 0)

            this.node = node
            this.reader = reader

            return node
        }
        set(value) {
            nodeCache = value
        }
    var reader: ClassReader? = null
        private set
}