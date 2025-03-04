package com.abrovkin.service

import cats.MonadThrow
import cats.syntax.all.*
import com.abrovkin.cache.CardsCache
import com.abrovkin.model.{Card, UserId}
import io.circe.parser.*
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}

trait CardService[F[_]]:
  def getUserCards(userId: UserId): F[List[Card]]

object CardService:

  type StringDecoder[F[_]] = EntityDecoder[F, String]

  def getCardsRelativePath(userId: String): String = s"/users/$userId/cards"

  private class Impl[F[_]: MonadThrow: StringDecoder](
      client: Client[F],
      cache: CardsCache[F],
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
        _          <- cache.putUserCards(userId, maskedCards).handleError(_ => ())
      yield maskedCards
      getAndCacheCards
        .handleErrorWith(_ => cache.getUserCards(userId))
        .handleError(_ => List.empty)

  def apply[F[_]: MonadThrow: StringDecoder](
      externalService: Client[F],
      cache: CardsCache[F],
      masking: CardMasking,
      baseUri: String
  ): CardService[F] =
    new Impl[F](externalService, cache, masking, baseUri)
