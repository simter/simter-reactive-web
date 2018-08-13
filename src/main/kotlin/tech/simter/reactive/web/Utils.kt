package tech.simter.reactive.web

import org.springframework.http.MediaType
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE

/**
 * @author RJ
 */
object Utils {
  const val TEXT_PLAIN_UTF8_VALUE: String = "$TEXT_PLAIN_VALUE;charset=UTF-8"
  val TEXT_PLAIN_UTF8: MediaType = MediaType.valueOf(TEXT_PLAIN_UTF8_VALUE)
}