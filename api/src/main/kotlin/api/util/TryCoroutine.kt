package api.util

import kotlinx.coroutines.CancellationException

/**
 * tryCoroutine/catch DSL enables to try/catch the exceptions thrown from the suspend functions
 * exactly the same like by using standard try/catch blocks with the difference that it does not
 * catch [CancellationException] and automatically rethrows it to the caller. Same as try/catch, it
 * can serve either as a statement or an expression.
 */
// suspend is intentional to force usage only in coroutines since normal functions should use classic try/catch
internal suspend inline fun <T> tryCoroutine(tryBlock: () -> T): TryCoroutineResult<T> {
    return try {
        TryCoroutineResult.Success(tryBlock())
    } catch (e: Throwable) {
        TryCoroutineResult.Error(e)
    }
}

internal sealed class TryCoroutineResult<out T> {

    data class Success<T>(val result: T) : TryCoroutineResult<T>()

    data class Error<T>(val throwable: Throwable) : TryCoroutineResult<T>()
}

internal inline infix fun <T> TryCoroutineResult<T>.catch(catchBlock: (Throwable) -> T): T = when (this) {
    is TryCoroutineResult.Success -> result
    is TryCoroutineResult.Error -> {
        if (throwable is CancellationException) throw throwable else catchBlock(throwable)
    }
}
