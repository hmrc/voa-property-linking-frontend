package base

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.BeforeAndAfterAll
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

trait ListYearsFeatureSwitchedISpecBase extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with BeforeAndAfterAll with TestData {
  sealed trait Language

  case object English extends Language

  case object Welsh extends Language

  val mockHost = "localhost"
  val mockPort = 11111

  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(mockPort))

  val mockedMicroservices: Set[String] = Set(
    "property-linking",
    "business-rates-authorisation",
    "auth"
  )

  val config: Map[String, String] = Map(
    "auditing.enabled"                      -> "false",
    "feature-switch.agentListYears.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "play.filters.csrf.header.bypassHeaders.X-Requested-With" -> "*"
  ) ++ mockedMicroservices.flatMap { serviceName =>
    Map(
      s"microservice.services.$serviceName.host" -> mockHost,
      s"microservice.services.$serviceName.port" -> mockPort.toString,
    )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build

  implicit val ws: WSClient = app.injector.instanceOf[WSClient]

  override def beforeAll(): Unit = {
    wireMockServer.start()
    configureFor(mockHost, mockPort)
  }

  override def afterAll(): Unit =
    wireMockServer.stop()

  def languageCookie(lang: Language): DefaultWSCookie = lang match {
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