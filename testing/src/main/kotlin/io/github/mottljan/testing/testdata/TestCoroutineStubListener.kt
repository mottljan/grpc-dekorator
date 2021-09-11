package io.github.mottljan.testing.testdata

/**
 * Listener which can be installed fo the stub under test to be able to listen for calls.
 */
interface TestCoroutineStubListener {

    fun onRpcCalled(request: String, result: String)

    fun onStreamingRpcCalled(request: String, result: List<String>)
}
