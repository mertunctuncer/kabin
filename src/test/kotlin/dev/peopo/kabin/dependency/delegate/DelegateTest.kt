package dev.peopo.kabin.dependency.delegate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DelegateTest {

    private class HolderEager(value: String) {
        val dep: String by EagerInjectionDelegate(value)
    }

    private class HolderLazy(provider: () -> String) {
        val dep: String by LazyInjectionDelegate(provider)
    }

    @Test
    fun `EagerInjectionDelegate always returns same stored value`() {
        val holder = HolderEager("hello")
        assertEquals("hello", holder.dep)
        assertSame(holder.dep, holder.dep)
    }

    @Test
    fun `LazyInjectionDelegate computes value once and caches it`() {
        var calls = 0
        val holder = HolderLazy {
            calls++
            "computed"
        }

        assertEquals("computed", holder.dep)
        assertEquals("computed", holder.dep)
        assertEquals(1, calls)
    }

}