package api.decoration

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * [Decoration] of RPCs swapping current [CoroutineDispatcher] with the provided [dispatcher]
 */
class DispatcherSwappingDecoration(private val dispatcher: CoroutineDispatcher) : Decoration {

    override val id = ID

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
        return withContext(dispatcher) { rpc() }
    }

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
        return rpc().flowOn(dispatcher)
    }

    companion object {

        val ID = Decoration.Id(DispatcherSwappingDecoration::class.qualifiedName!!)
    }
}
