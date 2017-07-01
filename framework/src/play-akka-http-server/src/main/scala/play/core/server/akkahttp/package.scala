/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server

import com.typesafe.config.Config

import scala.language.implicitConversions

package object akkahttp {

  private[server] implicit def enhanceConfig(config: Config): EnhancedConfig = new EnhancedConfig(config)

}
