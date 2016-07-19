package useCaseSpecs.utils

import config.{VPLFrontendGlobal, Wiring}
import org.scalatest._
import play.api.mvc.EssentialFilter
import play.api.test.{DefaultAwaitTimeout, FakeApplication, FutureAwaits}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter

object FrontendTest {
  private val testConfigs = Map("auditing.enabled" -> false)
  val HTTP = new TestHttpClient()

  val g = new VPLFrontendGlobal {
    override def frontendFilters: Seq[EssentialFilter] = super.frontendFilters.filterNot(_.getClass == SessionCookieCryptoFilter.getClass)
    override val wiring = new Wiring {
      override val http = HTTP
    }
  }
  val app = FakeApplication(withGlobal = Some(g), additionalConfiguration = testConfigs)
  play.api.Play.start(app)
}

abstract class FrontendTest extends FreeSpec with MustMatchers with OptionValues with FutureAwaits
                            with DefaultAwaitTimeout with AppendedClues with BeforeAndAfterAll {
  implicit val app = FrontendTest.app
  val HTTP = FrontendTest.HTTP
}