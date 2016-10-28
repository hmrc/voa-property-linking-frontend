package useCaseSpecs.utils

import org.jsoup.Jsoup
import org.scalatest.{AppendedClues, MustMatchers}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.http.HeaderNames

object Page extends MustMatchers with AppendedClues {

  def get(url: String)(implicit sessionId: SessionId, session: GGSession): HtmlPage = {
    val Some(response) = route(FakeRequest("GET", url)
      .withHeaders(HeaderNames.xSessionId -> sessionId)
      .withSession(session.toSeq: _*))
    status(response) mustEqual 200 withClue s"Error for request: GET $url"
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  def getResult(url: String)(implicit sid: SessionId, session: GGSession): Result = {
    val Some(response) = route(FakeRequest("GET", url)
      .withHeaders(HeaderNames.xSessionId -> sid)
      .withSession(session.toSeq: _*))
    await(response)
  }

  def postValid(url: String, formData: (String, String)*)(implicit sid: SessionId, session: GGSession): Result = {
    val token = "Csrf-Token" -> "nocheck"
    val Some(response) = route(FakeRequest("POST", url)
      .withHeaders(HeaderNames.xSessionId -> sid)
      .withSession(session.toSeq :+ (token):_*)
      .withFormUrlEncodedBody(formData :+ (token): _*))
    await(response)
  }

  def postInvalid(url: String, formData: (String, String)*)(implicit sid: SessionId, session: GGSession): HtmlPage = {
    val token = "Csrf-Token" -> "nocheck"
    val Some(response) = route(FakeRequest("POST", url)
      .withHeaders(HeaderNames.xSessionId -> sid)
      .withSession(session.toSeq :+ (token):_*)
      .withFormUrlEncodedBody(formData :+ (token): _*))
    status(response) mustEqual 400
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  object NoSessionId extends Exception

}
