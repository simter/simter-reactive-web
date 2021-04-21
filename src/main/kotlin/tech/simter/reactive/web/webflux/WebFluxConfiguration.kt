package tech.simter.reactive.web.webflux

//import com.fasterxml.jackson.annotation.JsonInclude
//import com.fasterxml.jackson.databind.DeserializationFeature
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.databind.SerializationFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * The default WebFlux config for simter base projects.
 *
 * Auto create a [WebFluxConfigurer] bean with Jackson or kotlinx-serialization.
 *
 * @author RJ
 */
@Configuration("tech.simter.reactive.web.webflux.WebFluxConfiguration")
class WebFluxConfiguration {
  private val logger: Logger = LoggerFactory.getLogger(WebFluxConfiguration::class.java)

  /**
   * Register by method [DelegatingWebFluxConfiguration.setConfigurers].
   */
  @Bean
  fun simterWebFluxConfigurer(
    @Autowired(required = false) @Qualifier("simterJackson2JsonEncoder")
    jackson2JsonEncoder: Jackson2JsonEncoder? = null,
    @Autowired(required = false) @Qualifier("simterJackson2JsonDecoder")
    jackson2JsonDecoder: Jackson2JsonDecoder? = null,
    @Autowired(required = false) @Qualifier("simterKotlinSerializationJsonEncoder")
    kotlinSerializationJsonEncoder: KotlinSerializationJsonEncoder? = null,
    @Autowired(required = false) @Qualifier("simterKotlinSerializationJsonDecoder")
    kotlinSerializationJsonDecoder: KotlinSerializationJsonDecoder? = null
  ): WebFluxConfigurer {
    logger.debug("register a WebFluxConfigurer bean")
    return object : WebFluxConfigurer {
      override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val defaultCodecs = configurer.defaultCodecs()

        // jackson
        if (jackson2JsonEncoder != null) {
          logger.info("Use simterJackson2JsonEncoder")
          defaultCodecs.jackson2JsonEncoder(jackson2JsonEncoder)
        } else defaultCodecs.jackson2JsonEncoder(null)
        if (jackson2JsonDecoder != null) {
          logger.info("Use simterJackson2JsonDecoder")
          defaultCodecs.jackson2JsonDecoder(jackson2JsonDecoder)
        } else defaultCodecs.jackson2JsonDecoder(null)

        // kotlin-serialization
        if (kotlinSerializationJsonEncoder != null) {
          logger.info("Use simterKotlinSerializationJsonEncoder")
          defaultCodecs.kotlinSerializationJsonEncoder(kotlinSerializationJsonEncoder)
        } else defaultCodecs.kotlinSerializationJsonEncoder(null)
        if (kotlinSerializationJsonDecoder != null) {
          logger.info("Use simterKotlinSerializationJsonDecoder")
          defaultCodecs.kotlinSerializationJsonDecoder(kotlinSerializationJsonDecoder)
        } else defaultCodecs.kotlinSerializationJsonDecoder(null)
      }
    }
  }
}