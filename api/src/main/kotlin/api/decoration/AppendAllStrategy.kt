package api.decoration

import api.decorator.DecoratorConfig

/**
 * TODO add class description
 */
data class AppendAllStrategy(val providers: List<Decoration.Provider<*>>) : Decoration.Strategy()

// Extension to limit the scope where this can be used
@Suppress("unused")
fun DecoratorConfig<*>.appendAllStrategy(block: AppendAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorationProviders = AppendAllStrategyDefinition().apply(block).decorationProviders
    return AppendAllStrategy(decorationProviders)
}

class AppendAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun append(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}
