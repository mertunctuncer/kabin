package dev.peopo.kabin.dependency.provider

import kotlin.reflect.KType

interface ServiceProvider {
    fun <T: Any> provide(type: KType): T?
}