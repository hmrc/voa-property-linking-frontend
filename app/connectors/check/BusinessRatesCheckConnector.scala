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

package connectors.check

import models.PartialCheckFormats
import models.check.CheckId
import models.dvr.cases.check.CheckType
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.uritemplate.syntax.UriTemplateSyntax
import uk.gov.voa.businessrates.values._
import uk.gov.voa.businessrates.values.connectors.RequestResult._
import utils.HttpReads.createReads

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesCheckConnector @Inject() (http: DefaultHttpClient, servicesConfig: ServicesConfig)(implicit
      executionContext: ExecutionContext
) extends Logging with UriTemplateSyntax with PartialCheckFormats {
  protected lazy val serviceUrl: String = servicesConfig.baseUrl("business-rates-check")

  private def url(path: String) = s"$serviceUrl/$path"

  def start(
        propertyLinkId: PropertyLinkId,
        assessmentRef: AssessmentRef,
        checkType: CheckType,
        propertyLinkSubmissionId: Option[String] = None,
        uarn: Option[Long] = None,
        dvrCheck: Boolean = false,
        rateableValueTooHigh: Option[Boolean] = None
  )(implicit hc: HeaderCarrier): Future[Either[RequestFailure, HttpResponse]] = {
    implicit val httpReads: HttpReads[Either[RequestFailure, HttpResponse]] =
      createReads(CREATED, Map(FORBIDDEN -> Forbidden))
    http
      .POSTEmpty[Either[RequestFailure, HttpResponse]](
        url(
          s"property-link/$propertyLinkId/assessment/$assessmentRef/start-check/${checkType.value}${queryParams(propertyLinkSubmissionId, uarn, Some(dvrCheck), rateableValueTooHigh)}"
        )
      )
  }

  def updateResumeCheckUrl(checkId: CheckId, resumeUrl: String)(implicit
        hc: HeaderCarrier
  ): Future[Either[RequestFailure, HttpResponse]] = {

    implicit val httpReads = createReads(NO_CONTENT, Map(FORBIDDEN -> Forbidden, NOT_FOUND -> NotFound))

    http.PUT[JsValue, Either[RequestFailure, HttpResponse]](
      url(s"partial-check/$checkId/resume"),
      Json.obj("resumeUrl" -> resumeUrl)
    )
  }

  private def queryParams(
        propertyLinkSubmissionId: Option[String],
        uarn: Option[Long],
        dvrCheck: Option[Boolean] = None,
        rateableValueTooHigh: Option[Boolean] = None
  ): String =
    Seq(
      propertyLinkSubmissionId.map(p => s"propertyLinkSubmissionId=$p"),
      uarn.map(u => s"uarn=$u"),
      dvrCheck.map(d => s"dvrCheck=$d"),
      rateableValueTooHigh.map(r => s"rateableValueTooHigh=$r")
    ).flatten.mkString("?", "&", "")

}
