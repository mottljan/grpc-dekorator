package api.annotation

import api.decorator.DecoratorConfig

///**
// * Marks class as a decorator configuration for a specific gRPC stub. Class annotated with
// * [DecoratorConfiguration] needs to implement [DecoratorConfig] interface. Annotated class serves
// * as provider of configuration for the generated decorator of the stub. For each annotated class
// * gRPC stub decorator is generated.
// */
// TODO docs
@Target(AnnotationTarget.CLASS)
annotation class GlobalDecoratorConfiguration
