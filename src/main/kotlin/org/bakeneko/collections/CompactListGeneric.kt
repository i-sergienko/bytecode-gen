package org.bakeneko.collections

/**
 * A generic ordered collection of elements that supports adding elements.
 * @param T the type of elements contained in the list. The compact list is invariant in its element type.
 */
class CompactListGeneric<T>(initialCapacity: Int = 16) : CompactList<T> {
    private var lastElementIndex: Int = -1
    private var elements: Array<Any?>

    override val size: Int
        get() = lastElementIndex + 1

    init {
        if (initialCapacity < 1) {
            throw IllegalArgumentException("Invalid initialCapacity: expected to be in [1, Integer.MAX_VALUE].")
        }
        elements = Array(initialCapacity) { null }
    }

    override fun add(value: T) {
        if (elements.size == size) {
            growArray()
        }

        lastElementIndex++
        elements[lastElementIndex] = value
    }

    @SuppressWarnings("unchecked")
    override fun get(index: Int): T {
        if (index < 0 || index > lastElementIndex) {
            throw IndexOutOfBoundsException("Invalid index: expected [0, ${lastElementIndex}].")
        }

        return elements[index] as T
    }

    private fun growArray() {
        if (elements.size == Int.MAX_VALUE) {
            throw IllegalStateException("CompactList reached its maximum size and cannot grow any more.")
        }
        // If the size ends up overflowing Int at any point, use Int.MAX_VALUE.
        val newSize = if (elements.size * 2 > elements.size) elements.size * 2 else Int.MAX_VALUE

        val newElements = Array(newSize) { i -> if (i < elements.size) elements[i] else null }
        elements = newElements
    }
}