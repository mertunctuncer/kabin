package dev.peopo.kabin.dependency.factory

interface Factory<out T: Any> {
    fun provide(): T
}