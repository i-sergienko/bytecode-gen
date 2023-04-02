package org.bakeneko.collections

import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.test.assertEquals

class CompactListGenericTest {
    @Test
    fun `ensure initialization fails when initialCapacity is too low`() {
        assertThrows(IllegalArgumentException::class.java) { newCompactList<String>(0) }
    }

    @Test
    fun `ensure the list keeps growing as new elements are added`() {
        val subject = newCompactList<String>(1)
        val elementsToAdd = 1_048_576

        (0 until elementsToAdd).forEach { expectedSize ->
            assertEquals(expectedSize, subject.size)
            subject.add(expectedSize.toString())
            assertEquals(expectedSize + 1, subject.size)
            assertEquals(expectedSize.toString(), subject.get(expectedSize))
        }

        (0 until elementsToAdd).forEach { i -> assertEquals(i.toString(), subject.get(i)) }
    }

    @Test
    fun `ensure get fails on invalid indices`() {
        val subject = newCompactList<String>(16)

        assertThrows(IndexOutOfBoundsException::class.java) { subject.get(-1) }
        assertThrows(IndexOutOfBoundsException::class.java) { subject.get(0) }
        assertThrows(IndexOutOfBoundsException::class.java) { subject.get(1) }

        subject.add("0")
        assertEquals("0", subject.get(0))
        assertThrows(IndexOutOfBoundsException::class.java) { subject.get(1) }

        subject.add("1")
        assertEquals("0", subject.get(0))
        assertEquals("1", subject.get(1))
    }
}