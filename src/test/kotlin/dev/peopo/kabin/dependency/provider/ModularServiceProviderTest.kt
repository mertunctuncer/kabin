package dev.peopo.kabin.dependency.provider

import dev.peopo.kabin.dependency.factory.SingletonFactory
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class ModularServiceProviderTest {

    @Test
    fun `register host and provide by exact type`() {
        val sp = ModularServiceProvider()
        sp.register(typeOf<String>(), SingletonFactory("host"))

        val got = sp.provide<String>(typeOf<String>())
        assertEquals("host", got)
    }

    @Test
    fun `unregister removes only services owned by that ownerId`() {
        val sp = ModularServiceProvider()

        sp.register(ownerId = "moduleA", type = typeOf<String>(), factory = SingletonFactory("A"))
        sp.register(ownerId = "moduleB", type = typeOf<Int>(), factory = SingletonFactory(42))

        sp.register(typeOf<Long>(), SingletonFactory(7L))

        sp.unregister("moduleA")

        assertNull(sp.provide<String>(typeOf<String>()))
        assertNotNull(sp.provide<Int>(typeOf<Int>()))
        assertNotNull(sp.provide<Long>(typeOf<Long>()))
    }

    @Test
    fun `unregister is idempotent for unknown owner`() {
        val sp = ModularServiceProvider()
        sp.register(ownerId = "moduleA", instance = "A")

        sp.unregister("does-not-exist")

        assertEquals("A", sp.provide<String>(typeOf<String>()))
    }

    @Test
    fun `provide falls back to star-projected type`() {
        val sp = ModularServiceProvider()
        val map: Map<*, *> = mapOf("k" to 1)

        sp.register(ownerId = "moduleA", type = typeOf<Map<* , *>>(), factory = SingletonFactory(map))

        val got = sp.provide<Map<String, Int>>(typeOf<Map<String, Int>>())
        assertNotNull(got)
        assertSame(map, got)
    }

    @Test
    fun `clear wipes providers and owner index`() {
        val sp = ModularServiceProvider()
        sp.register(ownerId = "moduleA", instance = "A")
        sp.register(instance = 1)

        sp.clear()

        assertNull(sp.provide<String>(typeOf<String>()))
        assertNull(sp.provide<Int>(typeOf<Int>()))
    }

}