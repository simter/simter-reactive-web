# simter-reactive-web changelog

## 2.0.0 - 2020-11-19

- Upgrade to simter-dependencies-2.0.0

## 2.0.0-M2 - 2020-07-27

- Upgrade to simter-dependencies-2.0.0-M2
- Move java-time config to module `simter-jackson-javatime`

## 2.0.0-M1 - 2020-06-02

- Upgrade to simter-dependencies-2.0.0-M1

## 1.2.0-M6 - 2020-04-15

- Upgrade to simter-1.3.0-M14

## 1.2.0-M5 - 2020-03-03

- Add Utils.createClientHttpConnector method
- Upgrade to simter-1.3.0-M13

## 1.2.0-M4 - 2019-12-07

- Upgrade to simter-1.3.0-M9

## 1.2.0-M3 - 2019-11-28

- Upgrade to simter-1.3.0-M7
- Set default user-agent to chrome-46
- Should not share sslContext on each WebClient
- Use fixed pool connection by default on Utils.createWebClientBuilder

## 1.2.0-M2 - 2019-11-20

- Upgrade to simter-1.3.0-M6
- Add maxInMemorySize (KB unit) param on Utils.createWebClient

## 1.2.0-M1 - 2019-11-16

- Upgrade to simter-1.3.0-M4
- Fixed empty proxy error in Utils.createWebClient
- Add timeout, redirect, secure and userAgent params on Utils.createWebClient

## 1.1.2 - 2019-10-14

- Add DefaultDataBufferInserter class
    > Extension of `BodyInserter` that allows for write data to body through default allocate `DataBuffer`

## 1.1.1 - 2019-09-27

- Fixed kotlin compile config

## 1.1.0 - 2019-07-03

- Change parent to simter-dependencies-1.2.0
- Use `simter-jackson-javatime` instead of `simter-jackson-ext`
- Add `Utils.TEXT_HTML_UTF8|TEXT_XML_UTF8` constants
- Set `simter-jackson-javatime` optional
- Add `Utils.createWebClient` method
- Add extras data to context by `JwtWebFilter`

## 1.0.0 - 2019-01-08

- `OPTIONS` request not need to authorized
- Add default WebFlux configuration class `WebFluxConfiguration`
- Set `simter.jwt.require-authorized` default value to false
- Modify global setting json include non null
- Add exclude paths `simter.jwt.exclude-paths` config for JwtWebFilter

## 0.5.0 - 2018-08-13

- Add `JwtWebFilter`
- Add `Utils.TEXT_PLAIN_UTF8_VALUE`
- Add `Utils.TEXT_PLAIN_UTF8`