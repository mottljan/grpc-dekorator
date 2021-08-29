package api.decoration

/**
 * Base class for "all" based strategies like [AppendAllStrategy]
 */
abstract class BaseAllStrategyDefinition internal constructor() {

    @Suppress("PropertyName")
    protected val _decorationProviders = mutableListOf<Decoration.Provider<*>>()
    val decorationProviders: List<Decoration.Provider<*>> get() = _decorationProviders
}
