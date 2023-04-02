package org.bakeneko.collections

import org.junit.Test
import kotlin.test.assertEquals

class CompactListTest {
    @Test
    fun `newCompactList returns the correct implementation depending on the type argument`() {
        val intList = newCompactList<Int>(1)
        assertEquals("CompactListInt", intList::class.simpleName)

        val genericList = newCompactList<String>(1)
        assertEquals("CompactListGeneric", genericList::class.simpleName)
    }
}