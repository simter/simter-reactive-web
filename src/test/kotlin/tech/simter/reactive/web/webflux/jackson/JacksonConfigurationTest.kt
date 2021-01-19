package tech.simter.reactive.web.webflux.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.Contextual
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Flux
import tech.simter.kotlin.serialization.KotlinJsonAutoConfiguration
import tech.simter.reactive.web.webflux.WebFluxConfiguration
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@SpringJUnitConfig(
  // spring-boot jackson auto config
  JacksonAutoConfiguration::class,
  // simter-jackson-javatime auto config
  tech.simter.jackson.javatime.support.JavaTimeConfiguration::class,
  // this module
  WebFluxConfiguration::class,
  JacksonConfiguration::class,
  // test configuration
  JacksonSerializationTest.Cfg::class
)
@EnableWebFlux
@WebFluxTest(
  // exclude kotlin json auto config
  excludeAutoConfiguration = [KotlinJsonAutoConfiguration::class]
)
class JacksonSerializationTest @Autowired constructor(
  private val json: ObjectMapper,
  private val client: WebTestClient
) {
  companion object {
    var tester: Tester = Tester()
  }

  @Configuration
  class Cfg {
    /** Register a `RouterFunction<ServerResponse>` for WebFlux test*/
    @Bean
    fun testRoutes() = router {
      "/".nest {
        GET("/one") {
          ok().contentType(APPLICATION_JSON)
            .bodyValue(tester)
        }
        GET("/list") {
          ok().contentType(APPLICATION_JSON)
            .body(Flux.fromIterable(listOf(tester)))
          //.bodyValue(listOf(tester))
        }
      }
    }
  }

  data class Tester(
    val id: Int = 0,
    val name: String? = null,
    val bd: BigDecimal? = null,
    val ld: LocalDate? = null,
    val ldt: LocalDateTime? = null,
    val ldtList: List<@Contextual LocalDateTime>? = null
  )

  private fun testIt(tester: Tester, testerJson: String) {
    // test get one
    assertThat(json.writeValueAsString(tester)).isEqualTo(testerJson)
    client.get().uri("/one").exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>().isEqualTo(testerJson)
    //.expectBody().json(testerJson)

    // test get list
    assertThat(json.writeValueAsString(listOf(tester))).isEqualTo("[$testerJson]")
    client.get().uri("/list").exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>().isEqualTo("[$testerJson]")
  }

  @Test
  fun `encode simple type`() {
    tester = Tester(id = 1, name = "n")
    val testerJson = """{"id":1,"name":"n"}"""
    testIt(tester, testerJson)
  }

  @Test
  fun `encode BigDecimal`() {
    tester = Tester(bd = BigDecimal("100.12"))
    val testerJson = """{"id":0,"bd":100.12}"""
    testIt(tester, testerJson)
  }

  @Test
  fun `encode LocalDate`() {
    tester = Tester(ld = LocalDate.of(2021, 1, 31))
    val testerJson = """{"id":0,"ld":"2021-01-31"}"""
    testIt(tester, testerJson)
  }

  @Test
  fun `encode LocalDateTime`() {
    tester = Tester(ldt = LocalDateTime.of(2021, 1, 31, 0, 10, 20))
    val testerJson = """{"id":0,"ldt":"2021-01-31 00:10"}"""
    testIt(tester, testerJson)
  }
}