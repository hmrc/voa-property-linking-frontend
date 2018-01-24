package services.email

import javax.inject.Inject

import connectors.email.EmailConnector
import models.email.EmailRequest


class EmailService @Inject()(emailConnector: EmailConnector){

  def sendEnrolmentSuccess(to: String, personId: String) =
    emailConnector
      .send(EmailRequest(to, personId))

}
