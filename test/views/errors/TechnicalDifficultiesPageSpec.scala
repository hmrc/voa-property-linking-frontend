/*
 * Copyright 2018 HM Revenue & Customs
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

package views.errors

import java.time.LocalDateTime

import play.api.i18n.Messages
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import resources._
import uk.gov.hmrc.play.test.UnitSpec

class TechnicalDifficultiesPageSpec extends UnitSpec {

  implicit val app = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> "false")
    .build()

  "The technical difficulties page" should {
    val ref: String = shortString
    val html = views.html.errors.technicalDifficulties(Some(ref), LocalDateTime.of(2017, 4, 1, 9, 30))(FakeRequest(), Messages.Implicits.applicationMessages)
    val page = html.toString

    "display an error reference number" in {
      page should include (s"Reference number: $ref")
    }

    "display the current date" in {
      page should include ("Date: 1 April 2017")
    }

    "display the current time in 12 hour format" in {
      page should include ("Time: 09:30 AM")
    }
  }

}
