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

package utils

import connectors.Addresses
import models.registration.GroupAccountDetails
import models.{Address, DetailedAddress}
import org.mockito.Mockito._
import org.scalacheck.Arbitrary._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import utils.Configs._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object StubAddresses extends Addresses(servicesConfig, mock(classOf[HttpClient]))(ExecutionContext.global) {
  val noResultPostcode = "NO RESULT"

  override def create(address: Address)(implicit hc: HeaderCarrier) = Future.successful(Random.nextInt)

  override def findByPostcode(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[DetailedAddress]] =
    if (postcode.contentEquals(noResultPostcode)) {
      Future.successful(Seq[DetailedAddress]())
    } else {
      Future.successful(Seq.fill(10)(arbitrary[DetailedAddress].sample.get))
    }

  override def findById(id: Long)(implicit hc: HeaderCarrier): Future[Option[Address]] =
    Future.successful(Some(arbitrary[Address].sample.get))

  override def registerAddress(details: GroupAccountDetails)(implicit hc: HeaderCarrier): Future[Long] =
    if (details.companyName == "FAIL")
      create(details.address)
    else
      Future.successful(12L)
}
