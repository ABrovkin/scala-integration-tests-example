package com.abrovkin.wirings

import java.time.Duration

import cats.effect.{Async, Resource}
import com.abrovkin.cache.CardsCache
import com.abrovkin.config.ServiceConfig
import com.abrovkin.external.CardsExternalService
import com.abrovkin.service.{CardMaskingImpl, CardService}
import com.abrovkin.http.Controller
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import dev.profunktor.redis4cats.log4cats.*
import io.lettuce.core.{ClientOptions, TimeoutOptions}
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.ServerBuilder
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pureconfig.ConfigSource

object ProgramWiring:

  def redis[F[_]: Async](redisUri: String, timeout: Int): Resource[F, RedisCommands[F, String, String]] =
    given Logger[F] = Slf4jLogger.getLogger[F]

    val opts = ClientOptions
      .builder()
      .timeoutOptions(
        TimeoutOptions
          .builder()
          .fixedTimeout(Duration.ofMillis(timeout))
          .build()
      )
      .build()
    Redis[F].withOptions(redisUri, opts, RedisCodec.Utf8)

  def wire[F[_]: Async](externalServiceUri: String, redisUri: String): Resource[F, CardService[F]] =
    for
      httpClient     <- BlazeClientBuilder[F].resource
      externalService = CardsExternalService(httpClient, externalServiceUri)
      redisCommands  <- redis(redisUri, 200)
      cache           = CardsCache(redisCommands)
      service         = CardService(externalService, cache, CardMaskingImpl)
      controller      = Controller(service)
    yield service

  def buildApp[F[_]: Async](): Resource[F, ServerBuilder[F]] =
    for
      httpClient     <- BlazeClientBuilder[F].resource
      config          = ConfigSource.default.loadOrThrow[ServiceConfig]
      externalService = CardsExternalService(httpClient, config.externalServiceConfig.externalServiceUri)
      redisUri        = s"redis://${config.redisConfig.redisHost}:${config.redisConfig.redisPort}"
      redisCommands  <- redis(redisUri, config.redisConfig.timeout)
      cache           = CardsCache(redisCommands)
      service         = CardService(externalService, cache, CardMaskingImpl)
      controller      = Controller(service)
      httpApp         = controller.mkCardsController
      serverBuilder   = BlazeServerBuilder[F]
                          .bindHttp(config.httpPort, config.httpHost)
                          .withHttpApp(httpApp)
    yield serverBuilder
