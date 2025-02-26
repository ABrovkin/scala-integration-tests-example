package com.abrovkin

import cats.effect.*
import com.abrovkin.wirings.ProgramWiring.buildApp

object CardsApp extends IOApp.Simple:

  val run: IO[Unit] = buildApp[IO]().use { server =>
    server.serve.compile.drain
  }
