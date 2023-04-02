package org.bakeneko.collections

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.Opcodes
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

/**
 * [CompactList] is an ordered collection of elements of the same type [T].
 * */
interface CompactList<T> {
    val size: Int

    /** Adds the specified element to the end of this list. */
    fun add(value: T)

    /** Returns the element at the specified [index] in the list. */
    fun get(index: Int): T
}

/**
 * [newCompactList] returns an instance of an implementation of [CompactList].
 *
 * The specific implementation to use is determined by the type parameter [T] - if [T] is equivalent to [Int],
 * then an implementation is generated dynamically, otherwise an instance of the statically defined
 * [CompactListGeneric] is used.
 * */
inline fun <reified T> newCompactList(initialCapacity: Int = 16): CompactList<T> {
    return when (T::class) {
        Int::class -> {
            val cls = CompactListIntLoader.loadCompactList()

            try {
                cls.getDeclaredConstructor(Int::class.java).newInstance(initialCapacity) as CompactList<T>
            } catch (e: InvocationTargetException) {
                // Any exceptions thrown from methods and constructors invoked via reflection
                // are wrapped in a InvocationTargetException. We need to unwrap the exception to get the original
                // message.
                throw e.targetException
            }
        }

        else -> CompactListGeneric(initialCapacity)
    }
}