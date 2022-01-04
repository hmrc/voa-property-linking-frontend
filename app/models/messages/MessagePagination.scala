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

package models.messages

import models.SortOrder
import play.api.mvc.QueryStringBindable

case class MessagePagination(
      clientName: Option[String] = None,
      referenceNumber: Option[String] = None,
      address: Option[String] = None,
      pageNumber: Int = 1,
      pageSize: Int = 15,
      sortField: MessageSortField = MessageSortField.EffectiveDate,
      sortOrder: SortOrder = SortOrder.Descending) {

  lazy val startPoint: Int = (pageNumber - 1) * pageSize + 1

  def reverseSorting: MessagePagination = copy(sortOrder = sortOrder.reverse)

  def previousPage: MessagePagination = copy(pageNumber = pageNumber - 1)

  def nextPage: MessagePagination = copy(pageNumber = pageNumber + 1)

  def clear: MessagePagination = copy(address = None, referenceNumber = None, clientName = None)

  lazy val queryString: String =
    s"""
       |${clientName.filter(_.nonEmpty).fold("")(cn => s"clientName=$cn&")}
       |${referenceNumber.filter(_.nonEmpty).fold("")(rn => s"referenceNumber=$rn&")}
       |${address.filter(_.nonEmpty).fold("")(a => s"address=$a&")}
       |startPoint=$startPoint&
       |pageSize=$pageSize&
       |sortField=$sortField&
       |sortOrder=$sortOrder
       |""".stripMargin.replaceAll("\n", "")
}

object MessagePagination {
  implicit val queryStringBindable: QueryStringBindable[MessagePagination] =
    new QueryStringBindable[MessagePagination] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MessagePagination]] = {
        def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] =
          qsb.bind(key, params)

        for {
          clientName      <- bindParam[Option[String]]("clientName")
          referenceNumber <- bindParam[Option[String]]("referenceNumber")
          address         <- bindParam[Option[String]]("address")
          pageNumber      <- bindParam[Int]("pageNumber")
          pageSize        <- bindParam[Int]("pageSize")
          sortField       <- bindParam[MessageSortField]("sortField")
          sortOrder       <- bindParam[SortOrder]("sortOrder")
        } yield {
          (clientName, referenceNumber, address, pageNumber, pageSize, sortField, sortOrder) match {
            case (Right(cn), Right(rn), Right(ad), Right(pn), Right(ps), Right(sf), Right(so)) =>
              Right(MessagePagination(cn, rn, ad, pn, ps, sf, so))
            case _ => Left("Unable to bind to MessagePagination")
          }
        }
      }

      override def unbind(key: String, value: MessagePagination): String =
        s"""
           |clientName=${value.clientName.getOrElse("")}&
           |referenceNumber=${value.referenceNumber.getOrElse("")}&
           |address=${value.address.getOrElse("")}&
           |pageNumber=${value.pageNumber}&
           |pageSize=${value.pageSize}&
           |sortField=${value.sortField}&
           |sortOrder=${value.sortOrder}
           |""".stripMargin.replaceAll("\n", "")
    }
}
