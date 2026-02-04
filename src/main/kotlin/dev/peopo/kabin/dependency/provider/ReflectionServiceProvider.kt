package dev.peopo.kabin.dependency.provider

import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.kotlinFunction

internal class ReflectionServiceProvider(
    private val provider: ServiceProvider? = null
)  : ServiceProvider {
    constructor(): this(null)

    private val resolvingPath = ThreadLocal.withInitial { ArrayDeque<KType>() }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> provide(type: KType): T? = resolve(type) as? T

    private fun resolve(type: KType): Any = provider?.provide(type) ?: autoResolve(type)

    private fun autoResolve(type: KType): Any = withCycleGuard(type) { initialize(type) }

    private fun initialize(requestedType: KType): Any {
        val kClass = requestedType.asKClassOrFail()

        if (!kClass.isResolvable()) error("Type is not instantiable: $requestedType")

        val constructor = selectConstructor(kClass) ?: error("No accessible constructor found for ${kClass.qualifiedName}")
        constructor.isAccessible = true

        val substitution = typeSubstitutionMap(requestedType)

        val args = buildArgs(constructor, substitution, owner = kClass)

        return constructor.callBy(args)
    }

    private fun buildArgs(
        constructor: KFunction<Any>,
        substitution: Map<KTypeParameter, KTypeProjection>,
        owner: KClass<*>
    ): Map<KParameter, Any?> {
        val args = LinkedHashMap<KParameter, Any?>()

        for (parameter in constructor.parameters) {
            if (parameter.kind != KParameter.Kind.VALUE) continue

            val wanted = substituteKType(parameter.type, substitution)
            val value = resolveValueForParameter(wanted)

            when {
                value != null -> args[parameter] = value
                parameter.type.isMarkedNullable -> args[parameter] = null
                parameter.isOptional -> Unit
                else -> error("Cannot resolve required parameter ${parameter.name}: $wanted for ${owner.qualifiedName}")
            }
        }

        return args
    }

    private fun resolveValueForParameter(wanted: KType): Any? {
        provider?.provide<Any>(wanted)?.let { return it }

        if (!wanted.shouldAutoResolve()) return null

        return runCatching { resolve(wanted) }.getOrNull()
    }

    private fun selectConstructor(kClass: KClass<*>): KFunction<Any>? {
        val constructors = this.getConstructors(kClass)

        val injectable = constructors.filter { it.hasInjectAnnotation() }
        if (injectable.size > 1) error("Multiple @Inject constructors found for ${kClass.qualifiedName}")
        if (injectable.size == 1) return injectable.single()

        kClass.primaryConstructor?.let { return it }

        val publicConstructors = kClass.constructors
        if (publicConstructors.size > 1) return publicConstructors.first()

        return constructors.firstOrNull()
    }

    private fun getConstructors(kClass: KClass<*>): List<KFunction<Any>> {
        val declared = kClass.java.declaredConstructors.mapNotNull { it.kotlinFunction }
        val public = kClass.constructors

        return (declared + public)
            .distinctBy { fn ->
                fn.parameters
                    .filter { it.kind == KParameter.Kind.VALUE }
                    .map { it.type }               // avoid toString() where possible
                    .toList()
            }
    }

    private fun withCycleGuard(type: KType, block: () -> Any): Any {
        val path = resolvingPath.get()
        if (path.contains(type)) {
            val chain = (path + type).joinToString(" -> ")
            error("Circular dependency detected: $chain")
        }

        path.addLast(type)
        try {
            return block()
        } finally {
            path.removeLast()
        }
    }

    private fun KType.asKClassOrFail(): KClass<*> =
        (classifier as? KClass<*>) ?: error("Cannot instantiate non-class type: $this")
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

        if (classifier is KTypeParameter) return mapping[classifier]?.type ?: type

        val kClass = classifier as? KClass<*> ?: return type
        if (type.arguments.isEmpty()) return type

        val newArgs = type.arguments.map { arg ->
            val inner = arg.type ?: return@map arg
            arg.copy(type = this.substituteKType(inner, mapping))
        }

        return kClass.createType(arguments = newArgs, nullable = type.isMarkedNullable)
    }

    private fun KClass<*>.isResolvable(): Boolean {
        if (java.isInterface) return false
        if (java.isPrimitive) return false
        if (java.isEnum) return false
        if (isSubclassOf(Annotation::class)) return false
        return constructors.isNotEmpty() || java.declaredConstructors.isNotEmpty()
    }

    private fun KType.shouldAutoResolve(): Boolean {
        val kClass = classifier as? KClass<*> ?: return false
        val qn = kClass.qualifiedName ?: return false
        if (qn.startsWith("kotlin.")) return false
        if (qn.startsWith("java.")) return false
        return kClass.isResolvable()
    }
}