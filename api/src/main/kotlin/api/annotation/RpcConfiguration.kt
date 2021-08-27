package api.annotation

/**
 * TODO add class description
 */
@Target(AnnotationTarget.FUNCTION)
annotation class RpcConfiguration(val rpcName: String)
