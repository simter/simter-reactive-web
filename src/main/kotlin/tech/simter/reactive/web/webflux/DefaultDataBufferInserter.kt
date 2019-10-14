package tech.simter.reactive.web.webflux

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.web.reactive.function.BodyInserter
import reactor.core.publisher.Mono

/**
 * Extension of [BodyInserter] that allows for write data to body through default allocate [DataBuffer].
 *
 * The default allocate [DataBuffer] is created by method [ReactiveHttpOutputMessage.bufferFactory().allocateBuffer()].
 *
 * Example:
 * ```
 * override fun handle(request: ServerRequest): Mono<ServerResponse> {
 *   return ok()
 *     .contentType(APPLICATION_OCTET_STREAM)
 *     .header("Content-Disposition", "attachment; filename=\"t.txt\"")
 *     .body(DefaultDataBufferInserter {
 *       // way 1: write something to this dataBuffer directly
 *       // it.write(...)
 *
 *       // way 2: write something to outputStream from this dataBuffer
 *       // val os: OutputStream = it.asOutputStream()
 *       // write something to os, such as `os.write(...)`
 *     })
 * }
 * ```
 */
class DefaultDataBufferInserter(private val writeTo: (DataBuffer) -> Mono<Void>) : BodyInserter<Void, ServerHttpResponse> {
  override fun insert(outputMessage: ServerHttpResponse, context: BodyInserter.Context): Mono<Void> {
    val buffer = outputMessage.bufferFactory().allocateBuffer()
    return outputMessage.writeWith(writeTo(buffer).thenReturn(buffer))
  }
}