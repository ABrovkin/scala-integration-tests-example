package com.abrovkin.clients.toxiproxy

import cats.{Applicative, Defer}
import cats.syntax.all.*
import eu.rekawek.toxiproxy.{ToxiproxyClient => JavaToxiproxyClient}

trait ToxiProxyClient[F[_]]:
  def turnOffRedis(): F[Unit]
  def turnOnRedis(): F[Unit]

object ToxiProxyClient:

  private class Impl[F[_]: Applicative: Defer](port: Int) extends ToxiProxyClient[F]:

    private val client = new JavaToxiproxyClient("localhost", port)

    override def turnOffRedis(): F[Unit] =
      Defer[F].defer {
        client.getProxy("redis-proxy").disable().pure[F]
      }

    override def turnOnRedis(): F[Unit] =
      Defer[F].defer {
        client.getProxy("redis-proxy").enable().pure[F]
      }

  def apply[F[_]: Applicative: Defer](port: Int): ToxiProxyClient[F] =
    new Impl[F](port)
