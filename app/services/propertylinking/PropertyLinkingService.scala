/*
 * Copyright 2019 HM Revenue & Customs
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

package services.propertylinking

import actions.propertylinking.LinkingSessionRequest
import cats.data.EitherT
import connectors.propertyLinking.PropertyLinkConnector
import javax.inject.Inject
import models.propertylinking.payload.PropertyLinkPayload
import models.propertylinking.requests.PropertyLinkRequest
import services.BusinessRatesAttachmentService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.voa.propertylinking.exceptions.attachments.AttachmentException
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingService @Inject()(
                                      businessRatesAttachmentService: BusinessRatesAttachmentService,
                                      propertyLinkConnector: PropertyLinkConnector
                                      )(implicit executionContext: ExecutionContext) extends Cats {

  def submit(
              propertyLinkRequest: PropertyLinkRequest
            )(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): EitherT[Future, AttachmentException, Unit] =
    for {
      _ <- businessRatesAttachmentService.submit(propertyLinkRequest.submissionId, propertyLinkRequest.references)
      _ <- EitherT.liftF(propertyLinkConnector.createPropertyLink(PropertyLinkPayload(propertyLinkRequest)))
    } yield ()

}
