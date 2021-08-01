package api.decoration

import api.extension.CoroutineTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesApi
internal class ExceptionMappingDecorationTest : CoroutineTest() {

    private val underTest = ExceptionMappingDecoration(ExceptionMapperImpl())

    @Test
    fun `decoration returns result successfully when no exception is thrown for suspend fun`() = testDispatcher.runBlockingTest {
        val expected = 1

        val actual = underTest.decorate { expected }

        actual `should be equal to` expected
    }

    @Test
    fun `decoration throws mapped exception when exception thrown for suspend fun`() = testDispatcher.runBlockingTest {
        assertThrows<CustomTestException> {
            underTest.decorate { throw IllegalStateException() }
        }
    }

    @Test
    fun `decoration rethrows CancellationException when it is thrown for suspend fun`() = testDispatcher.runBlockingTest {
        assertThrows<CancellationException> {
            underTest.decorate { throw CancellationException() }
        }
    }

    @Test
    fun `decoration emits result successfully when no exception is thrown for stream`() = testDispatcher.runBlockingTest {
        val expected = 1

        val actual = underTest.decorateStream { flowOf(expected) }.first()

        actual `should be equal to` expected
    }

    @Test
    fun `decoration throws mapped exception when exception thrown for stream`() = testDispatcher.runBlockingTest {
        assertThrows<CustomTestException> {
            underTest.decorateStream<Unit> {
                flow { throw IllegalStateException() }
            }.first()
        }
    }

    @Test
    fun `decoration rethrows CancellationException when the stream collection is cancelled`() = testDispatcher.runBlockingTest {
        assertThrows<CancellationException> {
            coroutineScope {
                underTest.decorateStream {
                    flow {
                        emit(Unit)
                        delay(Long.MAX_VALUE)
                    }
                }.collect {
                    cancel()
                }
            }
        }
    }
}

private class ExceptionMapperImpl : ExceptionMapper {

    override fun mapException(throwable: Throwable) = CustomTestException()
}

private class CustomTestException : Exception()
