package api.internal.decorator

import api.decoration.Decoration
import kotlinx.coroutines.flow.Flow

/**
 * Common base class for all generated stub decorators containing helper methods for applying
 * [Decoration]s. This class is intended to be used ONLY by generated decorators and because of that
 * it can not be internal and must be public.
 */
@Deprecated("Should be used only by generated decorators!", level = DeprecationLevel.HIDDEN)
abstract class CoroutineStubDecorator {

    protected suspend fun <Resp> Iterator<Decoration>.applyNextDecorationOrCallRpc(callRpc: suspend () -> Resp): Resp {
        return if (hasNext()) {
            next().decorate { applyNextDecorationOrCallRpc(callRpc) }
        } else {
            callRpc()
        }
    }

    protected fun <Resp> Iterator<Decoration>.applyNextDecorationOrCallStreamRpc(callRpc: () -> Flow<Resp>): Flow<Resp> {
        return if (hasNext()) {
            next().decorateStream { applyNextDecorationOrCallStreamRpc(callRpc) }
        } else {
            callRpc()
        }
    }
}
