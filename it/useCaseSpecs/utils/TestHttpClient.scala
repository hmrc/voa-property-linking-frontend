package useCaseSpecs.utils

import java.util.Base64

import org.scalatest.{AppendedClues, MustMatchers}
import play.api.Logger
import play.api.libs.json._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.{HeaderCarrier, _}
import useCaseSpecs.utils.SessionDocument._

import scala.concurrent.Future

object TestHttpClient {
  type Url = String
  type Body = String
}

import useCaseSpecs.utils.TestHttpClient._

class TestHttpClient extends HttpGet with HTTPTestPUT with HTTPTestPOST with HttpDelete with MustMatchers with AppendedClues with VPLAPIs {

  private var stubbedGets: Seq[(Url, Seq[(String, String)], HttpResponse)] = Seq.empty

  def stubGet(url: String, headers: Seq[(String, String)], response: HttpResponse) = {
    stubbedGets = stubbedGets :+ ((url, headers, response))
  }

  def stubAuthentication(session: GGSession) = {
    val stubResponse = HttpResponse(
      responseStatus = 200,
      responseJson = Some(
        Json.obj(
          "userId" -> session.userId,
          "credentialStrength" -> "weak",
          "uri" -> session.userId,
          "confidenceLevel" -> 50,
          "accounts" -> JsArray()
        )
      )
    )
    stubGet("http://localhost:8500/auth/authority", Seq("token" -> session.token), stubResponse)
  }

  def reset() = {
    stubbedGets = Seq.empty
    resetPUTs()
    resetPOSTs()
    this
  }

  override protected def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] ={
    stubbedGets.find(x => x._1 == url && x._2.forall(y => hc.headers.exists(h => h._1 == y._1 && h._2 == y._2))) match {
      case Some((_, _, res)) => Future.successful(res)
      case _ => throw new HttpGetRequestNotStubbed(url, hc, stubbedGets.map(_._1))
    }
  }

  override protected def doPostString(url: String, body: String, headers: Seq[(String, String)])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doFormPost(url: String, body: Map[String, Seq[String]])(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doEmptyPost[A](url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override protected def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = ???

  override val hooks: Seq[HttpHook] = Seq.empty

  class HttpGetRequestNotStubbed(url: String, hc: HeaderCarrier, all: Seq[String]) extends Exception(s"GET Request not stubbed: $url - ${hc.headers}\n. Expected one of: ${all.mkString("\n")}")
}

trait HTTPTestPOST extends HttpPost with MustMatchers with AppendedClues {
  private var stubbedPosts: Seq[(Url, Seq[(String, String)], Body, HttpResponse)] = Seq.empty
  private var actualPosts: Seq[(Url, Body)] = Seq.empty
  private var allowedPosts: Seq[Url] = Nil

  def allowPOSTsFor(baseUrl: String) = allowedPosts = allowedPosts :+ baseUrl

  class HttpPostRequestNotStubbed(url: String, hc: HeaderCarrier, body: String, all: Seq[(String, Any)])
    extends Exception(s"POST Request not stubbed: $url = ${hc.headers} - $body\nExpected one of: $all\n")

  private def insertUUID(stubbed: String, url: String): String = {
    stubbed.replaceAll("""UUID""", url.split("/").last)
  }

  override protected def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val b: String = Json.stringify(wts.writes(body))
    actualPosts = actualPosts :+ (url, b)
    stubbedPosts.find(x => insertUUID(x._1, url) == url && x._2.forall(hc.headers.contains) && x._3 == b) match {
      case Some((_, _, _, res)) =>
        Future.successful(res)
      case _ if allowedPosts.exists(url.startsWith) =>
        Future.successful(HttpResponse(201))
      case _ =>
        throw new HttpPostRequestNotStubbed(url, hc, b, stubbedPosts.map(p => (p._1, p._3)))
    }
  }

  def verifyOnlyNPOSTsFor(baseUrl: String, amount: Int) =
    actualPosts.count(_._1.startsWith(baseUrl)) mustEqual amount withClue s"More than $amount POSTs for $baseUrl"

  def verifyPOST(url: String, body: String) = {
    val post = actualPosts.filter { p => insertUUID(url, p._1) == p._1 }.lastOption getOrElse {fail(s"No POST occurred for: $url")}
    post._2 mustEqual body
  }

  def resetPOSTs(): Unit = {
    stubbedPosts = Seq.empty
    actualPosts = Seq.empty
  }

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

  override protected def doPut[A](url: String, body: A)(implicit wts: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    val b: String = Json.stringify(wts.writes(body))
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
    val put = actualPuts.filter { p => insertUUID(url, p._1) == p._1}.lastOption.getOrElse(fail(s"No PUT occurred for: $url"))
    put._2 mustEqual body
  }

  def verifyOnlyNPUTsFor(baseUrl: String, amount: Int) =
    actualPuts.count(_._1.startsWith(baseUrl)) mustEqual amount withClue s"More than $amount PUTs for $baseUrl"

  def verifyNoPUTsFor(baseUrl: String) =
    actualPuts.filter(_._1.startsWith(baseUrl)) mustBe empty withClue s"No PUTs expected for: $baseUrl, but found: ${actualPuts.map(_._1)}"

  private def insertUUID(stubbed: String, url: String): String = {
    stubbed.replaceAll("""UUID""", url.split("/").last)
  }

  class HttpPutRequestNotStubbed(url: String, hc: HeaderCarrier, body: String, all: Seq[(String, Any)])
    extends Exception(s"PUT Request not stubbed: $url = ${hc.headers} - $body\nExpected one of: $all\nOr: $allowedPUTs")
}

trait VPLAPIs { this: TestHttpClient =>
  lazy val keystoreBaseUrl = "http://localhost:8400/keystore"
  lazy val propertiesBaseUrl = "http://localhost:9524/property-linking/properties"
  lazy val propertyLinksBaseUrl = "http://localhost:9524/property-linking/property-links"
  lazy val ratesBillCheckBaseUrl = "http://localhost:9524/rates-bill-checks"
  lazy val fileUploadBaseUrl = "http://localhost:9524/file-uploads"
  lazy val propertyRepresentationLinksBaseUrl = "http://localhost:9524/property-linking/property-representations"
  lazy val accountsBaseUrl = "http://localhost:9524/property-linking/accounts"

  allowPUTsFor(keystoreBaseUrl)
  allowPUTsFor(propertyLinksBaseUrl)
  allowPOSTsFor(propertyLinksBaseUrl)

  def stubPropertiesAPI(uarn: String, p: Property) = {
    stubGet(s"$propertiesBaseUrl/$uarn", Seq.empty, HttpResponse(200, responseJson = Some(Json.toJson(p))))
  }

  def stubKeystoreSession(session: SessionDocument, accounts: Seq[Account])(implicit sid: SessionId) = {
    stubGet(
      s"$keystoreBaseUrl/voa-property-linking-frontend/$sid", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(CacheMap("", Map(SessionDocument.sessionKey -> Json.toJson(session))))))
    )
    stubGet(
      s"$accountsBaseUrl", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(accounts)))
    )
  }

  def verifyKeystoreSaved(session: SessionDocument)(implicit sid: SessionId) =
    verifyPUT(
      s"$keystoreBaseUrl/voa-property-linking-frontend/$sid/data/${SessionDocument.sessionKey}", Json.stringify(Json.toJson(Json.toJson(session)))
    )

  def verifyPropertyLinkRequest(uarn: String, accountId: String, request: LinkToProperty) =
    verifyPOST(
      s"$propertyLinksBaseUrl/$uarn/$accountId/UUID", Json.stringify(Json.toJson(request))
    )

  def verifyNoPropertyLinkRequest(billingAuthorifyReference: String, accountId: String) =
    verifyNoPUTsFor(s"$propertyLinksBaseUrl/$billingAuthorifyReference/$accountId/UUID")

  def verifyNoMoreLinkRequests(amount: Int) =
    verifyOnlyNPOSTsFor(propertyLinksBaseUrl, amount)

  def stubLinkedPropertiesAPI(accountId: String, added: Seq[PropertyLink], pending: Seq[PropertyLink]) = {
    stubGet(
      s"$propertyLinksBaseUrl/$accountId", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(added ++ pending)))
    )
  }

  def stubPropertyRepresentationAPI(accountId: String, uarn: String) = {
    stubGet(
      s"$propertyRepresentationLinksBaseUrl/$accountId/$uarn", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(Seq(PropertyRepresentation("id", "id", accountId, uarn, true, false, true)))))
    )
  }

  def stubRatesBillCheck(baRef: String, ratesBill: Array[Byte], ratesBillAccepted: Boolean) =
    stubPut(
      s"$ratesBillCheckBaseUrl/$baRef/UUID", Seq.empty,
      Json.stringify(Json.toJson(Map("data" -> Base64.getEncoder.encodeToString(ratesBill)))),
      HttpResponse(200, responseJson = Some(Json.toJson(Map("isValid" -> ratesBillAccepted))))
    )

  def stubFileUpload(accountId: String, sessionId: SessionId, key: String, files: (String, Array[Byte])*) = {
    val body = JsArray(files.map(f => Json.toJson(Map("name" -> f._1, "content" -> Base64.getEncoder.encodeToString(f._2)))))
    stubGet(
      s"$fileUploadBaseUrl/$accountId/$sessionId/$key", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(body)))
    )
  }

  def stubFileUploadWithNoFile(accountId: String, sessionId: SessionId, key: String) =
    stubGet(
      s"$fileUploadBaseUrl/$accountId/$sessionId/$key", Seq.empty,
      HttpResponse(404)
    )

  def stubAccounts(accounts: Seq[Account]) =
    stubGet(
      s"$accountsBaseUrl", Seq.empty,
      HttpResponse(200, responseJson = Some(Json.toJson(accounts)))
    )

}
