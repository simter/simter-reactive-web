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

  const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36"
  private var defaultSslContext = SslContextBuilder
    .forClient()
    .trustManager(InsecureTrustManagerFactory.INSTANCE)
    .build()

  /**
   * Create a [WebClient] instance with a [baseUrl] for common usage.
   *
   * 1. Specify a proxy by [proxyHost] and [proxyPort] parameters.
   * 2. default not auto redirect by param [autoRedirect].
   * 3. default not use ssl by param [secure].
   * 4. default connect timeout 30 seconds by param [connectTimeout].
   * 5. default read write timeout 120 seconds by param [readWriteTimeout].
   */
  fun createWebClient(
    baseUrl: String,
    proxyHost: String? = null,
    proxyPort: Int? = null,
    connectTimeout: Int = 30,      // default 30 seconds
    readWriteTimeout: Int = 120,   // default 2 minutes
    secure: Boolean = false,       // whether use ssl (true-https, false-http)
    autoRedirect: Boolean = false, // whether auto redirect when status code in 30[1278]
    userAgent: String? = null
  ): WebClient {
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
    if (secure) tcpClient = tcpClient.secure { it.sslContext(defaultSslContext) }

    // create web client instance
    return WebClient.builder()
      .baseUrl(baseUrl)                   // base url
      .defaultHeader("User-Agent", userAgent ?: DEFAULT_USER_AGENT) // default User-Agent
      .clientConnector(
        ReactorClientHttpConnector(
          HttpClient.from(tcpClient)
            .followRedirect(autoRedirect) // auto redirect
        )
      )
      .build()
  }
}