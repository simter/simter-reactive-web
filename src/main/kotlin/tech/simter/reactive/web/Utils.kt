package tech.simter.reactive.web

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.MediaType.*
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.ProxyProvider

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

  /**
   * Create a [WebClient] instance with a [baseUrl].
   *
   * Specify a proxy by [proxyHost] and [proxyPort] parameters.
   */
  fun createWebClient(baseUrl: String, proxyHost: String? = null, proxyPort: Int? = null): WebClient {
    val clientBuilder = WebClient.builder().baseUrl(baseUrl)
    return if (proxyHost == null) clientBuilder.build()
    else { // use proxy
      logger.warn("use proxy(host={}, port={}) for server '{}'", proxyHost, proxyPort, baseUrl)
      clientBuilder
        .clientConnector(
          ReactorClientHttpConnector(
            HttpClient.create().tcpConfiguration { tcpClient ->
              tcpClient.proxy { proxy ->
                val builder = proxy.type(ProxyProvider.Proxy.HTTP).host(proxyHost)
                if (proxyPort != null) builder.port(proxyPort)
              }
            }
          )
        ).build()
    }
  }
}