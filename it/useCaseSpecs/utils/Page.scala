package useCaseSpecs.utils

import org.jsoup.Jsoup
import org.scalatest.{AppendedClues, MustMatchers}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.{HeaderCarrier, HeaderNames}

object Page extends MustMatchers with AppendedClues {
  def get(url: String)(implicit hc: HeaderCarrier): HtmlPage = {
    val Some(response) = route(FakeRequest("GET", url)
                           .withHeaders(HeaderNames.xSessionId -> hc.sessionId.map(_.value).getOrElse(throw NoSessionId)))
    status(response) mustEqual 200 withClue s"Error for request: GET $url"
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  def postValid(url: String, formData: (String, String)*)(implicit hc: HeaderCarrier): Result = {
    val Some(response) = route(FakeRequest("POST", url)
                          .withHeaders(HeaderNames.xSessionId -> hc.sessionId.map(_.value).getOrElse(throw NoSessionId))
                          .withFormUrlEncodedBody(formData:_*))
    await(response)
  }

  def postInvalid(url: String, formData: (String, String)*)(implicit hc: HeaderCarrier): HtmlPage = {
    val Some(response) = route(FakeRequest("POST", url)
                          .withHeaders(HeaderNames.xSessionId -> hc.sessionId.map(_.value).getOrElse(throw NoSessionId))
                          .withFormUrlEncodedBody(formData:_*))
    status(response) mustEqual 400
    HtmlPage(Jsoup.parse(contentAsString(response)))
  }

  object NoSessionId extends Exception
}
