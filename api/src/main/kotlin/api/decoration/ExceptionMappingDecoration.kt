package api.decoration

import api.util.catch
import api.util.tryCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * [Decoration] mapping exceptions thrown from RPCs using provided [exceptionMapper]. If the running
 * RPC is cancelled, [CancellationException] is not propagated to the [exceptionMapper] and is
 * immediately thrown to the client.
 */
class ExceptionMappingDecoration(private val exceptionMapper: ExceptionMapper) : Decoration {

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
        return tryCoroutine {
            rpc()
        } catch {
            throw exceptionMapper.mapException(it)
        }
    }
    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
        return rpc().catch { throw exceptionMapper.mapException(it) }
    }
}

/**
 * Maps exceptions thrown from RPCs
 */
interface ExceptionMapper {

    fun mapException(throwable: Throwable): Throwable
}
