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

package repositories

import com.google.inject.Singleton
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("personDetails", mongo)

@Singleton
class PropertyLinkingSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("propertyLinking", mongo)

@Singleton
class PropertyLinksSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("propertyLinks", mongo)

@Singleton
class AppointAgentSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("appointNewAgent", mongo)

@Singleton
class RevokeAgentPropertiesSessionRepository @Inject() (mongo: MongoComponent)(implicit
      executionContext: ExecutionContext
) extends SessionRepository("revokeAgentProperties", mongo)

@Singleton
class AppointAgentPropertiesSessionRepository @Inject() (mongo: MongoComponent)(implicit
      executionContext: ExecutionContext
) extends SessionRepository("appointAgentProperties", mongo)

@Singleton
class AssessmentsPageSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("assessmentPage", mongo)

@Singleton
class ManageAgentSessionRepository @Inject() (mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends SessionRepository("manageAgent", mongo)

//TODO: investigate ttl being 2Hours(Other services session cache is set to 15mins - make config driven
abstract class SessionRepository @Inject() (formId: String, mongo: MongoComponent)(implicit
      executionContext: ExecutionContext
) extends PlayMongoRepository[SessionData](
      collectionName = "sessions",
      mongoComponent = mongo,
      domainFormat = SessionData.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("createdAt"),
          IndexOptions()
            .name("sessionTTL")
            .expireAfter(2L, HOURS)
        )
      )
    ) with SessionRepo {

  override def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] =
    saveOrUpdate[A](data)

  override def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit] =
    for {
      sessionId <- getSessionId
      _ <- collection
             .findOneAndUpdate(
               filter = Filters.equal("_id", sessionId),
               update = Updates.combine(
                 Updates.set(s"data.$formId", Codecs.toBson(data)),
                 Updates.setOnInsert("createdAt", Instant.now)
               ),
               options = FindOneAndUpdateOptions().upsert(true)
             )
             .toFuture()
    } yield ()

  override def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]] =
    for {
      sessionId   <- getSessionId
      maybeOption <- collection.find(Filters.equal("_id", sessionId)).headOption()
    } yield maybeOption
      .map(_.data \ formId)
      .flatMap(x =>
        x match {
          case JsDefined(value) => Some(value.as[A])
          case JsUndefined()    => None
        }
      )

  def findFirst(implicit hc: HeaderCarrier): Future[SessionData] =
    for {
      sessionId <- getSessionId
      session   <- collection.find(equal("_id", sessionId)).first().toFuture()
    } yield session

  override def remove()(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      sessionId <- getSessionId
      _         <- collection.deleteOne(equal("_id", sessionId)).toFuture()
    } yield ()

  def removeAll()(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      sessionId <- getSessionId
      _         <- collection.deleteMany(equal("_id", sessionId)).toFuture()
    } yield ()

  private case class NoSessionException() extends Exception("Session not found")

  private val noSession = Future.failed[String](NoSessionException())

  private def getSessionId(implicit hc: HeaderCarrier): Future[String] =
    hc.sessionId.fold(noSession)(c => Future.successful(c.value))

}

case class SessionData(_id: String, data: JsValue, createdAt: Instant = Instant.now)

object SessionData {

  implicit val formatInstant: Format[Instant] = MongoJavatimeFormats.instantFormat
  val format = Json.format[SessionData]
}

trait SessionRepo {

  def start[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def saveOrUpdate[A](data: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[Unit]

  def get[A](implicit rds: Reads[A], hc: HeaderCarrier): Future[Option[A]]

  def remove()(implicit hc: HeaderCarrier): Future[Unit]
}
