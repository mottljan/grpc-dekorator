package api.decorator

import api.decoration.Decoration

/**
 * TODO add class description
 */
interface GlobalDecoratorConfig {

    val decorationProviders: List<Decoration.Provider<*>>
}
