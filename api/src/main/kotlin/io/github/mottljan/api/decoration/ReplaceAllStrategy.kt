package io.github.mottljan.api.decoration

import io.github.mottljan.api.decorator.DecoratorConfig

/**
 * [Decoration.Strategy] which replaces all higher level [Decoration]s with [decorations].
 * It means for stub it replaces all globally defined decorations (if any) and for particular RPC it
 * replaces all stub's decorations (which also already include the global ones resolved based on the
 * stub's [Decoration.Strategy]).
 */
data class ReplaceAllStrategy(val decorations: List<Decoration>) : Decoration.Strategy()

/**
 * DSL for creating [ReplaceAllStrategy]
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.replaceAllStrategy(block: ReplaceAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorations = ReplaceAllStrategyDefinition().apply(block).decorations
    return ReplaceAllStrategy(decorations)
}

class ReplaceAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun replaceWith(decoration: Decoration) {
        _decorations += decoration
    }
}
