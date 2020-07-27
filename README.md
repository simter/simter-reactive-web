# simter-reactive-web

Simter reactive web extension. It's for webflux extension config.

## 1. Installation

```xml
<dependency>
  <groupId>tech.simter.reactive</groupId>
  <artifactId>simter-reactive-web</artifactId>
  <version>${VERSION}</version>
</dependency>
```

## 2. Extensions

### 2.1. JwtWebFilter

A Jwt WebFilter for webflux.

- Enabled by `simter.jwt.require-authorized`, it's default value is `false`.
- Config the Jwt secret key by `simter.jwt.secret-key`, it's default value is `test`.
- Config the Jwt exclude array paths by `simter.jwt.exclude-paths`, it's default value is `null`.

### 2.2. WebFluxConfiguration

A '[WebFlux config API]' implementation (implements `WebFluxConfigurer` interface). And some jackson global config:

1. Some global jackson config :
    - Set serialization inclusion to `NON_NULL`
    - Disable some features:
        - DeserializationFeature.`FAIL_ON_UNKNOWN_PROPERTIES`
        - DeserializationFeature.`ADJUST_DATES_TO_CONTEXT_TIME_ZONE`
        - SerializationFeature.`WRITE_DATES_AS_TIMESTAMPS`
    - Enable feature DeserializationFeature.`ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`
    - Add a custom `JavaTimeModule` if has dependency `tech.simter:simter-jackson-javatime`.
        > It's a brand new java-time serialize and deserialize module with global config from [simter-jackson-javatime],
          not the jackson standard `JavaTimeModule` module. It's data-time format is for localize config, 
          not the standard ISO format. The default data-time format is like `yyyy-MM-dd HH:mm`, 
          accurate to minute and without zone and offset info (global use local zone and offset default)

2. Add a `Jackson2JsonEncoder` to `WebFluxConfigurer/HttpMessageCodecs`
3. Add a `Jackson2JsonDecoder` to `WebFluxConfigurer/HttpMessageCodecs`

### 2.3. DefaultDataBufferInserter

An extension of `BodyInserter` that allows for write data to body through default allocate `DataBuffer`.

Example:

```
override fun handle(request: ServerRequest): Mono<ServerResponse> {
  return ok()
    .contentType(APPLICATION_OCTET_STREAM)
    .header("Content-Disposition", "attachment; filename=\"t.txt\"")
    .body(DefaultDataBufferInserter {
      // way 1: write something to this dataBuffer directly
      // it.write(...)

      // way 2: write something to outputStream from this dataBuffer
      // val os: OutputStream = it.asOutputStream()
      // write something to os, such as `os.write(...)`
    })
}
```

## 3. Build

```bash
mvn clean package
```

[WebFlux config API]: https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-config-customize
[simter-jackson-javatime]: https://github.com/simter/simter-jackson-javatime