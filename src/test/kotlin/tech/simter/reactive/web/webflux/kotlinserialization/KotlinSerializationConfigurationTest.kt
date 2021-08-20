package tech.simter.reactive.web.webflux.kotlinserialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
  // simter-kotlin module
  KotlinJsonAutoConfiguration::class,
  // this module
  WebFluxConfiguration::class,
  KotlinSerializationConfiguration::class,
  // test configuration
  KotlinxSerializationTest.Cfg::class
)
@EnableWebFlux
@WebFluxTest(
  // exclude jackson auto config
  excludeAutoConfiguration = [JacksonAutoConfiguration::class]
)
class KotlinxSerializationTest @Autowired constructor(
  private val json: Json,
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

  @Serializable
  data class Tester(
    val id: Int = 0,
    val name: String? = null,
    @Contextual
    val bd: BigDecimal? = null,
    @Contextual
    val ld: LocalDate? = null,
    @Contextual
    val ldt: LocalDateTime? = null,
    val ldtList: List<@Contextual LocalDateTime>? = null
  )

  private fun testIt(tester: Tester, testerJson: String) {
    // test get one
    assertThat(json.encodeToString(tester)).isEqualTo(testerJson)
    client.get().uri("/one").exchange()
      .expectStatus().isOk
      .expectHeader().contentType(APPLICATION_JSON)
      .expectBody<String>().isEqualTo(testerJson)
    //.expectBody().json(testerJson)

    // test get list
    assertThat(json.encodeToString(listOf(tester))).isEqualTo("[$testerJson]")
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
    val testerJson = """{"bd":100.12}"""
    testIt(tester, testerJson)
  }

  @Test
  fun `encode LocalDate`() {
    tester = Tester(ld = LocalDate.of(2021, 1, 31))
    val testerJson = """{"ld":"2021-01-31"}"""
    testIt(tester, testerJson)
  }

  @Test
  fun `encode LocalDateTime`() {
    tester = Tester(ldt = LocalDateTime.of(2021, 1, 31, 0, 10, 20))
    val testerJson = """{"ldt":"2021-01-31T00:10:20"}"""
    testIt(tester, testerJson)
  }
}