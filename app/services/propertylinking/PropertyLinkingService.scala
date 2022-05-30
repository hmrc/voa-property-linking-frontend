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

package services.propertylinking

import actions.propertylinking.requests.LinkingSessionRequest
import cats.data.EitherT
import config.ApplicationConfig
import connectors.propertyLinking.PropertyLinkConnector
import models.properties.PropertyHistory
import models.propertylinking.payload.PropertyLinkPayload
import models.propertylinking.requests.PropertyLinkRequest
import services.BusinessRatesAttachmentsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.propertylinking.exceptions.attachments.AttachmentException
import utils.Cats

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class PropertyLinkingService @Inject()(
      businessRatesAttachmentService: BusinessRatesAttachmentsService,
      propertyLinkConnector: PropertyLinkConnector,
      config: ApplicationConfig
)(implicit executionContext: ExecutionContext)
    extends Cats {

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

  def findEarliestStartDate(propertyHistory: PropertyHistory): LocalDate = {
    implicit val localDateOrdering: Ordering[LocalDate] = _ compareTo _

    val dates = propertyHistory.history
      .flatMap(_.propertyLinkEarliestStartDate)
    Try[LocalDate](dates.min)
      .getOrElse(if (propertyHistory.isWelsh) config.earliestWelshStartDate else config.earliestEnglishStartDate)
  }
}
