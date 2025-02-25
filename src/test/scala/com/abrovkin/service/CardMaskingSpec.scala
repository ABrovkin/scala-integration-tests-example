package com.abrovkin.service

import com.abrovkin.testdata.CardsTestData._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CardMaskingSpec extends AnyFlatSpec with Matchers:

  "mask" should "return a copy of Card object with masked card number" in {
    CardMaskingImpl.mask(card) shouldBe maskedCard
  }
