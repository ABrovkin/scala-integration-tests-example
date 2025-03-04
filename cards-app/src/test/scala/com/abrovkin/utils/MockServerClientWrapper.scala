package com.abrovkin.utils

import cats.effect.IO
import com.dimafeng.testcontainers.MockServerContainer
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest.request
import org.mockserver.model.HttpResponse.response

object MockServerClientWrapper:

  def getCardsRelativePath(userId: String): String = s"/users/$userId/cards"

  def mockGetCards(mockServer: MockServerContainer, userId: String, cards: String): IO[Unit] = IO {
    new MockServerClient("localhost", mockServer.serverPort)
      .when(
        request()
          .withMethod("GET")
          .withPath(getCardsRelativePath(userId))
      )
      .respond(response().withBody(cards))
  }

  def mockFailGetCards(mockServer: MockServerContainer, userId: String): IO[Unit] = IO {
    new MockServerClient("localhost", mockServer.serverPort)
      .when(
        request()
          .withMethod("GET")
          .withPath(getCardsRelativePath(userId))
      )
      .respond(response().withStatusCode(500))
  }
