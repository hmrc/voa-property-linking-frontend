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

import connectors.PropertyConnector
import models.Property
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

object StubPropertyConnector extends PropertyConnector(StubHttp) {
  private var stubbedProperties: Seq[Property] = Nil

  def stubProperty(property: Property): Unit = {
    stubbedProperties :+= property
  }

  def reset() = {
    stubbedProperties = Nil
  }

  override def find(uarn: Long)(implicit hc: HeaderCarrier): Future[Option[Property]] = Future.successful(stubbedProperties.find(_.uarn == uarn))
}
