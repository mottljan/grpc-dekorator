# gRPC Dekorator

gRPC Dekorator is a library that you can use to decorate your RPCs to add them some special
functionality which is not possible using gRPC's `ClientInterceptor`s. Specifically, it works with
Kotlin implementation of gRPC (gRPC Kotlin Decorator -> gRPC Dekorator). gRPC Dekorator generates
decorator classes using [KSP](https://github.com/google/ksp). These classes decorate your gRPC stubs
and add them some special functionality of your choice.

## I mean that's cool and everything, but why?

You already can decorate/intercept your requests and responses using `ClientInterceptor`s. You can
for example add some headers (like authorization) to your requests or log response data. So why do
we need this library? Well, unfortunately `ClientInterceptor`s have some limitations like you can't
examine and map call exceptions or you can't easily limit interceptors to only some subset of 
RPCs (i.e. of particular stub or particular RPC). gRPC Dekorator is a missing piece in this 
functionality and enables you to decorate arbitrary number of RPCs and do stuff which are not 
possible with `ClientInterceptor`s.

## Ok, how do I use it?

Let's imagine a basic situation, where you have two gRPC stubs and you want to implement 
`CoroutineDispatcher` swapping for all RPCs. For this you need to get familiar with these types:

- `Decoration`,
- `GlobalDecoratorConfiguration`,
- `DecoratorConfiguration`.

`Decoration` specifies a functionality you want to use to decorate your RPCs. Class annotated with
`GlobalDecoratorConfiguration` can provide a global decorator configuration used for all stubs.
Usually this means providing a list of decorations common to all stubs. Classes annotated with
`DecoratorConfiguration` declare a configuration for the decorator of the particular stub. This 
decorator will be generated based on this and global configuration and use provided decorations 
to decorate your RPCs.

First you need to implement your custom `Decoration` which will decorate both `suspend` and 
streaming RPCs:

```
// This decoration is actually provided by the gRPC Dekorator library, so you don't have to 
// implement it yourself.
class DispatcherSwappingDecoration(private val dispatcher: CoroutineDispatcher) : Decoration {

    override val id = ID

    override suspend fun <Response> decorate(rpc: suspend () -> Response): Response {
        return withContext(dispatcher) { rpc() }
    }

    override fun <Response> decorateStream(rpc: () -> Flow<Response>): Flow<Response> {
        return rpc().flowOn(dispatcher)
    }

    companion object {

        val ID = Decoration.Id(DispatcherSwappingDecoration::class.qualifiedName!!)
    }
}
```

You need to implement `decorate` and `decorateStream` methods, where you swap dispatcher appropriately
and call `rpc` to continue the actual RPC call. You also need to provide an `id` unique per `Decoration`
class (more on this in [Stub-specific decorations](#stub-specific-decorations) section).

Now to apply your decoration globally to all RPCs in all stubs, you need to implement 
`GlobalDecoratorConfig` interface and annotate the class with `GlobalDecoratorConfiguration`:

```
@GlobalDecoratorConfiguration
class MyGlobalDecoratorConfig : GlobalDecoratorConfig {

    override val decorations = listOf(DispatcherSwappingDecoration(Dispatchers.IO))
}
```

You also need to tell to gRPC Dekorator which stubs it should decorate. Let's say we have 
`ArticleCoroutineStub` and `UserCoroutineStub` and we want to apply our created decoration to them.
For this you need to implement `DecoratorConfig` interface per stub:

```
@DecoratorConfiguration
class ArticleStubDecoratorConfig(private val channel: ManagedChannel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun getStub() = ArticleGrpcKt.ArticleCoroutineStub(channel)
}

@DecoratorConfiguration
class UserStubDecoratorConfig(private val channel: ManagedChannel) : DecoratorConfig<UserGrpcKt.UserCoroutineStub> {

    override fun getStub() = UserGrpcKt.UserCoroutineStub(channel)
}
```

Classes need to be annotated with `DecoratorConfiguration` annotation and return stub instance using
`getStub` method. Based on all this above configuration, gRPC Dekorator generates 
`ArticleCoroutineStubDecorator` and `UserCoroutineStubDecorator` classes which you can easily use
instead of your original stub classes. Annotation processor generates methods for all RPCs with the
same method signature, so you can only change your stub type to the decorator type without touching
RPCs. For creating an instance of the generated decorator class, you need to provide in the 
constructor an instance of the `MyGlobalDecoratorConfig` and particular `DecoratorConfig` 
implementation tied to that decorator. This provides a decorator with everything needed to decorate 
a stub with your custom decoration.

### Stub-specific decorations

You can also declare a list of decorations specific to a particular stub. To do this, you need to
provide an appropriate `Decoration.Strategy` in your `DecoratorConfig` implementation:

```
@DecoratorConfiguration
class ArticleStubDecoratorConfig(private val channel: ManagedChannel) : DecoratorConfig<ArticleGrpcKt.ArticleCoroutineStub> {

    override fun getStub() = ArticleGrpcKt.ArticleCoroutineStub(channel)

    override fun getStubDecorationStrategy(): Decoration.Strategy = appendAllStrategy { 
        append(StubSpecificDecoration())
    }
}
```

Based on this strategy the generated decorator will take `StubSpecificDecoration` and appends it to
the end of the list of globally defined decorations (if any). You can also select a different 
strategy and for example replace all global decorations with your stub-specific ones. In some cases
you also need to specify a `Decoration.Id` like in the following example:

```
...
override fun getStubDecorationStrategy(): Decoration.Strategy = customStrategy {
    removeDecorationWithId(DispatcherSwappingDecoration.ID)
}
...
```

Here you apply a `CustomStrategy` in which you remove globally declared decoration with 
`DispatcherSwappingDecoration.ID` id. You can read more about decoration strategies in the docs or
check out `testing` module for samples.

### RPC-specific decorations

Similar to stub-specific decorations you can also declare RPC-specific decorations like this:

```
@DecoratorConfiguration
class UserStubDecoratorConfig(private val channel: ManagedChannel) : DecoratorConfig<UserGrpcKt.UserCoroutineStub> {

    ...

    @RpcConfiguration(rpcName = "createUser")
    fun getCreateUserStrategy() = appendAllStrategy { 
        append(RpcSpecificDecoration())
    }
}
```

Just create a method annotated with `RpcConfiguration` annotation with `rpcName` parameter with the
name of the RPC you want to decorate and return a decoration strategy you want to apply. Since 
gRPC Dekorator tries to provide clear and expressive API, you also need to do a small adjustment in
your RPC call:

```
userStubDecorator.createUser(grpcRequest, userStubDecorator.createUserDecorations)
```

With the above configuration gRPC Dekorator will generate one more parameter to the `createUser`
method of the given stub's decorator. Type of the parameter will be 
`UserCoroutineStubDecorator.CreateUserDecorations` and it is just a simple wrapper class of your
resolved RPC-specific decorations (final list of decorations after applying the strategy to the
global and stub-specific decorations). You can easily get the instance of that from the decorator 
and simply pass it to the `createUser` method. Of course this could be possible to do inside the 
decorator without passing decorations as a parameter, but it would be really hard to figure out that
this particular RPC uses some special configuration. By this simple "trick", RPC-specific configuration
becomes explicit to the clients and might eliminate a confusion about a different behaviour of the RPC.

### Exception handling

There are some cases when gRPC Dekorator can throw an exception at runtime because of invalid 
configuration (i.e trying to remove decoration with a non-existing ID). In these cases the exception
is thrown by default. However you can instead consume these exceptions and handle them in any way
you want by overriding `GlobalDecoratorConfig.handleException`:

```
@GlobalDecoratorConfiguration
class MyGlobalDecoratorConfig : GlobalDecoratorConfig {

    override fun handleException(exception: Exception) {
        println(exception)
    }
}
```

## Installation

Library is located on Maven Central. Just add these dependencies to your module's build file:

```
dependencies {
    implementation "io.github.mottljan:dekorator-api:$dekoratorVersion"
    ksp "io.github.mottljan:dekorator-processor:$dekoratorVersion"
}
```

Replace the variable `$dekoratorVersion` with the latest version: [ ![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.mottljan/dekorator-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.mottljan/dekorator-api)

If you don't already use KSP in your project, you also need to set it up:

```
// root project's build.gradle
classpath "com.google.devtools.ksp:symbol-processing-gradle-plugin:$kspVersion"

// module's build.gradle
apply plugin: "com.google.devtools.ksp"

```

### Additional setup

It is needed to do some additional setup for the library to work correctly. Processor needs to work
with generated gRPC sources and they have to be resolvable by the processor. To make them resolvable
you need to include them to the sources like this:

**JVM project**
```
kotlin.sourceSets.main {
    kotlin.srcDirs(
        ...
        file("$buildDir/generated/source/proto/main/grpckt"),
        file("$buildDir/generated/source/proto/main/java")
    )
}
```

**Android project**
```
sourceSets {
    applicationVariants.all { variant ->
        getByName(variant.name) {
            ...
            kotlin.srcDirs += "$buildDir/generated/source/proto/${variant.name}/grpckt"
            kotlin.srcDirs += "$buildDir/generated/source/proto/${variant.name}/java"
        }
    }
}
```

Next you should also make KSP task dependent on the task generating gRPC code from proto files. It
can work even without the explicit dependency, but Gradle will warn you during build about this 
"hidden" dependency and recommend you to make it explicit. Since it could potentially break in the 
future (or for some build variants), it is better to declare the dependency explicitly:

**JVM project**
```
afterEvaluate {
    def kspTaskName = "kspKotlin"
    def generateProtoTaskName = "generateProto"
    def kspTask = tasks.getByName(kspTaskName)
    def generateProtoTask = tasks.getByName(generateProtoTaskName)
    kspTask.dependsOn(generateProtoTask)
}
```

**Android project**
```
afterEvaluate {
    android.applicationVariants.all { variant ->
        def capitalizedVariantName = variant.name.capitalize()
        def kspTaskName = "ksp${capitalizedVariantName}Kotlin"
        def generateProtoTaskName = "generate${capitalizedVariantName}Proto"
        def kspTask = tasks.getByName(kspTaskName)
        def generateProtoTask = tasks.getByName(generateProtoTaskName)
        kspTask.dependsOn(generateProtoTask)
    }
}
```

Finally, there is an opened [issue](https://github.com/google/ksp/issues/37) for the generated files
not to be recognized by the IDE (project will compile though). You can make the files recognizable
like this:

**JVM project**
```
kotlin.sourceSets.main {
    kotlin.srcDirs(
        file("$buildDir/generated/ksp/main/kotlin"),
        ...
    )
}
```

**Android project**
```
sourceSets {
    applicationVariants.all { variant ->
        getByName(variant.name) {
            java.srcDirs += "build/generated/ksp/${variant.name}/kotlin"
            ...
        }
    }
}
```

If you don't use Dekorator in the Android app module but in some Android library module, you need to
replace `applicationVariants` with `libraryVariants`.

## Limitations

Because decorations are designed for usage with any RPC, you can't read request or response data.
Unlike `ClientInterceptor`s, decorations are a higher level construct so you do not have access
to gRPC metadata and other stuff either.

## More info

You can find more information in the source docs or check out `testing` module for sample usage.
