package actions


import auth.{GGActionEnrolment, GovernmentGatewayProvider}
import connectors.VPLAuthConnector
import models.enrolment.{UserDetails, UserInfo}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.test.UnitSpec
import utils.NoMetricsOneAppPerSuite

import scala.concurrent.Future

class VOAActionSpec extends UnitSpec with MockitoSugar with NoMetricsOneAppPerSuite {

  override val additionalAppConfig: Seq[(String, String)] = Seq("featureFlags.enrolment" -> "false")

  "when false provided but session is not stored, goes through process of getting details again" in {
    when(mockVplAuthConnector.getUserDetails(any())).thenReturn(Future.successful(UserDetails("123456", UserInfo(None, None, "", None, "", Individual))))
    println(Individual.toJson)
    val res = testActionEnrolment.async(false) { _ =>
      _ =>
        Future.successful(Ok("something"))
    }(FakeRequest())

    status(res) shouldBe OK
    contentAsString(res) shouldBe "something"
  }

  lazy val testActionEnrolment = new GGActionEnrolment(mockGG, mockAuthConnector, mockVplAuthConnector)
  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockVplAuthConnector = mock[VPLAuthConnector]
  lazy val mockGG = mock[GovernmentGatewayProvider]

}
