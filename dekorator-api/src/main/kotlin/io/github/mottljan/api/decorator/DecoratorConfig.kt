package io.github.mottljan.api.decorator

import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.decoration.Decoration
import io.github.mottljan.api.decoration.noChangesStrategy

/**
 * Specifies configuration of the decorator which will be generated. Classes implementing this
 * interface need to be also annotated with [DecoratorConfiguration].
 *
 * Classes implementing this interface have to provide instance of the decorated [Stub] and
 * optionally a [Decoration.Strategy] to be used for "merging" stub's specific [Decoration]s
 * with the globally defined ones. The resulting list of [Decoration]s is then used for decoration
 * of all stub's RPCs unless particular RPCs do not provide their own specific [Decoration.Strategy].
 */
interface DecoratorConfig<Stub> {

    /**
     * Returns instance of the [Stub] which will be decorated by the generated decorator class
     */
    fun getStub(): Stub

    /**
     * Returns stub's [Decoration.Strategy]. By default it does not change globally defined
     * [Decoration]s in any way. More info in [DecoratorConfig] docs.
     */
    fun getStubDecorationStrategy(): Decoration.Strategy = noChangesStrategy()
}
