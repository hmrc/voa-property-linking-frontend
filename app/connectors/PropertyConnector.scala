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

package connectors

import models.{Address, Property}
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

class PropertyConnector(http: HttpGet)(implicit ec: ExecutionContext) extends ServicesConfig {
  lazy val url = baseUrl("property-valuations")

  def find(billingAuthorityReference: String)(implicit hc: HeaderCarrier): Future[Option[Property]] =
    http.GET[Option[Property]](url + s"/properties/$billingAuthorityReference")
        .map(_.orElse(PrototypeTestData.pretendSearchResults.find(_.billingAuthorityReference == billingAuthorityReference)))
        .recover { case _ => PrototypeTestData.pretendSearchResults.find(_.billingAuthorityReference == billingAuthorityReference) }
}

object PrototypeTestData {
  lazy val conflictedProperty = Property(
    "testconflict", Address(Seq("22 Conflict Self-cert", "The Town"), "AA11 1AA"), true, true
  )
  lazy val bankForRatesBillVerifiedJourney = Property(
    "testbankaccepted", Address(Seq("Banky McBankface (rates bill accepted)", "Some Road", "Some Town"), "AA11 1AA"), false, true
  )
  lazy val bankForRatesBillFailedJourney = Property(
    "testbankrejected", Address(Seq("Banky McSadface (rates bill rejected)", "Some Road", "Some Town"), "AA11 1AA"), false, true
  )
  lazy val pretendSearchResults = Seq(
    Property("testselfcertifiableshop", Address(Seq("1 The Self-cert non-bank street", "The Town"), "AA11 1AA"), true, true),
    conflictedProperty,
    bankForRatesBillVerifiedJourney,
    bankForRatesBillFailedJourney,
    Property(
      "testbanknomail", Address(Seq("Banky McNoMailFace (Cannot receive mail)", "Some Road", "Some Town"), "AA11 1AA"), false, false
    )
  )
}
