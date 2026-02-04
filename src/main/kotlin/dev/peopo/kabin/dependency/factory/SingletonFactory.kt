package dev.peopo.kabin.dependency.factory

class SingletonFactory<out T: Any> (
    private val instance: T
): Factory<T> {
    override fun provide(): T = this.instance
}