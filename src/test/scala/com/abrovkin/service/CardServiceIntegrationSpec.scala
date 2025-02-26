package com.abrovkin.service

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import cats.syntax.all.*
import com.abrovkin.cache.RedisConfig
import com.abrovkin.config.ServiceConfig
import com.abrovkin.external.ExternalServiceConfig
import com.abrovkin.model.Card
import com.abrovkin.testdata.CardsTestData.*
import com.abrovkin.utils.MockServerClientWrapper
import com.abrovkin.wirings.ProgramWiring
import com.dimafeng.testcontainers.lifecycle.and
import com.dimafeng.testcontainers.{MockServerContainer, RedisContainer, ToxiproxyContainer}
import com.dimafeng.testcontainers.scalatest.TestContainersForAll
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import io.circe.syntax.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.client.dsl.io.*
import org.http4s.Method.*
import org.http4s.syntax.all.*
import org.http4s.implicits.*
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.testcontainers.containers.Network

class CardServiceIntegrationSpec extends AsyncFlatSpec with Matchers with TestContainersForAll with AsyncIOSpec:

  override type Containers = MockServerContainer and RedisContainer and ToxiproxyContainer

  "GET /api/v1/cards" should "return cards from external service and put them to cache for fallback" in
    testEnvironment { (mockServer, redis, client) =>
      for
        _ <- MockServerClientWrapper.mockGetCards(
               mockServer,
               userId,
               cards.asJson.noSpaces
             )

        request      = GET(Uri.unsafeFromString(s"/api/v1/cards?userId=$userId"))
        responseRaw <- client.expect[String](request)
        response     = decode[List[Card]](responseRaw).getOrElse(List.empty)
        _            = response shouldBe cardsResponse

        cachedCards <- redis.get(userId)
        _            = cachedCards shouldBe Some(cardsResponse.asJson.noSpaces)
      yield ()
    }

  it should "return cards from fallback cache if external service is failed" in
    testEnvironment { (mockServer, redis, client) =>
      for
        _ <- redis.set(anotherUserId, anotherCardsResponse.asJson.noSpaces)
        _ <- MockServerClientWrapper.mockFailGetCards(mockServer, anotherUserId)

        request      = GET(Uri.unsafeFromString(s"/api/v1/cards?userId=$anotherUserId"))
        responseRaw <- client.expect[String](request)
        response     = decode[List[Card]](responseRaw).getOrElse(List.empty)
        _            = response shouldBe anotherCardsResponse

        cachedCards <- redis.get(anotherUserId)
        _            = cachedCards shouldBe Some(anotherCardsResponse.asJson.noSpaces)
      yield ()
    }

  it should "return cards from external service or empty list if is fails and skip Redis fails" in
    testEnvironmentWithProxy { (mockServer, redisProxy, client) =>
      for
        _ <- MockServerClientWrapper.mockGetCards(
               mockServer,
               userId,
               cards.asJson.noSpaces
             )
        _ <- IO(redisProxy.setConnectionCut(true))

        request      = GET(Uri.unsafeFromString(s"/api/v1/cards?userId=$userId"))
        responseRaw <- client.expect[String](request)
        response     = decode[List[Card]](responseRaw).getOrElse(List.empty)
        _            = response shouldBe cardsResponse

        _ <- MockServerClientWrapper.mockFailGetCards(mockServer, anotherUserId)

        request      = GET(Uri.unsafeFromString(s"/api/v1/cards?userId=$anotherUserId"))
        responseRaw <- client.expect[String](request)
        response     = decode[List[Card]](responseRaw).getOrElse(List.empty)
        _            = response shouldBe List.empty

        _ <- IO(redisProxy.setConnectionCut(false))
      yield ()
    }

  override def startContainers(): Containers =
    val mockServer = MockServerContainer.Def("5.15.0").start()
    val network    = Network.newNetwork()
    val redis      = new RedisContainer("redis:latest")
      .configure(
        _.withNetwork(network)
          .withExposedPorts(6379)
          .withNetworkAliases("redis")
      )
    val toxiproxy  = new ToxiproxyContainer("shopify/toxiproxy:2.1.4")
      .configure(_.withNetwork(network))

    redis.start()
    toxiproxy.start()

    mockServer and redis and toxiproxy

  def testEnvironment(
      f: (MockServerContainer, RedisCommands[IO, String, String], Client[IO]) => IO[Unit]
  ): IO[Unit] =
    withContainers { case mockServer and redis and _ =>
      val config = ServiceConfig(
        httpHost = "0.0.0.0",
        httpPort = 8080,
        redisConfig = RedisConfig("localhost", redis.container.getMappedPort(6379), 200),
        externalServiceConfig = ExternalServiceConfig(mockServer.endpoint)
      )

      val redisResource      = ProgramWiring.redis[IO](redis.redisUri, 200)
      val controllerResource = ProgramWiring.buildController[IO](config)
      (redisResource product controllerResource).use { case (redis, controller) =>
        val client: Client[IO] = Client.fromHttpApp(controller.orNotFound)
        f(mockServer, redis, client)
      }
    }

  def testEnvironmentWithProxy(
      f: (MockServerContainer, ToxiproxyContainer.ContainerProxy, Client[IO]) => IO[Unit]
  ): IO[Unit] =
    withContainers { case mockServer and _ and _ =>
      val config             = ServiceConfig(
        httpHost = "0.0.0.0",
        httpPort = 8080,
        redisConfig = RedisConfig(redisProxy.getContainerIpAddress, redisProxy.getProxyPort, 200),
        externalServiceConfig = ExternalServiceConfig(mockServer.endpoint)
      )
      val controllerResource = ProgramWiring.buildController[IO](config)
      controllerResource.use { controller =>
        val client: Client[IO] = Client.fromHttpApp(controller.orNotFound)
        f(mockServer, redisProxy, client)
      }
    }

  private lazy val redisProxy = withContainers { case _ and _ and toxiproxy =>
    toxiproxy.proxy("redis", 6379)
  }
