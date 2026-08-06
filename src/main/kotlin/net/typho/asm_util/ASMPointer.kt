package net.typho.asm_util

import java.util.Optional
import java.util.function.BiPredicate
import java.util.function.Consumer

@Suppress("UNCHECKED_CAST")
abstract class ASMPointer<R, T, S> {
    protected lateinit var predicate: BiPredicate<S, R>
    @JvmField
    protected var debug = false

    protected fun self() = this as S

    fun debug(): S {
        debug = true
        return self()
    }

    fun and(predicate: BiPredicate<S, R>): S {
        this.predicate = this.predicate.and(predicate)
        return self()
    }

    fun and(pointer: ASMPointer<R, T, S>) = and(pointer.predicate)

    fun or(predicate: BiPredicate<S, R>): S {
        this.predicate = this.predicate.or(predicate)
        return self()
    }

    fun or(pointer: ASMPointer<R, T, S>) = or(pointer.predicate)

    /**
     * The first value that matches this pointer, or empty if none match
     */
    abstract fun find(target: T): Optional<R>

    fun findOrThrow(target: T): R {
        return find(target).orElseThrow { NullPointerException("Unable to find $this in $target") }
    }

    fun findOrThrow(target: T, out: Consumer<R>) {
        out.accept(findOrThrow(target))
    }

    companion object {
        @JvmStatic
        protected fun toString(name: String, vararg values: String?): String {
            val values = values.filterNotNull()
            return if (values.isEmpty()) name else "$name[${values.joinToString(separator = ", ")}]"
        }
    }
}