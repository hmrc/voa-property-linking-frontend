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

package models.check

import java.net.URL
import java.text.MessageFormat.format

import play.api.mvc.Call
import Url._

sealed trait PathAdding {

  type Self <: PathAdding

  final def addPath(path: String): Self = addPath(Path.of(path))

  def addPath(path: Path): Self

  def resolveParams(firstParam: String, otherParams: String*): Self
}

sealed trait Url extends PathAdding {

  final type Self = Url

  def urlWithoutHost: Url
}

object Url {

  def apply(protocol: String, host: String, port: Int, path: String): Url =
    UrlWithHost(new URL(protocol, host, port, Path.of(path).withLeadingSlash.path))

  def apply(protocol: String, host: String, port: Int): Url =
    UrlWithHost(new URL(protocol, host, port, ""))

  def apply(baseUrl: String): Url =
    baseUrl.trim match {
      case ""                                  => BaseUrl(Path.of("/"))
      case base if base.startsWith("http://")  => UrlWithHost(new URL(baseUrl))
      case base if base.startsWith("https://") => UrlWithHost(new URL(baseUrl))
      case base if base.startsWith("/")        => BaseUrl(Path.of(base))
      case other =>
        throw new IllegalArgumentException(s"Base URL must be fully qualified or start with a slash, got: $other")
    }

  private case class UrlWithHost(url: URL) extends Url {

    private lazy val path = Path.of(s"${url.getPath}${Option(url.getQuery).map("?" + _).getOrElse("")}")

    lazy val urlWithoutHost: Url = Url(path.toString)

    override def addPath(pathToAdd: Path) =
      UrlWithHost(new URL(url.getProtocol, url.getHost, url.getPort, s"${path.addPath(pathToAdd).withLeadingSlash}"))

    override def resolveParams(firstParam: String, otherParams: String*) =
      UrlWithHost(
        new URL(
          s"${url.getProtocol}://${url.getAuthority}${urlWithoutHost.resolveParams(firstParam, otherParams: _*)}"
        )
      )

    override val toString = url.toString
  }

  private case class BaseUrl(base: Path, path: Path = Path.empty) extends Url {

    private lazy val fullUrl = base.addPath(path)

    lazy val urlWithoutHost = Url(fullUrl.toString)

    override def addPath(pathToAdd: Path) = BaseUrl(base, path.addPath(pathToAdd))

    override def resolveParams(firstParam: String, otherParams: String*) =
      BaseUrl(
        base.addPath(path).resolveParams(firstParam, otherParams: _*)
      )

    override lazy val toString = fullUrl.toString
  }

  case class Path private (path: String) extends PathAdding {

    type Self = Path

    lazy val withLeadingSlash: Path = if (path.isEmpty || path.startsWith("/")) this else Path(s"/$path")

    private lazy val withoutTrailingSlash: Path = if (path.endsWith("/")) Path(path.init) else this

    def isEmpty: Boolean = path.isEmpty

    def addPath(pathToAdd: Path): Path =
      if (isEmpty)
        pathToAdd
      else if (pathToAdd.isEmpty)
        this
      else
        Path(s"$withoutTrailingSlash${pathToAdd.withLeadingSlash}")

    def addQueryString(rawQueryString: String) =
      if (rawQueryString.isEmpty) this
      else if (path.endsWith("?") || path.endsWith("&")) Path(s"$path$rawQueryString")
      else if (path.contains("?")) Path(s"$path&$rawQueryString")
      else Path(s"$path?$rawQueryString")

    override def resolveParams(firstParam: String, otherParams: String*) =
      Path(
        format(path, firstParam +: otherParams: _*)
      )

    override val toString = path
  }

  object Path {

    def of(path: String): Path = Path(path.trim)

    def of(call: Call): Path = Path(call.url)

    val empty = Path("")
  }

}
