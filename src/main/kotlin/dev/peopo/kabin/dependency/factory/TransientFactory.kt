package dev.peopo.kabin.dependency.factory

class TransientFactory<out T: Any>(
    private val factory: () -> T
): Factory<T> {
    override fun provide(): T = this.factory()
}