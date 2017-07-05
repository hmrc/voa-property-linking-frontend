/*
 * Copyright 2017 HM Revenue & Customs
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

import models.{DetailedIndividualAccount, IndividualAccount, IndividualAccountSubmission}
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class IndividualAccounts(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("property-linking") + s"/property-linking/individuals"

  def get(personId: Int)(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] = {
    http.GET[Option[DetailedIndividualAccount]](s"$baseUrl/$personId")
  }

  def withExternalId(externalId: String)(implicit hc: HeaderCarrier): Future[Option[DetailedIndividualAccount]] = {
    http.GET[Option[DetailedIndividualAccount]](s"$baseUrl?externalId=$externalId")
  }

  def create(account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[Int] = {
    http.POST[IndividualAccountSubmission, JsValue](baseUrl, account) map { js => js \ "id" match {
      case JsDefined(JsNumber(id)) => id.toInt
      case _ => throw new Exception(s"Invalid id: $js")
    }}
  }

  def update(account: DetailedIndividualAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[IndividualAccount, HttpResponse](baseUrl + s"/${account.individualId}", account.toIndividualAccount) map { _ => () }
  }
}
