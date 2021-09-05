package api.decoration

import api.decorator.DecoratorConfig
import kotlinx.coroutines.flow.Flow
import java.lang.IllegalStateException

/**
 * Decoration of the RPCs of the gRPC stubs. [Decoration] instances are used by the generated
 * stub decorator (see [DecoratorConfig]). It enables to specify an additional functionality to the
 * RPC like logging, exception mapping, etc. and reuse this easily across multiple RPCs and stubs.
 */
interface Decoration {

    val id: Id

    /**
     * Decorates suspend [rpc]. It must call [rpc] and return [Response] to the caller.
     */
    suspend fun <Response> decorate(rpc: suspend () -> Response): Response

    /**
     * Decorates streaming [rpc] returning [Flow].
     * It must call [rpc] and return [Flow] to the caller.
     */
    fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response>

    /**
     * [Id] of the [Decoration] which has to be unique to a specific [Decoration] subclass and not
     * an instance. Proper [Id]s are important for applying a [Decoration.Strategy] correctly.
     */
    data class Id(val value: String)

    /**
     * [Strategy] for resolving a final list of [Decoration]s to be applied to RPCs. It can
     * be used in two contexts: whole stub or particular RPC. [Strategy] enables to specify how the
     * unique list of [Decoration]s of stub or RPC should be "merged" together with the
     * higher level one list (either global [Decoration]s or stub ones based on the context).
     */
    sealed class Strategy
}

/**
 * Creates a [Decoration.Strategy] which doesn't change a higher level list of [Decoration]s
 * in any way.
 */
@Suppress("unused") // Extension to limit the scope where this can be used
fun DecoratorConfig<*>.noChangesStrategy(): Decoration.Strategy {
    return appendAllStrategy {}
}
