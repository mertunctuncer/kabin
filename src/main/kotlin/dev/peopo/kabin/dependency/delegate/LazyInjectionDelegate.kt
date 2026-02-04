package dev.peopo.kabin.dependency.delegate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class LazyInjectionDelegate<out T: Any>(
    provider: () -> T
): ReadOnlyProperty<Any?, T> {
    private val value by lazy { provider.invoke() }
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.value
}