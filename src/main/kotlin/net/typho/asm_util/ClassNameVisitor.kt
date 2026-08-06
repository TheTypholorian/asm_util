package net.typho.asm_util

import org.objectweb.asm.ClassVisitor
import java.util.function.Consumer

class ClassNameVisitor : ClassVisitor {
    @JvmField
    val out: Consumer<String>

    constructor(api: Int, out: Consumer<String>) : super(api) {
        this.out = out
    }

    constructor(api: Int, classVisitor: ClassVisitor?, out: Consumer<String>) : super(api, classVisitor) {
        this.out = out
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String,
        superName: String?,
        interfaces: Array<String>?
    ) {
        out.accept(name)
        super.visit(version, access, name, signature, superName, interfaces)
    }
}