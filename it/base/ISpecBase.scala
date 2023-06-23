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
import play.api.libs.ws.WSClient

trait ISpecBase extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with BeforeAndAfterAll {
  sealed trait Language

  case object English extends Language

  case object Welsh extends Language

  val mockHost = "localhost"
  val mockPort = 11111

  lazy val wireMockServer: WireMockServer = new WireMockServer(wireMockConfig().port(mockPort))

  val mockedMicroservices: Set[String] = Set(
    "property-linking",
  )

  val config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
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
}
