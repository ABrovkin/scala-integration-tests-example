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

  type StringDecoder[F[_]] = EntityDecoder[F, String]

  def getCardsRelativePath(userId: String): String = s"/users/$userId/cards"

  private class Impl[F[_]: MonadThrow: StringDecoder](
      client: Client[F],
      redisCommands: StringCommands[F, String, String],
      masking: CardMasking,
      baseUri: String
  ) extends CardService[F]:

    override def getUserCards(userId: UserId): F[List[Card]] =
      val uri              = Uri.unsafeFromString(s"$baseUri${getCardsRelativePath(userId)}")
      val getAndCacheCards = for
        cards      <- client
                        .expect[String](uri)
                        .map(decode[List[Card]])
                        .map(_.getOrElse(List.empty))
        maskedCards = cards.map(masking.mask)
        _          <- putUserCardsInternal(userId, maskedCards).handleError(_ => ())
      yield maskedCards

      getAndCacheCards
        .handleErrorWith(_ => getUserCardsInternal(userId))
        .handleError(_ => List.empty)

    private def getUserCardsInternal(userId: UserId): F[List[Card]] =
      redisCommands.get(userId).map {
        case Some(value) => decode[List[Card]](value).getOrElse(List.empty)
        case None        => List.empty
      }

    private def putUserCardsInternal(userId: UserId, cards: List[Card]): F[Unit] =
      redisCommands.set(userId, cards.asJson.noSpaces)

  def apply[F[_]: MonadThrow: StringDecoder](
      externalService: Client[F],
      redisCommands: StringCommands[F, String, String],
      masking: CardMasking,
      baseUri: String
  ): CardService[F] =
    new Impl[F](externalService, redisCommands, masking, baseUri)
