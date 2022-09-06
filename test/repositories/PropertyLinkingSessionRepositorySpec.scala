/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

class PropertyLinkingSessionRepositorySpec extends VoaPropertyLinkingSpec with LoneElement {

  lazy val repository = app.injector.instanceOf[PropertyLinkingSessionRepository]
  val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("my-session")))
  val writes = implicitly[Writes[Start]]
  val reads = implicitly[Reads[Start]]

  "session repository" should "start by saving or updating data" in {
    val start = Start(backLink = None)

    repository.start(start)(writes, hc).futureValue

    val returnedSessionData: SessionData = repository.findFirst.futureValue // shouldBe start

    inside(returnedSessionData) {
      case SessionData(_, data, createdAt) =>
        (data \ "propertyLinking" \ "status").as[String] shouldBe start.status.name
        val aSecondAgo: Long = Instant.now().minus(1, SECONDS).toEpochMilli
        createdAt.toEpochMilli should be > aSecondAgo
    }

  }

  "session repository" should "get data from current session" in {
    val start = Start(backLink = None)
    repository.start(start)(writes, hc).futureValue

    val returnedSessionData: Option[Start] = repository.get[Start](reads, hc).futureValue

    inside(returnedSessionData) {
      case Some(Start(status, _)) => status.name shouldBe StartJourney.name
    }

  }

  "session repository" should "remove data from current session" in {
    val start = Start(backLink = None)
    repository.start(start)(writes, hc).futureValue
    repository.remove()(hc).futureValue

    val returnedSessionData: Option[Start] = repository.get[Start](reads, hc).futureValue

    returnedSessionData shouldBe None

  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    repository.removeAll().futureValue
  }
}
