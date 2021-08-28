package api.decoration

import api.decorator.DecoratorConfig

/**
 * TODO add class description
 */
data class CustomStrategy(val actions: List<Action>) : Decoration.Strategy() {

    sealed interface Action {

        data class Remove(val providerId: Decoration.Provider.Id) : Action

        data class Replace(val oldProviderId: Decoration.Provider.Id, val newProvider: Decoration.Provider<*>) : Action

        data class Append(val provider: Decoration.Provider<*>) : Action
    }
}

// Extension to limit the scope where this can be used
@Suppress("unused")
fun DecoratorConfig<*>.customStrategy(block: CustomStrategyDefinition.() -> Unit): Decoration.Strategy {
    val actions = CustomStrategyDefinition().apply(block).actions
    return CustomStrategy(actions)
}

class CustomStrategyDefinition internal constructor()  {

    private val _actions = mutableListOf<CustomStrategy.Action>()
    val actions: List<CustomStrategy.Action> get() = _actions

    fun removeWithId(providerId: Decoration.Provider.Id) {
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
