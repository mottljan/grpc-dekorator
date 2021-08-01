package example

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("UnusedPrivateMember")
class FakeCoroutineStub {

    suspend fun rpc(request: String): String {
        println("rpc called")
        return ""
    }

    suspend fun rpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ): Boolean {
        println("rpcWithMultipleArgs called")
        return false
    }

    suspend fun rpcWithoutArgs(): Int {
        println("rpcWithoutArgs called")
        return 0
    }

    suspend fun rpcReturningUnit(request: String) {
        println("rpcReturningUnit called")
    }

    fun notSupportedFunForDecoration() {
        println("notSupportedFunForDecoration called")
    }

    fun streamingRpc(request: String): Flow<String> {
        println("streamingRpc called")
        return flowOf("first")
    }

    fun streamingRpcWithoutArgs(): Flow<Int> {
        println("streamingRpcWithoutArgs called")
        return flowOf(0, 1, 2)
    }

    fun streamingRpcWithMultipleArgs(
        firstArg: String,
        secondArg: Int,
        thirdArg: Boolean,
        fourthArg: Unit
    ): Flow<Unit> {
        println("streamingRpcWithMultipleArgs called")
        return flowOf(Unit)
    }
}
