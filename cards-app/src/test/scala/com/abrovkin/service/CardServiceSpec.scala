package com.abrovkin.service

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.abrovkin.model.UserId
import com.abrovkin.testdata.CardsTestData.*
import dev.profunktor.redis4cats.algebra.StringCommands
import io.circe.syntax.*
import org.http4s
import org.http4s.client.Client
import org.http4s.{EntityDecoder, Uri}
import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

class CardServiceSpec extends AsyncFlatSpec with Matchers with AsyncMockFactory with AsyncIOSpec:

  "getUserCards" should "return cards from external service and put them to cache for fallback" in
    testEnvironment { env =>
      import env.*

      (client.expect(_: Uri)(_: EntityDecoder[IO, String])) expects (getCardsPath(userId), *) returning IO(
        cards.asJson.noSpaces
      )
      cards.foreach(card => cardMasking.mask expects card returning card)
      (redis.set(_: String, _: String)) expects (userId, cards.asJson.noSpaces) returning IO(())

      service.getUserCards(userId).map(_ shouldBe cards)
    }

  it should "not fail if external service is available but cache is not" in
    testEnvironment { env =>
      import env.*

      (client
        .expect(_: Uri)(_: EntityDecoder[IO, String])) expects (getCardsPath(userId), *) returning IO(
        cards.asJson.noSpaces
      )

      cards.foreach(card => cardMasking.mask expects card returning card)
      (redis.set(_: String, _: String)) expects (userId, cards.asJson.noSpaces) returning IO.raiseError[Unit](
        new RuntimeException("Cache is not available")
      )

      service.getUserCards(userId).map(_ shouldBe cards)
    }

  it should "return cards from fallback cache if external service is unavailable" in
    testEnvironment { env =>
      import env.*

      (client.expect(_: Uri)(_: EntityDecoder[IO, String])) expects (getCardsPath(userId), *) returning IO.raiseError(
        new RuntimeException("Database is unavailable")
      )

      redis.get expects userId returning IO(Some(cards.asJson.noSpaces))

      service.getUserCards(userId).map(_ shouldBe cards)
    }

  def testEnvironment(f: TestEnvironment => IO[Unit]): IO[Unit] = f(new TestEnvironment {})

  trait TestEnvironment:

    given EntityDecoder[IO, String] = mock[EntityDecoder[IO, String]]

    val client             = mock[Client[IO]]
    val redis              = mock[StringCommands[IO, String, String]]
    val cardMasking        = mock[CardMasking]
    val externalServiceUrl = "http://cards-external-service.com"
    val service            = CardService(client, redis, cardMasking, externalServiceUrl)

    def getCardsPath(userId: UserId): Uri =
      Uri.unsafeFromString(s"$externalServiceUrl/users/$userId/cards")
