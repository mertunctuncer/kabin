package dev.peopo.kabin.dependency.factory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class FactoryTest {

    @Test
    fun `SingletonFactory returns the same instance every time`() {
        val obj = Any()
        val factory = SingletonFactory(obj)

        val a = factory.provide()
        val b = factory.provide()

        assertSame(obj, a)
        assertSame(a, b)
    }

    @Test
    fun `TransientFactory invokes supplier each time`() {
        var calls = 0
        val factory = TransientFactory {
            calls++
            Any()
        }

        val a = factory.provide()
        val b = factory.provide()

        assertEquals(2, calls)
        assertNotSame(a, b)
    }

}