package com.abrovkin.clients.cards

import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}

trait CardsAppClient[F[_]]:
  def getCards(userId: String): F[String]

object CardsAppClient:

  type StringDecoder[F[_]] = EntityDecoder[F, String]

  private def getCardsRelativePath(userId: String): String = s"/api/v1/cards?userId=$userId"

  private class Impl[F[_]: StringDecoder](client: Client[F], port: Int) extends CardsAppClient[F]:

    private def getCardsUri(userId: String): Uri =
      Uri.unsafeFromString(s"http://localhost:$port${getCardsRelativePath(userId)}")

    override def getCards(userId: String): F[String] =
      client.expect[String](getCardsUri(userId))

  def apply[F[_]: Client: StringDecoder](port: Int): CardsAppClient[F] =
    val client = summon[Client[F]]
    new Impl[F](client, port)
