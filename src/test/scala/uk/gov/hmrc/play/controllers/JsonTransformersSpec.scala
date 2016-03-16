package uk.gov.hmrc.play.controllers

import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.play.controllers.JsonTransformers.{EmptyToNullTransformer, TrimmerTransformer}

class JsonTransformersSpec extends WordSpecLike with Matchers {

  "TrimmerTransformer" should {

    trait Setup {
      val jsonString =
        """{
          | "name" : {
          |   "firstName" : "  John  ",
          |   "middleName" : "  ",
          |   "lastName" : "  Doe   "
          | },
          | "favouriteSports" : ["  Football  ", " Tennis  ", "", "  "]
          |}""".stripMargin
      val result = TrimmerTransformer()(Json.parse(jsonString))
    }

    "trim all string values in object" in new Setup {
      (result \ "name" \ "firstName").as[String] shouldBe "John"
      (result \ "name" \ "lastName").as[String] shouldBe "Doe"
    }

    "trim all string values in array" in new Setup {
      (result \ "favouriteSports")(0).as[String] shouldBe "Football"
      (result \ "favouriteSports")(1).as[String] shouldBe "Tennis"
    }
  }

  "EmptyToNullTransformer" should {

    trait Setup {
      val jsonString =
        """{
          | "name" : {
          |   "firstName" : "  John  ",
          |   "middleName" : "  ",
          |   "lastName" : "  Doe   "
          | },
          | "favouriteSports" : ["  Football  ", " Tennis  ", "", "  "]
          |}""".stripMargin
      val parse = Json.parse(jsonString)
      val result = EmptyToNullTransformer()(parse)
    }

    "convert empty/blank values to JsNull" in new Setup {
      (result \ "name" \ "middleName") shouldBe JsNull
      (result \ "favouriteSports")(2) shouldBe JsNull
      (result \ "favouriteSports")(3) shouldBe JsNull
    }
  }

}
