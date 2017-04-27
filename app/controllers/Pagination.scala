package controllers

import config.Global
import play.api.mvc.{AnyContent, Request, Result}
import play.api.mvc.Results.BadRequest

import scala.concurrent.Future

trait ValidPagination {
  protected def withValidPagination(page: Int, pageSize: Int)(default: Pagination => Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (page <= 0 || pageSize < 10 || pageSize > 100) {
      Future.successful(BadRequest(Global.badRequestTemplate))
    } else {
      default(Pagination(page, pageSize))
    }
  }
}

case class Pagination(pageNumber: Int, pageSize: Int, totalResults: Int = 0, resultCount: Boolean = true) {
  def startPoint: Int = pageSize * (pageNumber - 1) + 1
  override val toString = s"startPoint=$startPoint&pageSize=$pageSize&requestTotalRowCount=$resultCount"
}
