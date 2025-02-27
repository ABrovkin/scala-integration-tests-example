package com.abrovkin.clients.mockserver

import cats.{Applicative, Defer}
import cats.syntax.all.*
import org.mockserver.client.{MockServerClient => JavaMockServerClient}
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

trait MockServerClient[F[_]]:
  def stubGetCardsSuccess(userId: String, cards: String): F[Unit]
  def stubGetCardsFail(userId: String, errorCode: Int): F[Unit]

object MockServerClient:

  private def getCardsRelativePath(userId: String): String = s"/users/$userId/cards"

  private class Impl[F[_]: Applicative: Defer](port: Int) extends MockServerClient[F]:

    override def stubGetCardsSuccess(userId: String, cards: String): F[Unit] =
      Defer[F].defer {
        new JavaMockServerClient("localhost", port)
          .when(
            request()
              .withMethod("GET")
              .withPath(getCardsRelativePath(userId))
          )
          .respond(response().withBody(cards))
          .pure[F]
          .map(_ => ())
      }

    override def stubGetCardsFail(userId: String, errorCode: Int): F[Unit] =
      Defer[F].defer {
        new JavaMockServerClient("localhost", port)
          .when(
            request()
              .withMethod("GET")
              .withPath(getCardsRelativePath(userId))
          )
          .respond(response().withStatusCode(500))
          .pure[F]
          .map(_ => ())
      }

  def apply[F[_]: Applicative: Defer](port: Int): MockServerClient[F] = new Impl[F](port)
