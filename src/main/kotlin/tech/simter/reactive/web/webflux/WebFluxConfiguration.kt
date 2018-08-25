package tech.simter.reactive.web.webflux

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration
import org.springframework.web.reactive.config.WebFluxConfigurer
import tech.simter.jackson.ext.javatime.JavaTimeModule

/**
 * The default WebFlux config for simter base projects.
 *
 * 1. Some global jackson config :
 *   - Set serialization inclusion to `NON_EMPTY`
 *   - Disable some features:
 *     - DeserializationFeature.`FAIL_ON_UNKNOWN_PROPERTIES`
 *     - DeserializationFeature.`ADJUST_DATES_TO_CONTEXT_TIME_ZONE`
 *     - SerializationFeature.`WRITE_DATES_AS_TIMESTAMPS`
 *   - Enable feature DeserializationFeature.`ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`
 *   - Add a custom `JavaTimeModule`.
 *     > It's a brand new java-time serialize and deserialize module with global config from [simter-jackson-ext],
 *       not the jackson standard `JavaTimeModule` module. It's data-time format is for localize config,
 *       not the standard ISO format. The default data-time format is like '`yyyy-MM-dd HH:mm`',
 *       accurate to minute and without zone and offset info (global use local zone and offset default)
 * 2. Add a `Jackson2JsonEncoder` to `WebFluxConfigurer/HttpMessageCodecs`
 * 3. Add a `Jackson2JsonDecoder` to `WebFluxConfigurer/HttpMessageCodecs`
 *
 * @author RJ
 */
@Configuration("tech.simter.reactive.web.webflux.WebFluxConfiguration")
class WebFluxConfiguration {
  @Bean
  fun simterJackson2JsonEncoder(mapper: ObjectMapper): Jackson2JsonEncoder {
    return Jackson2JsonEncoder(mapper)
  }

  @Bean
  fun simterJackson2JsonDecoder(mapper: ObjectMapper): Jackson2JsonDecoder {
    return Jackson2JsonDecoder(mapper)
  }

  /**
   * Register by method [DelegatingWebFluxConfiguration.setConfigurers].
   */
  @Bean
  fun simterWebFluxConfigurer(
    @Qualifier("simterJackson2JsonEncoder") encoder: Jackson2JsonEncoder,
    @Qualifier("simterJackson2JsonDecoder") decoder: Jackson2JsonDecoder
  ): WebFluxConfigurer {
    return object : WebFluxConfigurer {
      override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        configurer.defaultCodecs().jackson2JsonEncoder(encoder)
        configurer.defaultCodecs().jackson2JsonDecoder(decoder)
      }
    }
  }

  /**
   * Register by method
   * [JacksonAutoConfiguration.Jackson2ObjectMapperBuilderCustomizerConfiguration.StandardJackson2ObjectMapperBuilderCustomizer.configureModules]
   */
  @Bean
  fun simterJavaTimeModule(): Module {
    return JavaTimeModule()
  }

  /**
   * Register by method [JacksonAutoConfiguration.JacksonObjectMapperBuilderConfiguration.jacksonObjectMapperBuilder]
   */
  @Bean
  fun simterJacksonCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
    return Jackson2ObjectMapperBuilderCustomizer {
      // not serialize null and empty value
      it.serializationInclusion(JsonInclude.Include.NON_EMPTY)

      it.featuresToDisable(
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE,
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
      )
      it.featuresToEnable(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT
      )
    }
  }
}