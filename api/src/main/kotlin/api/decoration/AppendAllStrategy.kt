package api.decoration

import api.decorator.DecoratorConfig

/**
 * [Decoration.Strategy] which appends all [decorations] to all higher level [Decoration]s.
 * It means for stub it appends all [decorations] at the end of the globally defined decorations (if any)
 * and for particular RPC it appends all [decorations] at the end of all stub's decorations (which also
 * already include the global ones resolved based on the stub's [Decoration.Strategy]).
 */
data class AppendAllStrategy(val decorations: List<Decoration>) : Decoration.Strategy()

/**
 * DSL for creating [AppendAllStrategy]
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.appendAllStrategy(block: AppendAllStrategyDefinition.() -> Unit): Decoration.Strategy {
    val decorations = AppendAllStrategyDefinition().apply(block).decorations
    return AppendAllStrategy(decorations)
}

class AppendAllStrategyDefinition internal constructor() : BaseAllStrategyDefinition() {

    fun append(decoration: Decoration) {
        _decorations += decoration
    }
}
