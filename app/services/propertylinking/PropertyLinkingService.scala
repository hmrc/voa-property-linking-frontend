/*
 * Copyright 2021 HM Revenue & Customs
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

import actions.propertylinking.requests.LinkingSessionRequest
import cats.data.EitherT
import connectors.propertyLinking.PropertyLinkConnector
import models.ListType
import models.properties.ValuationStatus

import javax.inject.Inject
import models.propertylinking.payload.PropertyLinkPayload
import models.propertylinking.requests.PropertyLinkRequest
import play.api.Logger
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.{HeaderCarrier}
import uk.gov.hmrc.propertylinking.exceptions.attachments.AttachmentException
import utils.Cats

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingService @Inject()(
      businessRatesAttachmentService: BusinessRatesAttachmentsService,
      propertyLinkConnector: PropertyLinkConnector
)(implicit executionContext: ExecutionContext)
    extends Cats {
  private val logger = Logger(this.getClass.getName)

  def submit(
        propertyLinkRequest: PropertyLinkRequest,
        clientId: Option[Long]
  )(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): EitherT[Future, AttachmentException, Unit] =
    clientId match {
      case Some(id) => submitOnClientBehalf(propertyLinkRequest, id)
      case _        => submit(propertyLinkRequest)
    }

  def submit(
        propertyLinkRequest: PropertyLinkRequest
  )(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): EitherT[Future, AttachmentException, Unit] =
    for {
      _ <- businessRatesAttachmentService.submit(propertyLinkRequest.submissionId, propertyLinkRequest.references)
      _ <- EitherT.liftF(propertyLinkConnector.createPropertyLink(PropertyLinkPayload(propertyLinkRequest)))
    } yield ()

  def submitOnClientBehalf(
        propertyLinkRequest: PropertyLinkRequest,
        clientId: Long
  )(implicit request: LinkingSessionRequest[_], hc: HeaderCarrier): EitherT[Future, AttachmentException, Unit] =
    for {
      _ <- businessRatesAttachmentService.submit(propertyLinkRequest.submissionId, propertyLinkRequest.references)
      _ <- EitherT.liftF(
            propertyLinkConnector.createPropertyLinkOnClientBehalf(PropertyLinkPayload(propertyLinkRequest), clientId))
    } yield ()

  def findEarliestStartDate(uarn: Long)(implicit hc: HeaderCarrier): Future[Option[LocalDate]] =
    for {
      propertyHistory <- propertyLinkConnector.getPropertyHistory(uarn)
      earliestStartDate: Option[LocalDate] = propertyHistory.history
        .find(_.propertyLinkEarliestStartDate.isDefined)
        .map(_.propertyLinkEarliestStartDate)
        .getOrElse(None)
    } yield earliestStartDate
}
