package dev.peopo.kabin.dependency.delegate

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class EagerInjectionDelegate<out T : Any>(
    private val value: T
) : ReadOnlyProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = this.value
}