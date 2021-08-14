object Versions {

    const val kotlin = "1.5.21"
    const val ksp = "1.5.21-1.0.0-beta06"
    const val protobufPlugin = "0.8.17"
    const val detekt = "1.17.1"
}

object Deps {

    // Coroutines
    private const val coroutinesVersion = "1.5.0"
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion"
    const val coroutinesTesting = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion"

    // detekt
    const val detektFormatting = "io.gitlab.arturbosch.detekt:detekt-formatting:${Versions.detekt}"

    // gRPC
    private const val gRpcVersion = "1.39.0"
    const val gRpcProtobuf = "io.grpc:grpc-protobuf:$gRpcVersion"
    const val gRpcKotlinStub = "io.grpc:grpc-kotlin-stub:1.0.0"
    const val gRpcJavaProtocGen = "io.grpc:protoc-gen-grpc-java:$gRpcVersion"
    const val gRpcKotlinProtocGen = "io.grpc:protoc-gen-grpc-kotlin:1.1.0:jdk7@jar"
    // javax annotations (for gRPC)
    const val javaxAnnotation = "javax.annotation:javax.annotation-api:1.3.2"

    // Kotlin
    const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"

    // KSP
    const val ksp = "com.google.devtools.ksp:symbol-processing-api:${Versions.ksp}"

    // Protobuf
    private const val protobufVersion = "3.17.3"
    const val protobufProtoc = "com.google.protobuf:protoc:$protobufVersion"
    const val protobufJavaUtil = "com.google.protobuf:protobuf-java-util:$protobufVersion"

    // Testing
    const val junitJupiter = "org.junit.jupiter:junit-jupiter:5.7.2"
    const val kluent = "org.amshove.kluent:kluent:1.67"
    const val kspTesting = "com.github.tschuchortdev:kotlin-compile-testing-ksp:1.4.2"
}
