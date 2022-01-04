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

package connectors

import connectors.errorhandler.exceptions.ExceptionThrowingReadsInstances
import javax.inject.Inject
import models.{DetailedIndividualAccount, IndividualAccount, IndividualAccountSubmission}
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class IndividualAccounts @Inject()(config: ServicesConfig, http: HttpClient)(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = config.baseUrl("property-linking") + s"/property-linking/individuals"

  def get(personId: Long)(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] =
    http.GET[Option[DetailedIndividualAccount]](s"$baseUrl/$personId")

  def withExternalId(externalId: String)(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] =
    http.GET[Option[DetailedIndividualAccount]](s"$baseUrl?externalId=$externalId")

  def create(account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[Int] =
    http.POST[IndividualAccountSubmission, JsValue](baseUrl, account) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(id)) => id.toInt
        case _                       => throw new Exception(s"Invalid id: $js")
      }
    }

  def update(account: DetailedIndividualAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    import ExceptionThrowingReadsInstances._
    http.PUT[IndividualAccount, HttpResponse](baseUrl + s"/${account.individualId}", account.toIndividualAccount) map {
      _ =>
        ()
    }
  }
}
