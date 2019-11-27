package tech.simter.reactive.web

import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
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
    logger.warn(
      "WebClient: connectTimeout={}s, readWriteTimeout={}s, proxy.host={}, proxy.port={}, secure={}",
      connectTimeout, readWriteTimeout, proxyHost, proxyPort, secure
    )

    // timeout（WebFlux-v5.1+）
    var tcpClient: TcpClient = TcpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000) // connect timeout ms
      .option(ChannelOption.SO_TIMEOUT, connectTimeout * 1000)             // socket timeout ms
      .doOnConnected {
        it.addHandlerLast(ReadTimeoutHandler(readWriteTimeout))            // read timeout seconds
          .addHandlerLast(WriteTimeoutHandler(readWriteTimeout))           // write timeout seconds
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

    // create web client instance
    // DEFAULT_MESSAGE_MAX_SIZE 256K (= 256 * 1024 = 262144)
    // See https://github.com/spring-projects/spring-framework/issues/23961
    val builder = WebClient.builder()
      .baseUrl(baseUrl)                   // base url
      .defaultHeader("User-Agent", userAgent ?: DEFAULT_USER_AGENT) // default User-Agent
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient.from(tcpClient)
            .followRedirect(autoRedirect) // auto redirect
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
}