/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.settings.ParserSettings
import akka.http.scaladsl.settings.ParserSettings.{ CookieParsingMode, ErrorLoggingVerbosity, IllegalResponseHeaderValueProcessingMode }
import com.typesafe.config.Config

import scala.collection.JavaConverters._

/**
 * References: [[akka.http.impl.settings.ParserSettingsImpl]]
 */
private[server] object PlayParserSettings {

  def fromSubConfig(config: Config): ParserSettings = {
    val settings = ParserSettings("") // with config, we can't use our own object
    val cacheConfig = config getConfig "header-cache"

    settings
      .withMaxUriLength(config getIntBytes "max-uri-length")
      .withMaxMethodLength(config getIntBytes "max-method-length")
      .withMaxResponseReasonLength(config getIntBytes "max-response-reason-length")
      .withMaxHeaderNameLength(config getIntBytes "max-header-name-length")
      .withMaxHeaderValueLength(config getIntBytes "max-header-value-length")
      .withMaxHeaderCount(config getIntBytes "max-header-count")
      .withMaxContentLength(config getPossiblyInfiniteBytes "max-content-length")
      .withMaxChunkExtLength(config getIntBytes "max-chunk-ext-length")
      .withMaxChunkSize(config getIntBytes "max-chunk-size")
      .withUriParsingMode(Uri.ParsingMode(config getString "uri-parsing-mode"))
      .withCookieParsingMode(CookieParsingMode(config getString "cookie-parsing-mode"))
      .withIllegalHeaderWarnings(config getBoolean "illegal-header-warnings")
      .withIllegalResponseHeaderValueProcessingMode(IllegalResponseHeaderValueProcessingMode(config getString "illegal-response-header-value-processing-mode"))
      .withErrorLoggingVerbosity(ErrorLoggingVerbosity(config getString "error-logging-verbosity"))
      .withIncludeTlsSessionInfoHeader(config getBoolean "tls-session-info-header")
      .withHeaderValueCacheLimits(cacheConfig.entrySet.asScala.map(kvp ⇒ kvp.getKey → cacheConfig.getInt(kvp.getKey))(collection.breakOut): Map[String, Int])
  }

}

