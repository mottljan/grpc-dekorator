package api.decoration

import api.decorator.DecoratorConfig
import api.decorator.GlobalDecoratorConfig

/**
 * [Decoration.Strategy] which enables to modify resulting list of [Decoration]s in a very custom
 * way. Clients can remove, replace or append to higher level [Decoration] list. When used as
 * a stub's [Decoration.Strategy], it modifies globally declared [Decoration]s (if any). When used
 * as a RPC's [Decoration], it modifies stub's [Decoration]s which were already resolved by applying
 * stub's [Decoration.Strategy] to the global [Decoration]s.
 */
data class CustomStrategy(val actions: List<Action>) : Decoration.Strategy() {

    /**
     * Specifies [Action] which should be taken to modify the higher level list of [Decoration]s
     */
    sealed interface Action {

        /**
         * Removes a specific [Decoration] from the higher level list identified by [decorationId].
         * Exception is delivered to [GlobalDecoratorConfig.handleException] if [Decoration] with
         * [decorationId] does not exist.
         */
        data class Remove(val decorationId: Decoration.Id) : Action

        /**
         * Replaces a [Decoration] specified by [oldDecorationId] in the higher level list
         * of [Decoration]s by [newDecoration]. Exception is delivered to
         * [GlobalDecoratorConfig.handleException] if [Decoration] with [oldDecorationId] does not
         * exist.
         */
        data class Replace(val oldDecorationId: Decoration.Id, val newDecoration: Decoration) : Action

        /**
         * Appends the [decoration] at the end of the higher level list of [Decoration]s
         */
        data class Append(val decoration: Decoration) : Action
    }
}

/**
 * DSL for creating [CustomStrategy]
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.customStrategy(block: CustomStrategyDefinition.() -> Unit): Decoration.Strategy {
    val actions = CustomStrategyDefinition().apply(block).actions
    return CustomStrategy(actions)
}

class CustomStrategyDefinition internal constructor()  {

    private val _actions = mutableListOf<CustomStrategy.Action>()
    val actions: List<CustomStrategy.Action> get() = _actions

    fun removeDecorationWithId(decorationId: Decoration.Id) {
        _actions += CustomStrategy.Action.Remove(decorationId)
    }

    fun replace(decorationId: Decoration.Id) = Replace(decorationId)

    fun append(decoration: Decoration) {
        _actions += CustomStrategy.Action.Append(decoration)
    }

    inner class Replace internal constructor(private val decorationId: Decoration.Id) {

        infix fun with(decoration: Decoration) {
            _actions += CustomStrategy.Action.Replace(decorationId, decoration)
        }
    }
}
