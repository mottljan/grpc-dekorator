package api.decoration

import api.decorator.DecoratorConfig

/**
 * [Decoration.Strategy] which replaces all higher level [Decoration.Provider]s with [providers].
 * It means for stub it replaces all globally defined providers (if any) and for particular RPC it
 * replaces all stub's providers (which also already include the global ones resolved based on the
 * stub's [Decoration.Strategy]).
 */
data class ReplaceAllStrategy(val providers: List<Decoration.Provider<*>>) : Decoration.Strategy()

/**
 * DSL for creating [ReplaceAllStrategy]
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.replaceAllStrategy(block: ReplaceAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorationProviders = ReplaceAllStrategyDefinition().apply(block).decorationProviders
    return ReplaceAllStrategy(decorationProviders)
}

class ReplaceAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun replaceWith(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}
