package com.abrovkin.clients.cards

import cats.MonadThrow
import cats.effect.Temporal
import cats.syntax.all.*
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}

import java.net.SocketException
import scala.concurrent.duration.*

trait CardsAppClient[F[_]]:
  def getCards(userId: String): F[String]

object CardsAppClient:

  type StringDecoder[F[_]] = EntityDecoder[F, String]

  private def getCardsRelativePath(userId: String): String = s"/api/v1/cards?userId=$userId"

  private class Impl[F[_]: Temporal: MonadThrow: StringDecoder](client: Client[F], port: Int) extends CardsAppClient[F]:

    private def getCardsUri(userId: String): Uri =
      Uri.unsafeFromString(s"http://localhost:$port${getCardsRelativePath(userId)}")

    override def getCards(userId: String): F[String] =

      // Самопальный ad-hoc retry сделан для того, чтобы обойти проблему долгого старта
      // Контейнеры в тестах стартуют достаточно долго
      // Из-за этого при запуске тестов несколько первых раз возникает SocketException
      // Простой retry позволяет снизить хрупкость тестов
      // По-хорошему это должно быть реализовано через стратегию ожидания старта контейнеров
      def retries(attempts: Int): F[String] =
        client
          .expect[String](getCardsUri(userId))
          .handleErrorWith {
            case _: SocketException if attempts > 0 =>
              Temporal[F]
                .sleep(1.second)
                .flatMap(_ => retries(attempts - 1))

            case e =>
              MonadThrow[F].raiseError(e)
          }

      retries(10)

  def apply[F[_]: Temporal: Client: MonadThrow: StringDecoder](port: Int): CardsAppClient[F] =
    val client = summon[Client[F]]
    new Impl[F](client, port)
