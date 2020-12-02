/*
 * Copyright 2020 HM Revenue & Customs
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

package repositories

import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

import controllers.VoaPropertyLinkingSpec
import models.propertyrepresentation.{Start, StartJourney}
import org.scalatest.LoneElement
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

class PropertyLinkingSessionRepositorySpec extends VoaPropertyLinkingSpec with LoneElement {

  lazy val sessionRepository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("my-session")))
  val writes = implicitly[Writes[Start]]
  val reads = implicitly[Reads[Start]]

  "session repository" should "start by saving or updating data" in {
    val start = Start()

    sessionRepository.start(start)(writes, hc).futureValue

    val returnedSessionData: SessionData = sessionRepository.findAll().futureValue.loneElement // mustBe start

    inside(returnedSessionData) {
      case SessionData(_, data, createdAt) =>
        (data \ "propertyLinking" \ "status").as[String] mustBe start.status.name
        val aSecondAgo: Long = Instant.now().minus(1, SECONDS).toEpochMilli
        createdAt.value must be > aSecondAgo
    }

  }

  "session repository" should "get data from current session" in {
    val start = Start()
    sessionRepository.start(start)(writes, hc).futureValue

    val returnedSessionData: Option[Start] = sessionRepository.get[Start](reads, hc).futureValue

    inside(returnedSessionData) {
      case Some(Start(status)) => status.name mustBe StartJourney.name
    }

  }

  "session repository" should "remove data from current session" in {
    val start = Start()
    sessionRepository.start(start)(writes, hc).futureValue
    sessionRepository.remove()(hc).futureValue

    val returnedSessionData: Option[Start] = sessionRepository.get[Start](reads, hc).futureValue

    returnedSessionData mustBe None

  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    sessionRepository.removeAll().futureValue
  }
}
