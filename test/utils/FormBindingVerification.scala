package utils

import org.scalatest.{AppendedClues, MustMatchers}
import play.api.data.Form

object FormBindingVerification extends MustMatchers with AppendedClues {

  def mustBindTo[A](form: Form[A], result: A) {
    form.value.map(_ mustEqual result) getOrElse fail(s"Form did not bind. \nErrors: ${form.errors} \nData: ${form.data}")
    assert(form.errors.isEmpty, s"Form unexpectedly contained errors. \nErrors: ${form.errors} \nData: ${form.data}")
  }

}
