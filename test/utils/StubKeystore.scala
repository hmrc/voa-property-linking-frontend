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

import config.VPLSessionCache
import models.PersonalDetails
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

//FIXME - delete me.
//object StubKeystore extends VPLSessionCache(StubHttp) {
//  private var personalDetails: Option[PersonalDetails] = None
//
//  def stubPersonalDetails(details: PersonalDetails) = personalDetails = Some(details)
//
//  override def get(implicit hc: HeaderCarrier): Future[PersonalDetails] = personalDetails match {
//    case Some(d) => Future.successful(d)
//    case None => Future.failed(new Exception("personal details not stubbed"))
//  }
//
//  override def saveOrUpdate(details: PersonalDetails)(implicit hc: HeaderCarrier) = Future.successful(Unit)
//}
