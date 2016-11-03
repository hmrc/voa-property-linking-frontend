/*
 * Copyright 2016 HM Revenue & Customs
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

package utils

import config.VPLSessionCache
import models.IndividualDetails
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

object StubKeystore extends VPLSessionCache(StubHttp) {
  private var individualDetails: Option[IndividualDetails] = None

  def stubIndividualDetails(details: IndividualDetails) = individualDetails = Some(details)
  
  override def getIndividualDetails(implicit hc: HeaderCarrier): Future[IndividualDetails] = individualDetails match {
    case Some(d) => Future.successful(d)
    case None => Future.failed(new Exception("individual details not stubbed"))
  }
}
