package api.decoration

import api.decorator.DecoratorConfig

/**
 * TODO add class description
 */
data class ReplaceAllStrategy(val providers: List<Decoration.Provider<*>>) : Decoration.Strategy()

// Extension to limit the scope where this can be used
@Suppress("unused")
fun DecoratorConfig<*>.replaceAllStrategy(block: ReplaceAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorationProviders = ReplaceAllStrategyDefinition().apply(block).decorationProviders
    return ReplaceAllStrategy(decorationProviders)
}

class ReplaceAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun replaceWith(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}
