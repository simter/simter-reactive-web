package tech.simter.reactive.web.webflux.kotlinserialization

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import tech.simter.kotlin.serialization.spring.KotlinSerializationJsonEncoderExt
import tech.simter.reactive.web.webflux.WebFluxConfiguration

/**
 * The default WebFlux config for simter base projects with kotlinx-serialization.
 *
 * @author RJ
 */
@Configuration("tech.simter.reactive.web.webflux.kotlinserialization.KotlinSerializationConfiguration")
@ConditionalOnProperty(name = ["simter.kotlinx-serialization.disabled"], havingValue = "false", matchIfMissing = true)
@ConditionalOnClass(Json::class)
@AutoConfigureBefore(WebFluxConfiguration::class)
class KotlinSerializationConfiguration {
  private val logger: Logger = LoggerFactory.getLogger(KotlinSerializationConfiguration::class.java)

  @Bean
  fun simterKotlinSerializationJsonEncoder(
    json: Json,
    @Value("\${simter.kotlinx-serialization.remove-class-discriminator:false}")
    removeClassDiscriminator: Boolean,
    @Value("\${simter.kotlinx-serialization.class-discriminator:type}")
    classDiscriminator: String = "type"
  ): KotlinSerializationJsonEncoder {
    logger.debug("register a KotlinSerializationJsonEncoder bean")
    return KotlinSerializationJsonEncoderExt(
      json = json,
      removeClassDiscriminator = removeClassDiscriminator,
      classDiscriminator = classDiscriminator
    )
  }

  @Bean
  fun simterKotlinSerializationJsonDecoder(json: Json): KotlinSerializationJsonDecoder {
    logger.debug("register a KotlinSerializationJsonDecoder bean")
    return KotlinSerializationJsonDecoder(json)
  }
}