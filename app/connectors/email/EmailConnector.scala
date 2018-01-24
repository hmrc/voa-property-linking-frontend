package connectors.email

import config.WSHttp
import models.email.EmailRequest
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future

class EmailConnector(config: ServicesConfig, http: WSHttp) {

  private val serviceUrl = config.baseUrl("email-render")

  def send(emailRequest: EmailRequest): Future[Unit] =
    http
      .POST[EmailRequest, HttpResponse](s"$serviceUrl/hmrc/email", emailRequest) //Will change to VOA when confirmed.
      .map(_ => ())

}
