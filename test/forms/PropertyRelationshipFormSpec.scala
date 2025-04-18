/*
 * Copyright 2024 HM Revenue & Customs
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

package forms

import controllers.propertyLinking.ClaimPropertyRelationship
import models._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.FormBindingVerification._

class PropertyRelationshipFormSpec extends AnyFlatSpec with Matchers {

  import TestData._

  behavior of "Property relationship form"

  it should "bind when the inputs are all valid" in
    shouldBindTo(form, validData, PropertyRelationship(Occupier, 1L))

  it should "mandate a capacity" in
    verifyMandatoryMultiChoice(form, validData, "capacity")

  it should "only allow recognised capacity values" in
    verifyMultiChoice(form, validData, "capacity", CapacityType)

  object TestData {
    val form = ClaimPropertyRelationship.relationshipForm
    val validData = Map(
      "capacity" -> Occupier.name,
      "uarn"     -> "1"
    )
  }

}
