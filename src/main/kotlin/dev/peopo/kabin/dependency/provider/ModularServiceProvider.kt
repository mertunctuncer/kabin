package dev.peopo.kabin.dependency.provider

import dev.peopo.kabin.dependency.factory.TransientFactory
import dev.peopo.kabin.dependency.factory.Factory
import dev.peopo.kabin.dependency.factory.SingletonFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf

class ModularServiceProvider: ServiceProvider {
    private data class Owned(val ownerId: String, val factory: Factory<*>)

    private val providers = ConcurrentHashMap<KType, Owned>()
    private val ownerIndex = ConcurrentHashMap<String, MutableSet<KType>>()

    internal fun registerUnsafe(ownerId: String, type: KType, factory: Factory<*>) {
        this.providers[type] = Owned(ownerId, factory)
        this.ownerIndex.computeIfAbsent(ownerId) { ConcurrentHashMap.newKeySet() }.add(type)
    }

    fun <T: Any> register(type: KType, factory: Factory<T>) {
        this.register(ownerId = "host", type = type, factory = factory)
    }

    fun <T: Any> register(ownerId: String, type: KType, factory: Factory<T>) {
        this.registerUnsafe(ownerId, type, factory)
    }

    inline fun <reified T : Any> register(instance: T) {
        this.register(typeOf<T>(), SingletonFactory(instance))
    }

    @Suppress("unused")
    inline fun <reified T : Any> register(noinline factory: () -> T) {
        this.register(typeOf<T>(), TransientFactory(factory))
    }

    inline fun <reified T : Any> register(ownerId: String, instance: T) {
        this.register(ownerId, typeOf<T>(), SingletonFactory(instance))
    }

    @Suppress("unused")
    inline fun <reified T : Any> register(ownerId: String, noinline factory: () -> T) {
        this.register(ownerId, typeOf<T>(), TransientFactory(factory))
    }

    fun unregister(ownerId: String) {
        val types = this.ownerIndex.remove(ownerId) ?: return
        for (t in types) {
            val owned = this.providers[t]
            if (owned != null && owned.ownerId == ownerId) {
                this.providers.remove(t)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> provide(type: KType): T? {
        return (
                this.providers[type]?.factory?.provide()
                    ?: (type.classifier as? KClass<T>)?.starProjectedType?.let { this.providers[it]?.factory?.provide() }
                ) as? T
    }

    fun clear() {
        this.providers.clear()
        this.ownerIndex.clear()
    }
}