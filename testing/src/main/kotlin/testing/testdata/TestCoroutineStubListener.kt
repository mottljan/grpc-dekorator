package testing.testdata

interface TestCoroutineStubListener {

    fun onRpcCalled(request: String, result: String)

    fun onStreamingRpcCalled(request: String, result: List<String>)
}
