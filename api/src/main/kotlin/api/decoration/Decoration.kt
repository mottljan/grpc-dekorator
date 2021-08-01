package api.decoration

import kotlinx.coroutines.flow.Flow

/**
 * Decoration of the RPCs of the gRPC stubs. [Decoration] instances are used by the generated
 * stub decorator.
 */
interface Decoration {

    /**
     * Decorates suspend [rpc]. It must call [rpc] and return [Response] to the caller.
     */
    suspend fun <Response> decorate(rpc: suspend () -> Response): Response

    /**
     * Decorates streaming [rpc] returning [Flow].
     * It must call [rpc] and return [Flow] to the caller.
     */
    fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response>
}
