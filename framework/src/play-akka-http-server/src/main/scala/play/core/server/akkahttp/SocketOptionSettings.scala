/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.server.akkahttp

import akka.io.{ Inet, Tcp }

import scala.collection.immutable
import akka.io.Inet.SocketOption
import com.typesafe.config.Config

/**
 * Copied from [[akka.http.impl.util.SocketOptionSettings]] (slightly modified)
 */
private[server] object SocketOptionSettings {

  def fromSubConfig(c: Config): immutable.Seq[SocketOption] = {
    def so[T](setting: String)(f: (Config, String) ⇒ T)(cons: T ⇒ SocketOption): List[SocketOption] =
      c.getString(setting) match {
        case "undefined" ⇒ Nil
        case x ⇒ cons(f(c, setting)) :: Nil
      }

    so("so-receive-buffer-size")(_ getIntBytes _)(Inet.SO.ReceiveBufferSize) :::
      so("so-send-buffer-size")(_ getIntBytes _)(Inet.SO.SendBufferSize) :::
      so("so-reuse-address")(_ getBoolean _)(Inet.SO.ReuseAddress) :::
      so("so-traffic-class")(_ getInt _)(Inet.SO.TrafficClass) :::
      so("tcp-keep-alive")(_ getBoolean _)(Tcp.SO.KeepAlive) :::
      so("tcp-oob-inline")(_ getBoolean _)(Tcp.SO.OOBInline) :::
      so("tcp-no-delay")(_ getBoolean _)(Tcp.SO.TcpNoDelay)
  }
}

