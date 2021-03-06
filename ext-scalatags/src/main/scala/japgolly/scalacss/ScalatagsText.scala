package japgolly.scalacss

import scalatags.Text._
import all._

trait ScalatagsTextImplicits {

  implicit final def styleaToTextTag(s: StyleA): Modifier =
    cls := s.htmlClass

  implicit final def styleTextTagRenderer(implicit s: Renderer[String]): Renderer[TypedTag[String]] =
    new ScalatagsTextRenderer(s)
}

class ScalatagsTextRenderer(s: Renderer[String]) extends Renderer[TypedTag[String]] {
  override def apply(css: Css) =
    tags2.style(`type` := "text/css", s(css))
}
