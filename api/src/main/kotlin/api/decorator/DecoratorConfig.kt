package api.decorator

import api.annotation.DecoratorConfiguration
import api.decoration.Decoration

/**
 * Specifies configuration of the decorator which will be generated. Classes implementing this
 * interface have to provide instance of the decorated [Stub] together with the list
 * of [Decoration]s which will be used in the generated code to decorate [Stub]'s RPCs. Classes
 * implementing this interface needs to be also annotated with [DecoratorConfiguration].
 */
interface DecoratorConfig<Stub> {

    /**
     * Provides instance of the [Stub] which will be decorated by the generated decorator class
     */
    fun provideStub(): Stub

    /**
     * Provides a list of [Decoration]s used by the generated decorator class to decorate [Stub]'s
     * RPCs. Order of [Decoration]s in the list determines the order in which they are applied to the
     * RPCs. There is no size limit. List can be even theoretically empty and RPCs will be just called
     * without any applied [Decoration]s.
     */
    fun provideDecorations(): List<Decoration>
}
