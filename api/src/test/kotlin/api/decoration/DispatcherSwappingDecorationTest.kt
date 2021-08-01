package api.decoration

import api.extension.CoroutineTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
internal class DispatcherSwappingDecorationTest : CoroutineTest() {

    private val swappingDispatcher = Dispatchers.Main
    private val underTest = DispatcherSwappingDecoration(swappingDispatcher)

    @Test
    @ExperimentalStdlibApi
    fun `decoration swaps current dispatcher with the provided one for suspend fun`() = testDispatcher.runBlockingTest {
        coroutineContext[CoroutineDispatcher.Key] `should be instance of` TestCoroutineDispatcher::class

        underTest.decorate {
            coroutineScope {
                coroutineContext[CoroutineDispatcher.Key] `should be` swappingDispatcher
            }
        }

        coroutineContext[CoroutineDispatcher.Key] `should be instance of` TestCoroutineDispatcher::class
    }

    @Test
    @ExperimentalStdlibApi
    fun `decoration swaps current dispatcher with the provided one for stream`() = testDispatcher.runBlockingTest {
        coroutineContext[CoroutineDispatcher.Key] `should be instance of` TestCoroutineDispatcher::class

        underTest.decorateStream {
            flowOf(Unit).onCompletion {
                coroutineScope {
                    coroutineContext[CoroutineDispatcher.Key] `should be` swappingDispatcher
                }
            }
        }.collect {
            coroutineScope {
                coroutineContext[CoroutineDispatcher.Key] `should be instance of` TestCoroutineDispatcher::class
            }
        }
    }
}
