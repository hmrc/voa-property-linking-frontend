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

package connectors.vmv

import models.properties.PropertyHistory
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.uritemplate.syntax.UriTemplateSyntax

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class VmvConnector @Inject() (
      http: DefaultHttpClient,
      @Named("vmv.singularPropertyUrl") propertyHistoryUrl: String
)(implicit ec: ExecutionContext)
    extends UriTemplateSyntax {

  def getPropertyHistory(uarn: Long)(implicit hc: HeaderCarrier): Future[PropertyHistory] =
    http.GET[PropertyHistory](propertyHistoryUrl.templated("uarn" -> uarn))
}
