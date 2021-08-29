package api.decoration

import api.decorator.DecoratorConfig
import api.decorator.GlobalDecoratorConfig

/**
 * [Decoration.Strategy] which enables to modify resulting list of [Decoration.Provider]s in a
 * very custom way. Clients can remove, replace or append to higher level [Decoration.Provider]
 * list. When used as a stub's [Decoration.Strategy], it modifies globally declared
 * [Decoration.Provider]s (if any). When used as a RPC's [Decoration.Strategy], it modifies stub's
 * [Decoration.Provider]s which were already resolved by applying stub's [Decoration.Strategy]
 * to the global [Decoration.Provider]s.
 */
data class CustomStrategy(val actions: List<Action>) : Decoration.Strategy() {

    /**
     * Specifies [Action] which should be taken to modify the higher level list of [Decoration.Provider]s
     */
    sealed interface Action {

        /**
         * Removes a specific [Decoration.Provider] from the higher level list identified by [providerId].
         * Exception is delivered to [GlobalDecoratorConfig.handleException] if [Decoration.Provider]
         * with [providerId] does not exist.
         */
        data class Remove(val providerId: Decoration.Provider.Id) : Action

        /**
         * Replaces a [Decoration.Provider] specified by [oldProviderId] in the higher level list
         * of [Decoration.Provider]s by [newProvider]. Exception is delivered to
         * [GlobalDecoratorConfig.handleException] if [Decoration.Provider] with [oldProviderId]
         * does not exist.
         */
        data class Replace(val oldProviderId: Decoration.Provider.Id, val newProvider: Decoration.Provider<*>) : Action

        /**
         * Appends the [provider] at the end of the higher level list of [Decoration.Provider]s
         */
        data class Append(val provider: Decoration.Provider<*>) : Action
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

    fun removeProviderWithId(providerId: Decoration.Provider.Id) {
        _actions += CustomStrategy.Action.Remove(providerId)
    }

    fun replace(providerId: Decoration.Provider.Id) = Replace(providerId)

    fun append(decorationProvider: Decoration.Provider<*>) {
        _actions += CustomStrategy.Action.Append(decorationProvider)
    }

    inner class Replace internal constructor(private val providerId: Decoration.Provider.Id) {

        infix fun with(decorationProvider: Decoration.Provider<*>) {
            _actions += CustomStrategy.Action.Replace(providerId, decorationProvider)
        }
    }
}
