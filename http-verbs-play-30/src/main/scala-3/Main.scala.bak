/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

object Hello {
  def main(args: Array[String]) = {
    println("Hello, world")

    println(typeOf[Map[String, String]])
    println(typeOf[Map[String, Seq[String]]])
    println(typeOf[Map[String, List[String]]])
    println(typeOf[Map[String, String]])
    println(typeOf[Map[String, Seq[String]]])
    println(typeOf[Map[String, List[String]]])


    println(unapply1(Map("k1" -> Seq("v1"))))
    println(unapply1(Map("k1" -> List("v1"))))
    println(unapply1(Map("k1" -> "v1")))
    println(unapply1(""))

    println(unapply(Map("k1" -> Seq("v1"))))
    println(unapply(Map("k1" -> List("v1"))))
    println(unapply(Map("k1" -> "v1")))
    println(unapply(""))
  }

  // calling typeOf directly works, but not indirectly. The following reports `B`:
  def typeOf[B]: String =
    uk.gov.hmrc.http.TypeUtil2.typeOf[B]

  // calling unapply directly works, but not indirectly. The following resolves to `B` so always says `None`
  def unapply1[B](b: B) = uk.gov.hmrc.http.TypeUtil2.IsMap1.unapply[B](b)

    // Works with
    // but we get warning "the type test for Map[String, Seq[String]] cannot be checked at runtime because its type arguments can't be determined from B"
  def unapply[B](b: B) = uk.gov.hmrc.http.TypeUtil2.IsMap.unapply[B](b)

}
