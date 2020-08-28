/*
 * Copyright 2020 HM Revenue & Customs
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

package repositories

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONString, _}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.NoSessionException
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

@Singleton
class PersonalDetailsSessionRepository @Inject()(mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends SessionRepository("personDetails", mongo)

@Singleton
class PropertyLinkingSessionRepository @Inject()(mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends SessionRepository("propertyLinking", mongo)

@Singleton
class PropertyLinksSessionRepository @Inject()(mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends SessionRepository("propertyLinks", mongo)

@Singleton
class AppointAgentSessionRepository @Inject()(mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends SessionRepository("appointNewAgent", mongo)

@Singleton
class AssessmentsPageSessionRepository @Inject()(mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends SessionRepository("assessmentPage", mongo)

class SessionRepository @Inject()(formId: String, mongo: ReactiveMongoComponent)(
      implicit executionContext: ExecutionContext)
    extends ReactiveRepository[SessionData, String](
      "sessions",
      mongo.mongoConnector.db,
      SessionData.format,
      implicitly[Format[String]]) with SessionRepo {

  override def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] =
    saveOrUpdate[A](data)

  override def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] =
    Mdc.preservingMdc {
      for {
        sessionId <- getSessionId
        _ <- collection
              .update(false)
              .one(
                BSONDocument("_id" -> BSONString(sessionId)),
                BSONDocument(
                  "$set"         -> BSONDocument(s"data.$formId" -> Json.toJson(data)),
                  "$setOnInsert" -> BSONDocument("createdAt"     -> BSONDateTime(System.currentTimeMillis))
                ),
                upsert = true
              )
      } yield {
        ()
      }
    }

  override def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]] =
    Mdc.preservingMdc {
      for {
        sessionId   <- getSessionId
        maybeOption <- findById(sessionId)
      } yield {
        maybeOption
          .map(_.data \ formId)
          .flatMap(x =>
            x match {
              case JsDefined(value) => Some(value.as[A])
              case JsUndefined()    => None
          })
      }
    }

  override def remove()(implicit hc: HeaderCarrier): Future[Unit] =
    Mdc.preservingMdc {
      for {
        sessionId <- getSessionId
        _ <- collection
              .update(false)
              .one(
                BSONDocument("_id"    -> BSONString(sessionId)),
                BSONDocument("$unset" -> BSONDocument(s"data.$formId" -> 1))
              )
      } yield {
        ()
      }
    }

  private val noSession = Future.failed[String](NoSessionException)

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq(("createdAt", IndexType.Ascending)),
      name = Some("sessionTTL"),
      options = BSONDocument("expireAfterSeconds" -> (2.hours).toSeconds)
    )
  )

  private def getSessionId(implicit hc: HeaderCarrier): Future[String] =
    hc.sessionId.fold(noSession)(c => Future.successful(c.value))

}

case class SessionData(_id: String, data: JsValue, createdAt: BSONDateTime = BSONDateTime(System.currentTimeMillis))

object SessionData {

  val format = Json.format[SessionData]
}

trait SessionRepo {

  def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]]

  def remove()(implicit hc: HeaderCarrier): Future[Unit]
}
