package dev.peopo.kabin.dependency.provider

import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor

internal class ReflectionServiceProvider(
    private val provider: ServiceProvider? = null
)  : ServiceProvider {
    constructor(): this(null)

    private val stack = ThreadLocal.withInitial { mutableSetOf<KType>() }

    override fun <T : Any> provide(type: KType): T? {
        return this.resolve(type) as? T
    }

    private fun resolve(type: KType): Any? {
        return this.provider?.provide(type) ?: this.autoResolve(type)
    }

    private fun autoResolve(type: KType): Any {
        val current = this.stack.get()
        if (!current.add(type)) error("Circular dependency detected: $type")

        try {
            return this.initialize(type)
        } finally {
            current.remove(type)
        }
    }

    private fun initialize(requestedType: KType): Any {
        val kClass = requestedType.classifier as? KClass<*> ?: error("Cannot instantiate non-class type: $requestedType")

        if (!kClass.isResolvable()) error("Type is not instantiable: $requestedType")
        val constructor = this.selectConstructor(kClass) ?: error("No accessible constructor found for ${kClass.qualifiedName}")

        val substitution = this.typeSubstitutionMap(requestedType)

        val args = mutableMapOf<KParameter, Any?>()

        for (parameter in constructor.parameters) {
            if (parameter.kind != KParameter.Kind.VALUE) continue

            val wanted = this.substituteKType(parameter.type, substitution)
            val value = runCatching { this.resolve(wanted) }.getOrNull()

            if (value != null) {
                args[parameter] = value
                continue
            }

            if (!parameter.isOptional) {
                error("Cannot resolve required parameter ${parameter.name}: $wanted for ${kClass.qualifiedName}")
            }
        }

        return constructor.callBy(args)
    }

    private fun selectConstructor(kClass: KClass<*>): KFunction<Any>? {
        val all = kClass.constructors.toList()

        val injectConstructors = all.filter { it.hasInjectAnnotation() }
        if (injectConstructors.size > 1) error("Multiple @Inject constructors found for ${kClass.qualifiedName}")

        if (injectConstructors.size == 1) return injectConstructors.single()

        val primary = kClass.primaryConstructor
        if (primary != null) return primary

        return all.firstOrNull()
    }

    private fun KFunction<*>.hasInjectAnnotation(): Boolean {
        val names = this.annotations.mapNotNull { it.annotationClass.qualifiedName }.toSet()
        return "javax.inject.Inject" in names || "jakarta.inject.Inject" in names
    }

    private fun typeSubstitutionMap(requestedType: KType): Map<KTypeParameter, KTypeProjection> {
        val kClass = requestedType.classifier as? KClass<*> ?: return emptyMap()
        val params = kClass.typeParameters
        val args = requestedType.arguments
        if (params.isEmpty() || args.isEmpty()) return emptyMap()
        return params.zip(args).toMap()
    }

    private fun substituteKType(type: KType, mapping: Map<KTypeParameter, KTypeProjection>): KType {
        val classifier = type.classifier

        if (classifier is KTypeParameter) {
            val repl = mapping[classifier]?.type
            return repl ?: type
        }

        val kClass = classifier as? KClass<*> ?: return type
        if (type.arguments.isEmpty()) return type

        val newArgs = type.arguments.map { arg ->
            val inner = arg.type ?: return@map arg
            arg.copy(type = this.substituteKType(inner, mapping))
        }

        return kClass.createType(arguments = newArgs, nullable = type.isMarkedNullable)
    }

    private fun KClass<*>.isResolvable(): Boolean {
        if (this.java.isInterface) return false
        if (this.java.isPrimitive) return false
        if (this.java.isEnum) return false
        if (this.isSubclassOf(Annotation::class)) return false
        return this.constructors.isNotEmpty()
    }
}