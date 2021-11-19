package tech.simter.reactive.web.webfilter

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import tech.simter.jwt.DecodeException
import tech.simter.jwt.JWT
import tech.simter.reactive.context.SystemContext
import tech.simter.reactive.context.SystemContext.SYSTEM_CONTEXT_KEY
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8_VALUE

private data class ExcludePath(val path: String, val method: String = "GET")

/**
 * A [WebFilter] for verify a `Authorization` header with jwt.
 *
 * Abort with status [UNAUTHORIZED] if without a jwt type `Authorization` header
 * or the jwt header verified failed.
 */
@Component
class JwtWebFilter @Autowired constructor(
  @Value("\${simter.jwt.secret-key:test}") private val secretKey: String,
  @Value("\${simter.jwt.require-authorized:false}") private val requireAuthorized: Boolean,
  @Value("\${simter.jwt.exclude-paths:#{null}}") private val excludeStringPaths: List<String>?
) : WebFilter {
  private val logger = LoggerFactory.getLogger(JwtWebFilter::class.java)
  private val hasExcludePaths: Boolean = excludeStringPaths != null && excludeStringPaths.isNotEmpty()

  // convert string value to ExcludePath
  private val excludePaths: List<ExcludePath> = excludeStringPaths?.let { list ->
    list.map {
      // such as 'GET:/static' or '/static'
      val t = it.split(":")
      if (t.size == 1) ExcludePath(path = t[0].trim())
      else if (t.size > 1) ExcludePath(path = t[1].trim(), method = t[0].trim().toUpperCase())
      else throw IllegalArgumentException("'simter.jwt.exclude-paths' config error on item '$it'")
    }
  } ?: emptyList()

  init {
    logger.warn("Register JwtWebFilter")
    logger.warn("simter.jwt.require-authorized={}", requireAuthorized)
    logger.warn("simter.jwt.exclude-paths={}", excludeStringPaths?.joinToString(","))
  }

  companion object {
    /** The header name to hold JWT token */
    const val JWT_HEADER_NAME = "Authorization"

    /** The prefix of the jwt header value */
    const val JWT_VALUE_PREFIX = "Bearer "
  }

  override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
    return if (!requireAuthorized
      || exchange.request.method == HttpMethod.OPTIONS
      || isExcludePath(exchange.request.path.value(), exchange.request.method!!.name))
      chain.filter(exchange)
    else { // need authorized
      var authorization = exchange.request.headers.getFirst(JWT_HEADER_NAME)
      if (authorization == null || !authorization.startsWith(JWT_VALUE_PREFIX)) {
        abortRequest(
          response = exchange.response,
          status = UNAUTHORIZED,
          body = "No valid jwt 'Authorization' header"
        )
      } else {
        authorization = authorization.substring(7) // "Bearer ".length() == 7
        logger.debug("jwt={}", authorization)
        try {
          val jwt = JWT.verify(authorization, secretKey)
          logger.debug("jwt verify success")
          chain.filter(exchange)
            .contextWrite {
              // generate extras data
              val extras = mutableMapOf("path" to exchange.request.path.value())
              exchange.request.headers.getFirst(JWT_HEADER_NAME)?.apply { extras[JWT_HEADER_NAME] = this }
              exchange.request.headers.getFirst("origin")?.apply { extras["origin"] = this }

              // create a system-context from jwt.payload.data
              val data = jwt.payload.data
              it.put(SYSTEM_CONTEXT_KEY, SystemContext.DataHolder(
                user = SystemContext.User(
                  id = data["user.id"]?.toInt() ?: 0,
                  account = data["user.code"] ?: "UNKNOWN",
                  name = data["user.name"] ?: "UNKNOWN"
                ),
                roles = data["roles"]?.split(",") ?: listOf(),
                extras = extras
              ))
            }
        } catch (e: DecodeException) { // jwt is illegal
          if (logger.isDebugEnabled) logger.debug(e.message, e) else logger.warn(e.message)
          abortRequest(
            response = exchange.response,
            status = UNAUTHORIZED,
            body = "Invalid JWT"
          )
        }
      }
    }
  }

  private fun isExcludePath(path: String, method: String = HttpMethod.GET.name): Boolean {
    return if (isRootPath(path)) true else hasExcludePaths && excludePaths.any {
      method == it.method && path.startsWith(it.path)
    }
  }

  private fun isRootPath(path: String) = path == "/" || path == "/index.html" || path == "/index.htm"

  private fun abortRequest(response: ServerHttpResponse, status: HttpStatus, body: String? = null): Mono<Void> {
    response.statusCode = status
    return if (body == null || body.isEmpty()) Mono.empty()
    else {
      response.headers.set("Content-Type", TEXT_PLAIN_UTF8_VALUE)
      response.writeWith(Flux.just(response.bufferFactory().wrap(body.toByteArray())))
    }
  }
}