package com.abrovkin

import io.circe.Json
import io.circe.syntax.*

object CardsTestData:
  val cards = Json
    .arr(
      Json.obj(
        "ucid"   -> "123".asJson,
        "number" -> "5555-3333-2213-1231".asJson,
        "amount" -> 10000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1234".asJson,
        "number" -> "5455-3333-2213-1231".asJson,
        "amount" -> 12000.0.asJson
      )
    )
    .noSpaces

  val cardsResponse = Json
    .arr(
      Json.obj(
        "ucid"   -> "123".asJson,
        "number" -> "5555-3******13-1231".asJson,
        "amount" -> 10000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1234".asJson,
        "number" -> "5455-3******13-1231".asJson,
        "amount" -> 12000.0.asJson
      )
    )
    .noSpaces

  val userId = "The-User"

  val anotherCards = Json
    .arr(
      Json.obj(
        "ucid"   -> "123".asJson,
        "number" -> "5555-3333-2213-1231".asJson,
        "amount" -> 10000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1234".asJson,
        "number" -> "5455-3333-2213-1231".asJson,
        "amount" -> 12000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1432".asJson,
        "number" -> "5445-3333-2213-1231".asJson,
        "amount" -> 65000.0.asJson
      )
    )
    .noSpaces

  val anotherCardsResponse = Json
    .arr(
      Json.obj(
        "ucid"   -> "123".asJson,
        "number" -> "5555-3******13-1231".asJson,
        "amount" -> 10000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1234".asJson,
        "number" -> "5455-3******13-1231".asJson,
        "amount" -> 12000.0.asJson
      ),
      Json.obj(
        "ucid"   -> "1432".asJson,
        "number" -> "5445-3******13-1231".asJson,
        "amount" -> 65000.0.asJson
      )
    )
    .noSpaces

  val anotherUserId = "The-User-2"

  val emptyResponse = Json.arr().noSpaces
