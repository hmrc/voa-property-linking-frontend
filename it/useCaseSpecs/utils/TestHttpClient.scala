package useCaseSpecs.utils

import org.scalatest.{AppendedClues, MustMatchers}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import SessionDocument._
import uk.gov.hmrc.http.cache.client.CacheMap
import useCaseSpecs.utils.Page.NoSessionId

import scala.concurrent.Future

object TestHttpClient {
  type Url = String
  type Body = String
}

import TestHttpClient._

class TestHttpClient extends HttpGet with HttpPost with HTTPTestPUT with HttpDelete with MustMatchers with AppendedClues with VPLAPIs {

  private var stubbedGets: Seq[(Url, Seq[(String, String)], HttpResponse)] = Seq.empty

  def stubGet(url: String, headers: Seq[(String, String)], response: HttpResponse) =
    stubbedGets = stubbedGets :+ ((url, headers, response))

  def reset() = {
    stubbedGets = Seq.empty
    resetPUTs()
    this
  }

  override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    stubbedGets.find(x => x._1 == url && x._2.forall(y => hc.headers.exists(h => h._1 == y._1 && h._2 == y._2))) match {
      case Some((_, _, res)) => Future.successful(res)
      case _ => throw new HttpGetRequestNotStubbed(url, hc, stubbedGets.map(_._1))
    }

  override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override val hooks: Seq[HttpHook] = Seq.empty

  class HttpGetRequestNotStubbed(url: String, hc: HeaderCarrier, all: Seq[String]) extends Exception(s"GET Request not stubbed: $url - ${hc.headers}\n. Expected one of: $all")
}

trait HTTPTestPUT extends HttpPut with MustMatchers with AppendedClues {
  private var stubbedPuts: Seq[(Url, Seq[(String, String)], Body, HttpResponse)] = Seq.empty
  private var actualPuts: Seq[(Url, Body)] = Seq.empty
  private var allowedPUTs: Seq[Url] = Seq.empty

  def stubPut(url: String, headers: Seq[(String, String)], body: String, response: HttpResponse) =
    stubbedPuts = stubbedPuts :+ ((url, headers, body, response))

  def allowPUTsFor(baseUrl: String) = allowedPUTs = allowedPUTs :+ baseUrl

  protected def resetPUTs() {
    stubbedPuts = Seq.empty
    actualPuts = Seq.empty
  }

  override protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val b: String = Json.stringify(rds.writes(body))
    actualPuts = actualPuts :+ (url, b)
    stubbedPuts.find(x => insertUUID(x._1, url) == url && x._2.forall(hc.headers.contains) && x._3 == b) match {
      case Some((_, _, _, res)) =>
        Future.successful(res)
      case _ if allowedPUTs.exists(url.startsWith) =>
        Future.successful(HttpResponse(201, responseJson = Some(Json.toJson(CacheMap("", Map.empty)))))
      case _ =>
        throw new HttpPutRequestNotStubbed(url, hc, b, stubbedPuts.map(p => (p._1, p._3)))
    }
  }

  def verifyPUT(url: String, body: String) {
    val put = actualPuts.filter(p => insertUUID(p._1, url) == p._1).lastOption.getOrElse(fail(s"No PUT occurred for: $url"))
    put._2 mustEqual body
  }

  def verifyOnlyNPUTsFor(baseUrl: String, amount: Int) =
    actualPuts.count(_._1.startsWith(baseUrl)) mustEqual amount withClue s"More than $amount PUTs for $baseUrl"

  def verifyNoPUTsFor(baseUrl: String) =
    actualPuts.filter(_._1.startsWith(baseUrl)) mustBe empty withClue s"No PUTs expected for: $baseUrl, but found: ${actualPuts.map(_._1)}"

  private def insertUUID(stubbed: String, url: String): String = {
    stubbed.replaceAll("[UUID]", url.split("/").last)
  }

  class HttpPutRequestNotStubbed(url: String, hc: HeaderCarrier, body: String, all: Seq[(String, Any)])
    extends Exception(s"PUT Request not stubbed: $url = ${hc.headers} - $body\nExpected one of: $all\nOr: $allowedPUTs")
}

trait VPLAPIs { this: TestHttpClient =>
  lazy val keystoreBaseUrl = "http://localhost:8400/keystore"
  lazy val propertiesBaseUrl = "http://localhost:9527/properties"
  lazy val propertyLinksBaseUrl = "http://localhost:9528/property-links"

  allowPUTsFor(keystoreBaseUrl)
  allowPUTsFor(propertyLinksBaseUrl)

  def stubPropertiesAPI(billingAuthorityReference: String, p: Property) =
    stubGet(s"$propertiesBaseUrl/$billingAuthorityReference", Seq.empty, HttpResponse(200, responseJson = Some(Json.toJson(p))))

  def stubKeystoreSession(session: SessionDocument)(implicit sid: SessionID) =
    stubGet(
      s"$keystoreBaseUrl/voa-property-linking-frontend/$sid", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(CacheMap("", Map(SessionDocument.sessionKey -> Json.toJson(session))))))
    )

  def verifyKeystoreSaved(session: SessionDocument)(implicit sid: SessionID) =
    verifyPUT(
      s"$keystoreBaseUrl/voa-property-linking-frontend/$sid/${SessionDocument.sessionKey}", Json.stringify(Json.toJson(Json.toJson(session)))
    )

  def verifyPropertyLinkRequest(billingAuthorityReference: String, accountId: String, request: LinkToProperty) =
    verifyPUT(
      s"$propertyLinksBaseUrl/$billingAuthorityReference/$accountId/[UUID]", Json.stringify(Json.toJson(request))
    )

  def verifyNoMoreLinkRequests(amount: Int) =
    verifyOnlyNPUTsFor(propertyLinksBaseUrl, amount)
}
