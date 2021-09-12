package io.github.mottljan.api.annotation

import io.github.mottljan.api.decoration.Decoration

/**
 * [RpcConfiguration] tells the annotation processor to generate a special [Decoration.Strategy]
 * for the particular RPC identified by the provided [rpcName]. Annotated method must be declared
 * inside the class annotated with [DecoratorConfiguration] and decorated RPC needs to be a part
 * of the stub which is decorated by the corresponding generated decorator. Decorated method must
 * have 0 arguments and return [Decoration.Strategy].
 */
@Target(AnnotationTarget.FUNCTION)
annotation class RpcConfiguration(val rpcName: String)
