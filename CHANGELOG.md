# simter-reactive-web changelog

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