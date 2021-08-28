package api.decoration

import api.decoration.Decoration

/**
 * TODO add class description
 */
abstract class BaseAllStrategyDefinition internal constructor() {

    protected val _decorationProviders = mutableListOf<Decoration.Provider<*>>()
    val decorationProviders: List<Decoration.Provider<*>> get() = _decorationProviders
}
