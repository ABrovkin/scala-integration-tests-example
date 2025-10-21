package com.abrovkin.http

import cats.effect.kernel.Async
import cats.syntax.all.*
import com.abrovkin.model.{UserId, CardUcid, CardNumber, Card}
import com.abrovkin.service.CardService
import io.circe.generic.auto.*
import org.http4s.HttpRoutes
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.circe.*
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

trait CardsController[F[_]]:
  def routes: HttpRoutes[F]

object CardsController:

  private class Impl[F[_]: Async](cardService: CardService[F]) extends CardsController[F]:

    // Implicit schemas for opaque types
    given Schema[UserId]     = Schema.schemaForString.as[UserId]
    given Schema[CardUcid]   = Schema.schemaForString.as[CardUcid]
    given Schema[CardNumber] = Schema.schemaForString.as[CardNumber]

    // Define the endpoint using tapir
    private val getUserCardsEndpoint: PublicEndpoint[String, Unit, List[com.abrovkin.model.Card], Any] =
      endpoint.get
        .in("api" / "v1" / "cards")
        .in(query[String]("userId"))
        .out(jsonBody[List[com.abrovkin.model.Card]])
        .description("Get user cards by user ID")
        .name("getUserCards")

    // Convert the endpoint to http4s routes
    private val getUserCardsRoute: HttpRoutes[F] =
      Http4sServerInterpreter[F]().toRoutes(
        getUserCardsEndpoint.serverLogicSuccess { userId =>
          cardService.getUserCards(UserId(userId))
        }
      )

    // Create OpenAPI documentation
    private val swaggerRoutes: HttpRoutes[F] = {
      val swaggerEndpoints = SwaggerInterpreter()
        .fromEndpoints[F](
          List(getUserCardsEndpoint),
          "Cards API",
          "1.0"
        )

      Http4sServerInterpreter[F]().toRoutes(swaggerEndpoints)
    }

    override def routes: HttpRoutes[F] = swaggerRoutes <+> getUserCardsRoute

  def apply[F[_]: Async](cardService: CardService[F]): CardsController[F] = new Impl[F](cardService)
