package api.annotation

import api.decorator.DecoratorConfig

/**
 * Marks class as a decorator configuration for a specific gRPC stub. Class annotated with
 * [DecoratorConfiguration] needs to implement [DecoratorConfig] interface. Annotated class serves
 * as a provider of the configuration for the generated decorator of the stub. For each annotated class
 * gRPC stub decorator is generated.
 */
@Target(AnnotationTarget.CLASS)
annotation class DecoratorConfiguration
