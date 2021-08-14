package testing.extension

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

@ExperimentalCoroutinesApi
class CoroutineExtension(val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }
}

/**
 * Helper interface for tests using [CoroutineExtension]. Implementing this interface implements also [CoroutineExtension].
 */
@ExperimentalCoroutinesApi
abstract class CoroutineTest {

    @JvmField
    @RegisterExtension
    val coroutinesRule = CoroutineExtension()

    val testDispatcher = coroutinesRule.testDispatcher
}
