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

package services.email

import connectors.email.EmailConnector

import javax.inject.Inject
import models.email.EmailRequest
import models.{DetailedIndividualAccount, GroupAccount}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EmailService @Inject()(emailConnector: EmailConnector) {

  def sendNewRegistrationSuccess(
        to: String,
        detailedIndividualAccount: DetailedIndividualAccount,
        groupAccount: Option[GroupAccount],
        affinityGroupOpt: Option[AffinityGroup])(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Unit] =
    send(EmailRequest.registration(to, detailedIndividualAccount, groupAccount, affinityGroupOpt))

  private def send(emailRequest: EmailRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext) =
    emailConnector
      .send(emailRequest)
      .recover { case _: Throwable => () }
}
