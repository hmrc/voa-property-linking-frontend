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

package controllers

import config.Global
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.BadRequest

import scala.concurrent.Future

trait ValidPagination extends PropertyLinkingController {
  protected def withValidPagination(page: Int, pageSize: Int, getTotal: Boolean = true)
                                   (default: Pagination => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      BadRequest(Global.badRequestTemplate)
    } else {
      default(Pagination(pageNumber = page, pageSize = pageSize, resultCount = getTotal))
    }
  }
}

case class Pagination(pageNumber: Int, pageSize: Int, totalResults: Long = 0, resultCount: Boolean = true) {
  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$resultCount"
}
