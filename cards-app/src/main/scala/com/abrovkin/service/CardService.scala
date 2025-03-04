package com.abrovkin.service

import cats.MonadThrow
import cats.syntax.all.*
import com.abrovkin.model.{Card, UserId}
import dev.profunktor.redis4cats.algebra.StringCommands
import io.circe.parser.*
import io.circe.syntax.*
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}

trait CardService[F[_]]:
  def getUserCards(userId: UserId): F[List[Card]]

object CardService:

  private type StringDecoder[F[_]] = EntityDecoder[F, String]
  private type Redis[F[_]]         = StringCommands[F, String, String]

  private class Impl[F[_]: MonadThrow: StringDecoder: Client: Redis](
      masking: CardMasking,
      baseUri: String
  ) extends CardService[F]:

    private val client = summon[Client[F]]
    private val redis  = summon[Redis[F]]

    private def getCardsPath(userId: String): Uri =
      Uri.unsafeFromString(s"$baseUri/users/$userId/cards")

    override def getUserCards(userId: UserId): F[List[Card]] =
      val getAndCacheCards = for
        cards      <- client
                        .expect[String](getCardsPath(userId))
                        .map(decode[List[Card]])
                        .map(_.getOrElse(List.empty))
        maskedCards = cards.map(masking.mask)
        _          <- putUserCardsToCache(userId, maskedCards).handleError(_ => ())
      yield maskedCards

      getAndCacheCards
        .handleErrorWith(_ => getUserCardsFromCache(userId))
        .handleError(_ => List.empty)

    private def getUserCardsFromCache(userId: UserId): F[List[Card]] =
      redis.get(userId).map {
        case Some(value) => decode[List[Card]](value).getOrElse(List.empty)
        case None        => List.empty
      }

    private def putUserCardsToCache(userId: UserId, cards: List[Card]): F[Unit] =
      redis.set(userId, cards.asJson.noSpaces)

  def apply[F[_]: MonadThrow: StringDecoder](
      externalService: Client[F],
      redisCommands: StringCommands[F, String, String],
      masking: CardMasking,
      baseUri: String
  ): CardService[F] =
    given Client[F] = externalService
    given Redis[F]  = redisCommands

    new Impl[F](masking, baseUri)
