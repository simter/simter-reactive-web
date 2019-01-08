# Changelog for simter-reactive-web package

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