package api.annotation

import api.decorator.GlobalDecoratorConfig

/**
 * Marks class as a global decorator configuration. Class annotated with [GlobalDecoratorConfiguration]
 * also needs to implement [GlobalDecoratorConfig]. This configuration is shared between all generated
 * decorators. Only one class can be annotated with this annotation.
 */
@Target(AnnotationTarget.CLASS)
annotation class GlobalDecoratorConfiguration
