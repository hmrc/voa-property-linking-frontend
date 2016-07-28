package useCaseSpecs.utils

import org.jsoup.Jsoup
import org.scalatest.{AppendedClues, MustMatchers}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF
import uk.gov.hmrc.play.http.HeaderNames

object Page extends MustMatchers with AppendedClues {

  def get(url: String)(implicit sid: SessionID, aid: AccountID): HtmlPage = {
    val Some(response) = route(FakeRequest("GET", url)
                           .withHeaders(HeaderNames.xSessionId -> sid)
                           .withSession("accountId" -> aid.id)) // TODO - remove account ID when auth finalised
    status(response) mustEqual 200 withClue s"Error for request: GET $url"
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  def postValid(url: String, formData: (String, String)*)(implicit sid: SessionID, aid: AccountID): Result = {
    val token = CSRF.SignedTokenProvider.generateToken
    val Some(response) = route(FakeRequest("POST", url)
                          .withHeaders(HeaderNames.xSessionId -> sid)
                          .withSession("csrfToken" -> token, "accountId" -> aid) // TODO - remove account ID when auth finalised
                          .withFormUrlEncodedBody(formData :+ (CSRF.TokenName -> token):_*))
    await(response)
  }

  def postInvalid(url: String, formData: (String, String)*)(implicit sid: SessionID, aid: AccountID): HtmlPage = {
    val token = CSRF.SignedTokenProvider.generateToken
    val Some(response) = route(FakeRequest("POST", url)
                          .withHeaders(HeaderNames.xSessionId -> sid)
                          .withSession("csrfToken" -> token, "accountId" -> aid) // TODO - remove account ID when auth finalised
                          .withFormUrlEncodedBody(formData :+ (CSRF.TokenName -> token) :_*))
    status(response) mustEqual 400
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  object NoSessionId extends Exception
}
