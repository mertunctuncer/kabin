package dev.peopo.kabin.dependency.provider

import dev.peopo.kabin.dependency.factory.Factory
import dev.peopo.kabin.dependency.factory.SingletonFactory
import dev.peopo.kabin.dependency.factory.TransientFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

class ScopedServiceProvider: ServiceProvider {

    private val providers = ConcurrentHashMap<KType, Factory<*>>()

    @PublishedApi
    internal fun registerUnsafe(type: KType, factory: Factory<*>) {
        this.providers[type] = factory
    }

    inline fun <reified T : Any> register(factory: Factory<T>) {
        registerUnsafe(typeOf<T>(), factory)
    }

    inline fun <reified T : Any> register(instance: T): Unit = this.register( SingletonFactory(instance))

    inline fun <reified T : Any> register(noinline factory: () -> T): Unit = this.register(TransientFactory(factory))

    @Suppress("UNCHECKED_CAST")
    override fun <T: Any> provide(type: KType): T? {
        return (
                this.providers[type]?.provide()
                    ?: (type.classifier as? KClass<T>)?.starProjectedType?.let { this.providers[it]?.provide() }
                ) as? T
    }

    fun clear(): Unit = this.providers.clear()
}