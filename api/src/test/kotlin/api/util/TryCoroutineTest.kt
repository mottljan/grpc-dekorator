package api.util

import api.extension.CoroutineTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesApi
class TryCoroutineTest : CoroutineTest() {

    @Test
    fun `tryCoroutine-catch assigns result from try when successful`() = testDispatcher.runBlockingTest {
        val expected = "result"
        var actual = ""

        tryCoroutine { actual = expected } catch {}

        actual `should be equal to` expected
    }

    @Test
    fun `tryCoroutine-catch catches thrown exception and assigns it`() = testDispatcher.runBlockingTest {
        val expected = Exception("tryCoroutine-catch")
        var actual: Throwable? = null

        tryCoroutine { throw expected } catch { actual = it }

        actual `should be` expected
    }

    @Test
    fun `tryCoroutine-catch doesn't catch cancellation exception and rethrows it`() = testDispatcher.runBlockingTest {
        var actual: Throwable? = null

        assertThrows<CancellationException> {
            tryCoroutine { throw CancellationException() } catch { actual = it }
        }
        actual `should be` null
    }

    @Test
    fun `tryCoroutine-catch returns result from try when successful`() = testDispatcher.runBlockingTest {
        val expected = "result"

        val actual = tryCoroutine { expected } catch { "fallback" }

        actual `should be equal to` expected
    }

    @Test
    fun `tryCoroutine-catch returns result from catch when exception is thrown`() = testDispatcher.runBlockingTest {
        val expected = "result"

        val actual = tryCoroutine { throw IllegalStateException() } catch { expected }

        actual `should be equal to` expected
    }

    @Test
    fun `tryCoroutine-catch rethrows exception rethrown from catch`() = testDispatcher.runBlockingTest {
        assertThrows<IllegalStateException> {
            tryCoroutine { throw IllegalStateException() } catch { throw it }
        }
    }

    @Test
    fun `tryCoroutine-catch can run suspend fun in tryCoroutine block`() = testDispatcher.runBlockingTest {
        val expected = "result"

        val actual = tryCoroutine { longSuspendingFun(expected) } catch { "fallback" }

        actual `should be equal to` expected
    }

    private suspend fun longSuspendingFun(returnValue: String): String {
        delay(1)
        return returnValue
    }

    @Test
    fun `tryCoroutine-catch can run suspend fun in catch block`() = testDispatcher.runBlockingTest {
        val expected = "result"

        val actual = tryCoroutine { throw IllegalStateException() } catch { longSuspendingFun(expected) }

        actual `should be equal to` expected
    }
}
