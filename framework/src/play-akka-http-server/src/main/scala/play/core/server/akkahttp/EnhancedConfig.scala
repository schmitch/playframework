/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.ConfigurationException
import com.typesafe.config.Config

import scala.concurrent.duration.Duration

/**
 * Copied from [[akka.http.impl.util.EnhancedConfig]] (slightly modified)
 */
private[server] class EnhancedConfig(val underlying: Config) extends AnyVal {

  def getPotentiallyInfiniteDuration(path: String): Duration = underlying.getString(path) match {
    case "infinite" ⇒ Duration.Inf
    case x ⇒ Duration(x)
  }

  def getIntBytes(path: String): Int = {
    val value: Long = underlying getBytes path
    if (value <= Int.MaxValue) value.toInt
    else throw new ConfigurationException(s"Config setting '$path' must not be larger than ${Int.MaxValue}")
  }

  def getPossiblyInfiniteBytes(path: String): Long = underlying.getString(path) match {
    case "infinite" ⇒ Long.MaxValue
    case x ⇒ underlying.getBytes(path)
  }
}
