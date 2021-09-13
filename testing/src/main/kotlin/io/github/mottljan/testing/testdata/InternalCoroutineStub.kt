package io.github.mottljan.testing.testdata

import io.github.mottljan.api.annotation.DecoratorConfiguration
import io.github.mottljan.api.decorator.DecoratorConfig

/**
 * "Stub" class used for generating decorator for testing purposes. It is focused on visibility
 * modifier testing. This class should be changed with caution since it can break tests.
 */
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
