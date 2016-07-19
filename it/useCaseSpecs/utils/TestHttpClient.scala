package useCaseSpecs.utils

import org.scalatest.MustMatchers
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import SessionDocument._

import scala.concurrent.Future

class TestHttpClient extends HttpGet with HttpPost with HttpPut with HttpDelete with MustMatchers with VPLAPIs {
  type Url = String
  type Body = String
  private var stubbedGets: Seq[(Url, Seq[(String, String)], HttpResponse)] = Seq.empty
  private var stubbedPuts: Seq[(Url, Seq[(String, String)], Body, HttpResponse)] = Seq.empty
  private var actualPuts: Seq[(Url, Body)] = Seq.empty

  def stubGet(url: String, headers: Seq[(String, String)], response: HttpResponse) =
    stubbedGets = stubbedGets :+ ((url, headers, response))

  def stubPut[A](url: String, headers: Seq[(String, String)], body: String, response: HttpResponse) =
    stubbedPuts = stubbedPuts :+ ((url, headers, body, response))

  def reset() = {
    stubbedGets = Seq.empty
    stubbedPuts = Seq.empty
    actualPuts = Seq.empty
    this
  }

  override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    stubbedGets.find(x => x._1 == url && x._2.forall(y => hc.headers.exists(h => h._1 == y._1 && h._2 == y._2))) match {
      case Some((_, _, res)) => Future.successful(res)
      case _ =>
        throw new HttpRequestNotStubbed(url, hc, stubbedGets.map(_._1))
    }

  override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val b: String = Json.stringify(rds.writes(body))
    actualPuts = actualPuts :+ (url, b)
    stubbedPuts.find(x => insertUUID(x._1, url) == url && x._2.forall(hc.headers.contains) && x._3 == b) match {
      case Some((_, _, _, res)) => Future.successful(res)
      case _ => throw new HttpPutRequestNotStubbed(url, hc, b, stubbedPuts.map(x => (insertUUID(x._1, url), x._3)))
    }
  }

  def verifyPUT(url: String, body: String): Unit = {
    val put = actualPuts.find(p => insertUUID(p._1, url) == p._1).getOrElse(fail(s"No PUT stubbed for: $url"))
    put._2 mustEqual body
  }

  private def insertUUID(stubbed: String, url: String): String = {
    stubbed.replaceAll("UUID", url.split("/").last)
  }

  override protected def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override val hooks: Seq[HttpHook] = Seq.empty

  class HttpRequestNotStubbed(url: String, hc: HeaderCarrier, all: Seq[String]) extends Exception(s"Request not stubbed: $url - ${hc.headers}\n. Expected one of: $all")

  class HttpPutRequestNotStubbed(url: String, hc: HeaderCarrier, body: String, all: Seq[(String, Any)])
        extends Exception(s"Request not stubbed: $url = ${hc.headers} - $body\nExpected one of: $all")
}

trait VPLAPIs { this: TestHttpClient =>
  def stubPropertiesAPI(billingAuthorityReference: String, p: Property) =
    stubGet(s"http://localhost:9527/property-valuations/properties/$billingAuthorityReference", Seq.empty, HttpResponse(200, responseJson = Some(Json.toJson(p))))
}
