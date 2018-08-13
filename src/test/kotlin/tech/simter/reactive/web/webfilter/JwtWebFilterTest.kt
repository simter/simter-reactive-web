package tech.simter.reactive.web.webfilter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.bindToRouterFunction
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunctions.route
import org.springframework.web.reactive.function.server.ServerResponse
import tech.simter.jwt.Header
import tech.simter.jwt.JWT
import tech.simter.jwt.Payload
import tech.simter.reactive.context.SystemContext
import tech.simter.reactive.web.Utils.TEXT_PLAIN_UTF8
import tech.simter.reactive.web.webfilter.JwtWebFilter.Companion.JWT_HEADER_NAME
import tech.simter.reactive.web.webfilter.JwtWebFilter.Companion.JWT_VALUE_PREFIX
import java.time.ZonedDateTime


@SpringJUnitConfig(JwtWebFilter::class)
@EnableWebFlux
@TestPropertySource(properties = [
  "simter.jwt.secret-key=jwt-test",
  "simter.jwt.require-authorized=true"
])
class JwtWebFilterTest @Autowired constructor(
  private val jwtWebFilter: JwtWebFilter,
  @Value("\${simter.jwt.secret-key}") private val secretKey: String
) {
  private var url = "/test"
  private var requestPredicate = RequestPredicates.POST(url)

  @Test
  fun withValidJwtHeader() {
    // create the client with the jwtWebFilter
    val client = bindToRouterFunction(route(requestPredicate, HandlerFunction<ServerResponse> {
      SystemContext.getAuthenticatedUser()
        .flatMap {
          // 1. should get the user correctly
          assertEquals(TEST_USER, it.get())
          SystemContext.hasAllRole(*TEST_ROLES.split(",").toTypedArray())
            .doOnNext {
              // 2. should has all the roles
              assertTrue(it)
            }
        }
        .flatMap { ServerResponse.noContent().build() }
    }))
      .webFilter<WebTestClient.RouterFunctionSpec>(jwtWebFilter)  // register WebFilter
      .build()

    // do request
    client.post().uri(url)
      .header(JWT_HEADER_NAME, "$JWT_VALUE_PREFIX${createJwt()}") // Add jwt header
      .exchange().expectStatus().isNoContent
  }

  @Test
  fun withInvalidJwtHeader() {
    // create the client with the jwtWebFilter
    val client = bindToRouterFunction(route(requestPredicate, HandlerFunction<ServerResponse> {
      ServerResponse.noContent().build()
    }))
      .webFilter<WebTestClient.RouterFunctionSpec>(jwtWebFilter)  // register WebFilter
      .build()

    // do request
    val responseBody = client.post().uri(url)
      .header(JWT_HEADER_NAME, "$JWT_VALUE_PREFIX something-wrong") // Add jwt header
      .exchange()
      .expectStatus().isUnauthorized
      .expectHeader().contentType(TEXT_PLAIN_UTF8)
      .expectBody(String::class.java)
      .returnResult().responseBody
    assertEquals("Invalid JWT", responseBody)
  }

  @Test
  fun withoutJwtHeader() {
    // create the client with the jwtWebFilter
    val client = bindToRouterFunction(route(requestPredicate, HandlerFunction<ServerResponse> {
      SystemContext.getAuthenticatedUser()
        .flatMap {
          // 1. without context should return null user
          assertThrows(NoSuchElementException::class.java, { it.get() })
          SystemContext.hasAllRole(*TEST_ROLES.split(",").toTypedArray())
            .doOnNext {
              // 2. should be false
              assertFalse(it)
            }
        }
        .flatMap { ServerResponse.noContent().build() }
    }))
      .webFilter<WebTestClient.RouterFunctionSpec>(jwtWebFilter)  // register WebFilter
      .build()

    // do request
    val responseBody = client.post().uri(url).exchange()
      .expectStatus().isUnauthorized
      .expectHeader().contentType(TEXT_PLAIN_UTF8)
      .expectBody(String::class.java)
      .returnResult().responseBody
    assertEquals("No valid jwt 'Authorization' header", responseBody)
  }

  private fun createJwt(): String {
    val header = Header.DEFAULT
    val payload = Payload()

    // set some registered claims
    payload.issuer = "simter"
    payload.audience = "tester"
    payload.issuedAt = ZonedDateTime.now().toEpochSecond()
    payload.expires = ZonedDateTime.now().plusMonths(1).toEpochSecond()

    // add your public/private claims
    payload.add("user.id", TEST_USER.id.toString())
    payload.add("user.code", TEST_USER.account)
    payload.add("user.name", TEST_USER.name)
    payload.add("roles", TEST_ROLES)
    return JWT(header, payload).generate(secretKey)
  }

  companion object {
    val TEST_USER = SystemContext.User(
      id = 1,
      account = "tester",
      name = "Tester"
    )
    const val TEST_ROLES = "ADMIN,COMMON,TEST"
  }
}