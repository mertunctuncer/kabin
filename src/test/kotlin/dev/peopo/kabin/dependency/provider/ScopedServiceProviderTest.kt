package dev.peopo.kabin.dependency.provider

import dev.peopo.kabin.dependency.factory.SingletonFactory
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ScopedServiceProviderTest {

    @Test
    fun `register instance and provide by exact type`() {
        val sp = ScopedServiceProvider()
        val instance = "value"

        sp.register(instance)

        val got = sp.provide<String>(typeOf<String>())
        assertEquals("value", got)
    }

    @Test
    fun `register lambda creates transient instances`() {
        val sp = ScopedServiceProvider()
        var counter = 0

        sp.register { ++counter }

        val a = sp.provide<Int>(typeOf<Int>())
        val b = sp.provide<Int>(typeOf<Int>())

        assertEquals(1, a)
        assertEquals(2, b)
    }

    @Test
    fun `register factory works`() {
        val sp = ScopedServiceProvider()
        val obj = Any()

        sp.register(SingletonFactory(obj))

        val got = sp.provide<Any>(typeOf<Any>())
        assertSame(obj, got)
    }

    @Test
    fun `provide falls back to star-projected type when exact KType not registered`() {
        val sp = ScopedServiceProvider()
        val list: List<*> = listOf("a", "b")

        sp.registerUnsafe(typeOf<List<*>>(), SingletonFactory(list))

        val got = sp.provide<List<String>>(typeOf<List<String>>())
        assertNotNull(got)
        assertSame(list, got)
    }

    @Test
    fun `clear removes all registrations`() {
        val sp = ScopedServiceProvider()
        sp.register("x")
        assertNotNull(sp.provide<String>(typeOf<String>()))

        sp.clear()

        assertNull(sp.provide<String>(typeOf<String>()))
    }
}