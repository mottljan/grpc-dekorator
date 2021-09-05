package api.decorator

import api.annotation.GlobalDecoratorConfiguration
import api.decoration.Decoration

/**
 * Specifies global decorator configuration which is shared between all generated stub's decorators.
 * Class implementing this interface needs to be also annotated with [GlobalDecoratorConfiguration].
 * Global configuration can be useful for providing a common shared configuration between all generated
 * decorators. It can provide a common list of [Decoration]s or enable to handle thrown exceptions
 * due to some invalid config.
 */
interface GlobalDecoratorConfig {

    /**
     * Declares a list of [Decoration]s common to all decorators
     */
    val decorations: List<Decoration> get() = emptyList()

    /**
     * Can be overridden to provide a unified way of handling possible runtime exceptions which
     * happened due to an invalid decorators configuration like trying to remove non-existing
     * [Decoration] while applying a [Decoration.Strategy].
     *
     * This method throws the [exception] by default and if the [GlobalDecoratorConfig] does not
     * exist, all exceptions are also always thrown when they can't be delivered to this method.
     */
    fun handleException(exception: Exception) {
        throw exception
    }
}
