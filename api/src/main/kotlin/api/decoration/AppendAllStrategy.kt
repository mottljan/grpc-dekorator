package api.decoration

import api.decorator.DecoratorConfig

/**
 * [Decoration.Strategy] which appends all [providers] to all higher level [Decoration.Provider]s.
 * It means for stub it appends all [providers] at the end of the globally defined providers (if any)
 * and for particular RPC it appends all [providers] at the end of all stub's providers (which also
 * already include the global ones resolved based on the stub's [Decoration.Strategy]).
 */
data class AppendAllStrategy(val providers: List<Decoration.Provider<*>>) : Decoration.Strategy()

/**
 * DSL for creating [AppendAllStrategy]
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.appendAllStrategy(block: AppendAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorationProviders = AppendAllStrategyDefinition().apply(block).decorationProviders
    return AppendAllStrategy(decorationProviders)
}

class AppendAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun append(decorationProvider: Decoration.Provider<*>) {
        _decorationProviders += decorationProvider
    }
}
