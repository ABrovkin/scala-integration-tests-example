package com.abrovkin.http

import cats.syntax.all.*
import cats.Monad
import io.circe.Encoder
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.*
import com.abrovkin.service.CardService
import com.abrovkin.model.UserId

trait Controller[F[_]]:
  def mkCardsController: HttpApp[F]

object Controller:

  private class Impl[F[_]: Monad](cardService: CardService[F]) extends Controller[F] with Http4sDsl[F]:

    private object UserIdQueryParamMatcher extends QueryParamDecoderMatcher[String]("userId")

    override def mkCardsController: HttpApp[F] =
      HttpRoutes
        .of[F] { case GET -> Root / "api" / "v1" / "cards" :? UserIdQueryParamMatcher(userId) =>
          cardService.getUserCards(UserId(userId)).flatMap { userCards =>
            Ok(userCards.asJson)
          }
        }
        .orNotFound

  def apply[F[_]: Monad](cardService: CardService[F]): Controller[F] = new Impl[F](cardService)
