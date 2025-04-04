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

package base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{DefaultWSCookie, WSClient, WSCookie}
import play.api.mvc.{Cookie, Session, SessionCookieBaker}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

trait ISpecBase
    extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with BeforeAndAfterAll with BeforeAndAfterEach
    with CommonStubs {
  sealed trait Language

  case object English extends Language

  case object Welsh extends Language

  val mockHost = "localhost"
  val mockPort = 11111

  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(mockPort))

  lazy val extraConfig: Map[String, Any] = Map.empty

  val config: Map[String, Any] = Map(
    "auditing.enabled"                                        -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token"       -> "nocheck",
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*",
    "microservice.services.vmv.host"                          -> mockHost,
    "microservice.services.vmv.port"                          -> mockPort.toString,
    "microservice.services.property-linking.host"             -> mockHost,
    "microservice.services.property-linking.port"             -> mockPort.toString,
    "microservice.services.business-rates-authorisation.host" -> mockHost,
    "microservice.services.business-rates-authorisation.port" -> mockPort.toString,
    "microservice.services.business-rates-attachments.host"   -> mockHost,
    "microservice.services.business-rates-attachments.port"   -> mockPort.toString,
    "microservice.services.auth.host"                         -> mockHost,
    "microservice.services.auth.port"                         -> mockPort.toString,
    "microservice.services.vmv.host"                          -> mockHost,
    "microservice.services.vmv.port"                          -> mockPort.toString,
    "business-rates-dashboard-frontend.url"                   -> "/business-rates-dashboard",
    "microservice.services.business-rates-attachments.host"   -> mockHost,
    "microservice.services.business-rates-attachments.port"   -> mockPort.toString,
    "microservice.services.business-rates-check.host"         -> mockHost,
    "microservice.services.business-rates-check.port"         -> mockPort.toString,
    "microservice.services.business-rates-challenge.host"     -> mockHost,
    "microservice.services.business-rates-challenge.port"     -> mockPort.toString,
    "feature-switch.agentJourney2026Enabled"                  -> false
  ) ++ extraConfig

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  implicit val ws: WSClient = app.injector.instanceOf[WSClient]

  override def beforeAll(): Unit = {
    wireMockServer.start()
    configureFor(mockHost, mockPort)
  }

  override def beforeEach(): Unit =
    wireMockServer.resetAll()

  override def afterAll(): Unit =
    wireMockServer.stop()

  def languageCookie(lang: Language): DefaultWSCookie =
    lang match {
      case English => DefaultWSCookie("PLAY_LANG", "en")
      case Welsh   => DefaultWSCookie("PLAY_LANG", "cy")
    }

  def getSessionCookie(testSessionId: String): WSCookie = {

    def makeSessionCookie(session: Session): Cookie = {
      val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
      val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
      val sessionCookie = cookieBaker.encodeAsCookie(session)
      val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
      sessionCookie.copy(value = encryptedValue.value)
    }

    val mockSession = Session(
      Map(
        SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
        SessionKeys.authToken            -> "mock-bearer-token",
        SessionKeys.sessionId            -> testSessionId
      )
    )

    val cookie = makeSessionCookie(mockSession)

    DefaultWSCookie(
      name = cookie.name,
      value = cookie.value,
      domain = cookie.domain,
      path = Some(cookie.path),
      maxAge = cookie.maxAge.map(_.toLong),
      secure = cookie.secure,
      httpOnly = cookie.httpOnly
    )
  }
}
