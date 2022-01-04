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

package models

import binders.validation.ValidationUtils
import play.api.libs.json.Json
import play.api.mvc.QueryStringBindable

case class ClientDetails(organisationId: Long, organisationName: String)

object ClientDetails extends ValidationUtils {
  implicit val format = Json.format[ClientDetails]

  implicit def queryStringBinder(
        implicit strBinder: QueryStringBindable[String],
        longBinder: QueryStringBindable[Long]) =
    new QueryStringBindable[Option[ClientDetails]] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[ClientDetails]]] =
        for {
          organisationId   <- longBinder.bind("organisationId", params)
          organisationName <- strBinder.bind("organisationName", params)
        } yield {
          (organisationId, organisationName) match {
            case (Right(organisationId), Right(organisationName)) =>
              Right(Some(ClientDetails(organisationId = organisationId, organisationName = organisationName)))
            case _ =>
              Left("Unable to bind a ClientDetails")
          }
        }

      override def unbind(key: String, params: Option[ClientDetails]): String =
        params match {
          case Some(client) =>
            strBinder.unbind("organisationId", client.organisationId.toString) + "&" + strBinder
              .unbind("organisationName", client.organisationName)
          case _ => ""
        }
    }

}
