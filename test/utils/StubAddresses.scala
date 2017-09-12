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

package utils

import connectors.Addresses
import models.Address
import org.scalacheck.Arbitrary
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Random
import org.scalacheck.Arbitrary._
import resources._

object StubAddresses extends Addresses(StubServicesConfig,StubHttp) {
  val noResultPostcode = "NO RESULT"

  override def create(address: Address)(implicit hc: HeaderCarrier) = Future.successful(Random.nextInt)

  override def findByPostcode(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[Address]] = {
    if (postcode.contentEquals(noResultPostcode)) {
      Future.successful(Seq[Address]())
    } else {
      Future.successful(Seq.fill(10)(arbitrary[Address].sample.get))
    }
  }
}
