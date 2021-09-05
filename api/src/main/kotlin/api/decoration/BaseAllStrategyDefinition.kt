package api.decoration

/**
 * Base class for "all" based strategies like [AppendAllStrategy]
 */
abstract class BaseAllStrategyDefinition internal constructor() {

    @Suppress("PropertyName")
    protected val _decorations = mutableListOf<Decoration>()
    val decorations: List<Decoration> get() = _decorations
}
