package io.github.mottljan.api.decoration

import io.github.mottljan.api.util.catch
import io.github.mottljan.api.util.tryCoroutine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * [Decoration] mapping exceptions thrown from RPCs using provided [exceptionMapper]. If the running
 * RPC is cancelled, [CancellationException] is not propagated to the [exceptionMapper] and is
 * immediately thrown to the client.
 */
class ExceptionMappingDecoration(private val exceptionMapper: ExceptionMapper) : Decoration {

    override val id = ID

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

    companion object {

        val ID = Decoration.Id(ExceptionMappingDecoration::class.qualifiedName!!)
    }
}

/**
 * Maps exceptions thrown from RPCs
 */
interface ExceptionMapper {

    fun mapException(throwable: Throwable): Throwable
}
