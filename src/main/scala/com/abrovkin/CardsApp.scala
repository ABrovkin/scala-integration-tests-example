package com.abrovkin

import cats.effect.*
import com.abrovkin.wirings.ProgramWiring.buildApp

object CardsApp extends IOApp.Simple:

  val run: IO[Unit] = buildApp[IO]("http://localhost:8081", "redis://localhost:6379", "0.0.0.0", 8080).use { server =>
    server.serve.compile.drain
  }
