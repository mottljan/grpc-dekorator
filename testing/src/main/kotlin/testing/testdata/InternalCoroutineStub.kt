package testing.testdata

import api.annotation.DecoratorConfiguration
import api.decorator.DecoratorConfig

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on visibility
 * modifier testing. This class should be changed with caution since it can break tests.
 */
@Suppress("UnusedPrivateMember")
internal class InternalCoroutineStub

/**
 * [DecoratorConfig] used for testing purposes. It is focused on visibility modifier testing.
 * This class should be changed with caution since it can break tests.
 */
@DecoratorConfiguration
internal class InternalCoroutineStubDecoratorConfig : DecoratorConfig<InternalCoroutineStub> {

    override fun getStub(): InternalCoroutineStub {
        return InternalCoroutineStub()
    }
}
