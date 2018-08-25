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

A Jwt WebFilter for webflux. Enabled by `simter.jwt.require-authorized`, it's default value is `true`.

The Jwt secret key should be config by `simter.jwt.secret-key`, it's default value is `test`.

### 2.2. WebFluxConfiguration

A '[WebFlux config API]' implementation (implements `WebFluxConfigurer` interface). And some jackson global config:

1. Some global jackson config :
    - Set serialization inclusion to `NON_EMPTY`
    - Disable some features:
        - DeserializationFeature.`FAIL_ON_UNKNOWN_PROPERTIES`
        - DeserializationFeature.`ADJUST_DATES_TO_CONTEXT_TIME_ZONE`
        - SerializationFeature.`WRITE_DATES_AS_TIMESTAMPS`
    - Enable feature DeserializationFeature.`ACCEPT_EMPTY_STRING_AS_NULL_OBJECT`
    - Add a custom `JavaTimeModule`.
        > It's a brand new java-time serialize and deserialize module with global config from [simter-jackson-ext],
          not the jackson standard `JavaTimeModule` module. It's data-time format is for localize config, 
          not the standard ISO format. The default data-time format is like `yyyy-MM-dd HH:mm`, 
          accurate to minute and without zone and offset info (global use local zone and offset default)

2. Add a `Jackson2JsonEncoder` to `WebFluxConfigurer/HttpMessageCodecs`
3. Add a `Jackson2JsonDecoder` to `WebFluxConfigurer/HttpMessageCodecs`


## 3. Build

```bash
mvn clean package
```

[WebFlux config API]: https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#webflux-config-customize
[simter-jackson-ext]: https://github.com/simter/simter-jackson-ext