package com.abrovkin

import java.io.File
import cats.syntax.*
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.abrovkin.clients.cards.CardsAppClient
import com.abrovkin.clients.mockserver.MockServerClient
import com.abrovkin.clients.toxiproxy.ToxiProxyClient
import com.dimafeng.testcontainers.DockerComposeContainer
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.log4cats.*
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.Client
import org.scalatest.ParallelTestExecution
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

class CardsApiSpec extends AsyncFlatSpec with Matchers with TestContainersForAll with AsyncIOSpec:

  import CardsTestData.*

  "GET /api/v1/cards" should "return cards from external service and put them to cache for fallback" in
    testEnvironment { (mockServerClient, redisClient, _, appClient, _) =>
      for
        _ <- mockServerClient.stubGetCardsSuccess(userId, cards)
        _ <- appClient.getCards(userId).map(_ shouldBe cardsResponse)
        _ <- redisClient.get(userId).map(_ shouldBe Some(cardsResponse))
      yield ()
    }

  it should "return cards from fallback cache if external service is failed" in
    testEnvironment { (mockServerClient, redisClient, _, appClient, _) =>
      for
        _ <- redisClient.set(anotherUserId, anotherCardsResponse)
        _ <- mockServerClient.stubGetCardsFail(anotherUserId, 500)
        _ <- appClient.getCards(anotherUserId).map(_ shouldBe anotherCardsResponse)
        _ <- redisClient.get(anotherUserId).map(_ shouldBe Some(anotherCardsResponse))
      yield ()
    }

  it should "return cards from external service or empty list if is fails and skip Redis fails" in
    testEnvironment { (mockServerClient, _, toxiProxyClient, _, appClient) =>
      for
        _ <- toxiProxyClient.turnOffRedis()

        _ <- mockServerClient.stubGetCardsSuccess(userId, cards)
        _ <- appClient.getCards(userId).map(_ shouldBe cardsResponse)

        _ <- mockServerClient.stubGetCardsFail(anotherUserId, 500)
        _ <- appClient.getCards(anotherUserId).map(_ shouldBe emptyResponse)

        _ <- toxiProxyClient.turnOnRedis()
      yield ()
    }

  override type Containers = DockerComposeContainer

  override def startContainers(): Containers =
    DockerComposeContainer
      .Def(composeFiles = File("docker-compose.yml"))
      .start()

  def testEnvironment(
      f: (
          MockServerClient[IO],
          RedisCommands[IO, String, String],
          ToxiProxyClient[IO],
          CardsAppClient[IO],
          CardsAppClient[IO]
      ) => IO[Unit]
  ): IO[Unit] =
    given Logger[IO] = Slf4jLogger.getLogger[IO]

    val res = for
      given Client[IO]      <- BlazeClientBuilder[IO].resource
      redis                 <- Redis[IO].simple("redis://localhost:6379", RedisCodec.Utf8)
      mockServerClient       = MockServerClient[IO](8082)
      toxiProxyClient        = ToxiProxyClient[IO](8474)
      appClient              = CardsAppClient[IO](8080)
      appWithToxiProxyClient = CardsAppClient[IO](8081)
    yield f(mockServerClient, redis, toxiProxyClient, appClient, appWithToxiProxyClient)

    res.use(identity)
