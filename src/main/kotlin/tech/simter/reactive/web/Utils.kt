package tech.simter.reactive.web

import org.springframework.http.MediaType
import org.springframework.http.MediaType.*

/**
 * @author RJ
 */
object Utils {
  const val TEXT_PLAIN_UTF8_VALUE: String = "$TEXT_PLAIN_VALUE;charset=UTF-8"
  val TEXT_PLAIN_UTF8: MediaType = MediaType.valueOf(TEXT_PLAIN_UTF8_VALUE)
  const val TEXT_HTML_UTF8_VALUE: String = "$TEXT_HTML_VALUE;charset=UTF-8"
  val TEXT_HTML_UTF8: MediaType = MediaType.valueOf(TEXT_HTML_UTF8_VALUE)
  const val TEXT_XML_UTF8_VALUE: String = "$TEXT_XML_VALUE;charset=UTF-8"
  val TEXT_XML_UTF8: MediaType = MediaType.valueOf(TEXT_XML_UTF8_VALUE)
}