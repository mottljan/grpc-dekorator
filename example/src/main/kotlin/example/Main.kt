package example

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import stub.decorator.wtf.FakeCoroutineStubDecorator

fun main() {
    runBlocking {
        val decorator = FakeCoroutineStubDecorator(FakeCoroutineStubDecoratorConfig())
        decorator.rpcWithoutArgs()
        println()
        decorator.streamingRpcWithoutArgs().collect {
            println("collecting $it")
        }
    }
}
