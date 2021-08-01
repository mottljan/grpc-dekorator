package api.internal.decorator

import api.decoration.Decoration
import api.extension.CoroutineExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
@Suppress("DEPRECATION_ERROR")
internal class CoroutineStubDecoratorTest : CoroutineStubDecorator() {

    @JvmField
    @RegisterExtension
    val coroutinesRule = CoroutineExtension()
    private val testDispatcher get() = coroutinesRule.testDispatcher

    private val decorationsCallOrder = mutableListOf<KClass<out Decoration>>()

    @Test
    fun `all decorations are called and in the correct order (A, B) for suspend fun`() {
        testAllDecorationsOrderForSuspendFun(
            decorations = listOf(DecorationA(), DecorationB()),
            expectedOrder = listOf(DecorationA::class, DecorationB::class)
        )
    }

    private fun testAllDecorationsOrderForSuspendFun(
        decorations: List<Decoration>,
        expectedOrder: List<KClass<out Decoration>>
    ) = testDispatcher.runBlockingTest {
        val expectedResult = true

        val actualResult = decorations.iterator().applyNextDecorationOrCallRpc { expectedResult }

        decorationsCallOrder `should be equal to` expectedOrder
        actualResult `should be equal to` expectedResult
    }

    @Test
    fun `all decorations are called and in the correct order (B, A) for suspend fun`() {
        testAllDecorationsOrderForSuspendFun(
            decorations = listOf(DecorationB(), DecorationA()),
            expectedOrder = listOf(DecorationB::class, DecorationA::class)
        )
    }

    @Test
    fun `no decorations are called for suspend fun and rpc is called`() {
        testAllDecorationsOrderForSuspendFun(
            decorations = emptyList(),
            expectedOrder = emptyList()
        )
    }

    @Test
    fun `all decorations are called and in the correct order (A, B) for stream`() {
        testAllDecorationsOrderForStream(
            decorations = listOf(DecorationA(), DecorationB()),
            expectedOrder = listOf(DecorationA::class, DecorationB::class)
        )
    }

    private fun testAllDecorationsOrderForStream(
        decorations: List<Decoration>,
        expectedOrder: List<KClass<out Decoration>>
    ) = testDispatcher.runBlockingTest {
        val expectedResult = true

        val actualResult = decorations.iterator()
            .applyNextDecorationOrCallStreamRpc { flowOf(expectedResult) }
            .first()

        decorationsCallOrder `should be equal to` expectedOrder
        actualResult `should be equal to` expectedResult
    }

    @Test
    fun `all decorations are called and in the correct order (B, A) for stream`() {
        testAllDecorationsOrderForStream(
            decorations = listOf(DecorationB(), DecorationA()),
            expectedOrder = listOf(DecorationB::class, DecorationA::class)
        )
    }

    @Test
    fun `no decorations are called for stream and rpc is called`() {
        testAllDecorationsOrderForStream(
            decorations = emptyList(),
            expectedOrder = emptyList()
        )
    }

    private inner class DecorationA : Decoration {

        override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
            decorationsCallOrder += this::class
            return rpc()
        }

        override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
            decorationsCallOrder += this::class
            return rpc()
        }
    }

    private inner class DecorationB : Decoration {

        override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
            decorationsCallOrder += this::class
            return rpc()
        }

        override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
            decorationsCallOrder += this::class
            return rpc()
        }
    }
}
