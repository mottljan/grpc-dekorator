package api.decorator

import api.annotation.GlobalDecoratorConfiguration
import api.decoration.Decoration

/**
 * Specifies global decorator configuration which is shared between all generated stub's decorators.
 * Class implementing this interface needs to be also annotated with [GlobalDecoratorConfiguration].
 * Global configuration can be useful for providing a common shared configuration between all generated
 * decorators. It can provide a common list of [Decoration.Provider]s or enable to handle thrown
 * exceptions due to some invalid config.
 */
abstract class GlobalDecoratorConfig {

    /**
     * Declares a list of [Decoration.Provider]s common to all decorators.
     *
     * It is mandatory that this property has to have a backing field. It is required for the
     * correct usage of [Decoration.Provider]s and their specified [Decoration.InitStrategy].
     * Having a backing field forces to create a list of [Decoration.Provider]s just once and not
     * for every call of the getter, thus allowing [Decoration.Provider]s to apply
     * [Decoration.InitStrategy] correctly and as expected (i.e. create a [Decoration] really as a
     * singleton).
     *
     * Note that depending on your usage of [GlobalDecoratorConfig] you can further influence how
     * [Decoration.InitStrategy] works. If you create a singleton of your [GlobalDecoratorConfig]
     * implementation and reuse it for all generated decorators, [Decoration] instances specified
     * with [Decoration.InitStrategy.SINGLETON] will be really created as singletons for the whole
     * app. However creating a unique instance of [GlobalDecoratorConfig] per decorator will lead
     * to creation of a new instance of a specific [Decoration.Provider] per decorator, thus leading
     * to a "singleton" per stub.
     */
    open val decorationProviders: List<Decoration.Provider<*>> = emptyList()

    /**
     * Can be overridden to provide a unified way of handling possible runtime exceptions which
     * happened due to an invalid decorators configuration like trying to remove non-existing
     * [Decoration.Provider] while applying a [Decoration.Strategy].
     *
     * This method throws the [exception] by default and if the [GlobalDecoratorConfig] does not
     * exist, all exceptions are also always thrown when they can't be delivered to this method.
     */
    open fun handleException(exception: Exception) {
        throw exception
    }
}
