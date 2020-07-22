package tech.simter.reactive.web

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import reactor.netty.tcp.ProxyProvider
import reactor.netty.tcp.TcpClient

/**
 * @author RJ
 */
object Utils {
  private val logger = LoggerFactory.getLogger(Utils::class.java)
  const val TEXT_PLAIN_UTF8_VALUE: String = "$TEXT_PLAIN_VALUE;charset=UTF-8"
  val TEXT_PLAIN_UTF8: MediaType = MediaType.valueOf(TEXT_PLAIN_UTF8_VALUE)
  const val TEXT_HTML_UTF8_VALUE: String = "$TEXT_HTML_VALUE;charset=UTF-8"
  val TEXT_HTML_UTF8: MediaType = MediaType.valueOf(TEXT_HTML_UTF8_VALUE)
  const val TEXT_XML_UTF8_VALUE: String = "$TEXT_XML_VALUE;charset=UTF-8"
  val TEXT_XML_UTF8: MediaType = MediaType.valueOf(TEXT_XML_UTF8_VALUE)

  const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36"

  fun createWebClient(
    baseUrl: String,
    proxyHost: String? = null,
    proxyPort: Int? = null,
    connectTimeout: Int = 30,      // default 30 seconds
    readWriteTimeout: Int = 120,   // default 2 minutes
    secure: Boolean = false,       // whether use ssl (true-https, false-http)
    autoRedirect: Boolean = false, // whether auto redirect when status code in 30[1278]
    userAgent: String? = null,
    maxInMemorySize: Int? = null   // MB unit for max body size
  ): WebClient {
    return createWebClientBuilder(
      baseUrl = baseUrl,
      proxyHost = proxyHost,
      proxyPort = proxyPort,
      connectTimeout = connectTimeout,
      readWriteTimeout = readWriteTimeout,
      secure = secure,
      autoRedirect = autoRedirect,
      userAgent = userAgent,
      maxInMemorySize = maxInMemorySize
    ).build()
  }

  /**
   * Create a [WebClient] instance with a [baseUrl] for common usage.
   *
   * 1. Specify a proxy by [proxyHost] and [proxyPort] parameters.
   * 2. default not auto redirect by param [autoRedirect].
   * 3. default not use ssl by param [secure].
   * 4. default connect timeout 30 seconds by param [connectTimeout].
   * 5. default read write timeout 120 seconds by param [readWriteTimeout].
   */
  fun createWebClientBuilder(
    baseUrl: String,
    proxyHost: String? = null,
    proxyPort: Int? = null,
    connectTimeout: Int = 30,      // default 30 seconds
    readWriteTimeout: Int = 120,   // default 2 minutes
    secure: Boolean = false,       // whether use ssl (true-https, false-http)
    autoRedirect: Boolean = false, // whether auto redirect when status code in 30[1278]
    userAgent: String? = null,
    maxInMemorySize: Int? = null   // KB unit, for max body size，since spring-5.2.1
  ): WebClient.Builder {
    logger.warn("WebClient: baseUrl={}, maxInMemorySize={}", baseUrl, maxInMemorySize)

    // create web client instance
    // DEFAULT_MESSAGE_MAX_SIZE 256K (= 256 * 1024 = 262144)
    // See https://github.com/spring-projects/spring-framework/issues/23961
    val builder = WebClient.builder()
      .baseUrl(baseUrl)                   // base url
      .defaultHeader("User-Agent", userAgent ?: DEFAULT_USER_AGENT) // default User-Agent
      .clientConnector(
        createClientHttpConnector(
          proxyHost = proxyHost,
          proxyPort = proxyPort,
          connectTimeout = connectTimeout,
          socketTimeout = connectTimeout,
          readTimeout = readWriteTimeout,
          writeTimeout = readWriteTimeout,
          secure = secure,
          autoRedirect = autoRedirect
        )
      )

    // set for max body size，since spring-5.2.1
    if (maxInMemorySize != null && maxInMemorySize > 0) {
      builder.exchangeStrategies(
        ExchangeStrategies.builder()
          .codecs { it.defaultCodecs().maxInMemorySize(maxInMemorySize * 1024) } // KB to Byte
          .build()
      )
    }
    return builder
  }

  fun createClientHttpConnector(
    proxyHost: String? = null,
    proxyPort: Int? = null,
    connectTimeout: Int = 30,      // default 30 seconds
    socketTimeout: Int = 30,       // default 30 seconds
    readTimeout: Int = 120,        // default 1 minutes
    writeTimeout: Int = 120,       // default 1 minutes
    secure: Boolean = false,       // whether use ssl (true-https, false-http)
    autoRedirect: Boolean = false  // whether auto redirect when status code in 30[1278]
  ): ClientHttpConnector {
    logger.warn("""
      ClientHttpConnector: 
        connectTimeout={}s,
        socketTimeout={}s,
        readTimeout={}s, 
        writeTimeout={}s, 
        autoRedirect={}, 
        proxyHost={}, 
        proxyPort={}, 
        secure={}
      """,
      connectTimeout, socketTimeout, readTimeout, writeTimeout, autoRedirect, proxyHost, proxyPort, secure
    )

    // timeout（WebFlux-v5.1+）
    var tcpClient: TcpClient = TcpClient.create(ConnectionProvider.create("httpPool")) // fixed max connect pool
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000) // connect timeout ms
      .option(ChannelOption.SO_TIMEOUT, socketTimeout * 1000)              // socket timeout ms
      .doOnConnected {
        it.addHandlerLast(ReadTimeoutHandler(readTimeout))                 // read timeout seconds
          .addHandlerLast(WriteTimeoutHandler(writeTimeout))               // write timeout seconds
      }

    // proxy
    if (proxyPort != null || !proxyHost.isNullOrEmpty()) {
      tcpClient = tcpClient.proxy {
        it.type(ProxyProvider.Proxy.HTTP)
          .host(proxyHost ?: "localhost") // default proxy host localhost
          .port(proxyPort ?: 8888)        // default proxy port 8888
      }
    }

    // ssl
    if (secure) tcpClient = tcpClient.secure {
      it.sslContext(
        SslContextBuilder
          .forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE)
          .build()
      )
    }

    return ReactorClientHttpConnector(
      HttpClient.from(tcpClient)
        .followRedirect(autoRedirect) // auto redirect
    )
  }

  /**
   * Build a `Mono<ServerResponse>` instance with the specific status code.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseSpecificStatus(status: HttpStatus, e: Throwable): Mono<ServerResponse> {
    return if (e.message.isNullOrBlank()) ServerResponse.status(status).build()
    else ServerResponse.status(status).contentType(TEXT_PLAIN_UTF8).bodyValue(e.message!!)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with the specific status code.
   * If the [msg] has content, use it as the response body content with content-type `text/plain`.
   */
  fun responseSpecificStatus(status: HttpStatus, msg: String? = null): Mono<ServerResponse> {
    return if (msg.isNullOrBlank()) ServerResponse.status(status).build()
    else ServerResponse.status(status).contentType(TEXT_PLAIN_UTF8).bodyValue(msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `404 Not Found`.
   * Use the [msg] as the response body content with content-type `text/plain`.
   */
  fun responseNotFoundStatus(msg: String): Mono<ServerResponse> {
    return responseSpecificStatus(NOT_FOUND, msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `404 Not Found`.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseNotFoundStatus(e: Throwable): Mono<ServerResponse> {
    return responseSpecificStatus(NOT_FOUND, e)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `410 Gone`.
   * Use the [msg] as the response body content with content-type `text/plain`.
   */
  fun responseGoneStatus(msg: String): Mono<ServerResponse> {
    return responseSpecificStatus(GONE, msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `410 Gone`.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseGoneStatus(e: Throwable): Mono<ServerResponse> {
    return responseSpecificStatus(GONE, e)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `403 Forbidden`.
   * Use the [msg] as the response body content with content-type `text/plain`.
   */
  fun responseForbiddenStatus(msg: String): Mono<ServerResponse> {
    return responseSpecificStatus(FORBIDDEN, msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `403 Forbidden`.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseForbiddenStatus(e: Throwable): Mono<ServerResponse> {
    return responseSpecificStatus(FORBIDDEN, e)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `400 Bad Request`.
   * Use the [msg] as the response body content with content-type `text/plain`.
   */
  fun responseBadRequestStatus(msg: String): Mono<ServerResponse> {
    return responseSpecificStatus(BAD_REQUEST, msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `400 Bad Request`.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseBadRequestStatus(e: Throwable): Mono<ServerResponse> {
    return responseSpecificStatus(BAD_REQUEST, e)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `409 Conflict`.
   * Use the [msg] as the response body content with content-type `text/plain`.
   */
  fun responseConflictStatus(msg: String): Mono<ServerResponse> {
    return responseSpecificStatus(CONFLICT, msg)
  }

  /**
   * Build a `Mono<ServerResponse>` instance with status code `409 Conflict`.
   * If the [e] has message content, use it as the response body content with content-type `text/plain`.
   */
  fun responseConflictStatus(e: Throwable): Mono<ServerResponse> {
    return responseSpecificStatus(CONFLICT, e)
  }
}