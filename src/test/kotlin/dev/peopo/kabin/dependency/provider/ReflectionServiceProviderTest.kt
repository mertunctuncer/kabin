package dev.peopo.kabin.dependency.provider

import dev.peopo.kabin.dependency.factory.SingletonFactory
import javax.inject.Inject
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class ReflectionServiceProviderTest {
    private class Leaf(val value: String = "default")

    private class NeedsLeaf(val leaf: Leaf)

    @Test
    fun `auto-resolves concrete types and their dependencies`() {
        val rsp = ReflectionServiceProvider()

        val got = rsp.provide<NeedsLeaf>(typeOf<NeedsLeaf>())

        assertNotNull(got)
        assertNotNull(got.leaf)
        assertEquals("default", got.leaf.value)
    }

    private class WantsString(val s: String)

    @Test
    fun `uses backing provider when available`() {
        val scoped = ScopedServiceProvider().apply { register("provided") }
        val rsp = ReflectionServiceProvider(scoped)

        val got = rsp.provide<WantsString>(typeOf<WantsString>())

        assertNotNull(got)
        assertEquals("provided", got.s)
    }

    private class RequiresUnresolvable(@Suppress("unused") val x: CharSequence)

    private class OptionalParam(val x: CharSequence = "fallback")

    @Test
    fun `throws when required parameter cannot be resolved`() {
        val rsp = ReflectionServiceProvider()

        val ex = assertFailsWith<IllegalStateException> {
            rsp.provide<RequiresUnresolvable>(typeOf<RequiresUnresolvable>())
        }

        assertNotNull(ex.message)
    }

    @Test
    fun `skips unresolved optional parameters and uses default`() {
        val rsp = ReflectionServiceProvider()

        val got = rsp.provide<OptionalParam>(typeOf<OptionalParam>())

        assertNotNull(got)
        assertEquals("fallback", got.x)
    }

    private class A(@Suppress("unused") val b: B)
    private class B(@Suppress("unused") val a: A)

    @Test
    fun `detects circular dependencies`() {
        val rsp = ReflectionServiceProvider()

        val ex = assertFailsWith<IllegalStateException> {
            rsp.provide<A>(typeOf<A>())
        }
        assertNotNull(ex.message)
    }

    private interface NotAClass

    @Test
    fun `fails for non-instantiable types like interfaces`() {
        val rsp = ReflectionServiceProvider()

        val ex = assertFailsWith<IllegalStateException> {
            rsp.provide<NotAClass>(typeOf<NotAClass>())
        }
        assertNotNull(ex.message)
    }

    @Suppress("unused")
    private class InjectChosen private constructor(val chosen: String) {
        constructor() : this("primary-ish")

        @Inject
        constructor(leaf: Leaf) : this("inject:${leaf.value}")
    }

    @Test
    fun `prefers single @Inject constructor when present`() {
        val rsp = ReflectionServiceProvider()

        val got = rsp.provide<InjectChosen>(typeOf<InjectChosen>())

        assertNotNull(got)
        assertEquals("inject:default", got.chosen)
    }

    @Suppress("unused")
    private class MultipleInjectCtors {
        @Inject
        constructor()

        @Inject
        constructor(leaf: Leaf)
    }

    @Test
    fun `throws when multiple @Inject constructors exist`() {
        val rsp = ReflectionServiceProvider()

        val ex = assertFailsWith<IllegalStateException> {
            rsp.provide<MultipleInjectCtors>(typeOf<MultipleInjectCtors>())
        }
        assertNotNull(ex.message)
    }

    private class Box<T : Any>(val value: T)
    private class GenericConsumer<T : Any>(val box: Box<T>)

    @Test
    fun `substitutes generic type parameters when resolving constructor params`() {
        val scoped = ScopedServiceProvider()
        val stringBox = Box("hello")
        scoped.registerUnsafe(typeOf<Box<String>>(), SingletonFactory(stringBox))

        val rsp = ReflectionServiceProvider(scoped)

        val got = rsp.provide<GenericConsumer<String>>(typeOf<GenericConsumer<String>>())

        assertNotNull(got)
        assertSame(stringBox, got.box)
        assertEquals("hello", got.box.value)
    }
}