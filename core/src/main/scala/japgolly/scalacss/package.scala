package japgolly

import scala.collection.GenTraversableOnce
import scalaz.{Equal, OneAnd, Traverse1}
import scalaz.std.AllInstances._
import shapeless.lens

package object scalacss {

  /**
   * A CSS value, like `"none"`, `"solid 3px black"`.
   */
  type Value = String

  private final val _important = " !important"

  final case class AV(attr: Attr, value: Value) {
    def important: AV =
      if (value endsWith _important)
        this
      else
        copy(value = this.value + _important)

    @inline def apply(env: Env) =
      attr.gen(env)(value)
  }

  type AVs = NonEmptyVector[AV]

  final case class ClassName(value: String)
  implicit def classNameEquality: Equal[ClassName] = Equal.equalA

  /**
   * Describes the context of a number of CSS attribute-value pairs.
   *
   * Examples: `"div"`, `".debug"`, `"h3.bottom"`, `"a:visited"`.
   */
  type CssSelector = String

  /**
   * A CSS attribute and its corresponding value.
   *
   * Example: `CssKV("margin-bottom", "12px")`
   */
  final case class CssKV(key: String, value: String)
  object CssKV {
    implicit val equality: Equal[CssKV] = Equal.equalA

    type Lens = shapeless.Lens[CssKV, String]
    val key  : Lens = lens[CssKV].key
    val value: Lens = lens[CssKV].value
  }

  /**
   * A media query in CSS.
   *
   * Examples: `"@media screen and (device-aspect-ratio: 16/9)"`.
   */
  type CssMediaQuery  = String
  type CssMediaQueryO = Option[CssMediaQuery]

  case class CssEntry(mq     : CssMediaQueryO,
                      sel    : CssSelector,
                      content: NonEmptyVector[CssKV])
  object CssEntry {
    implicit val equality: Equal[CssEntry] = {
      val A = Equal[CssMediaQueryO]
      val B = Equal[CssSelector]
      val C = Equal[NonEmptyVector[CssKV]]
      new Equal[CssEntry] {
        override val equalIsNatural =
          A.equalIsNatural
        override def equal(a: CssEntry, b: CssEntry): Boolean =
          B.equal(a.sel, b.sel) &&
          A.equal(a.mq, b.mq) &&
          C.equal(a.content, b.content)
      }
    }
  }

  /**
   * A stylesheet in its entirety. Normally turned into a `.css` file or a `&lt;style&gt;` tag.
   */
  type Css = Stream[CssEntry]
  implicit val cssEquality: Equal[Css] = streamEqual

  type WarningMsg = String
  final case class Warning(cond: Cond, msg: WarningMsg)

  /**
   * Applicable style.
   *
   * A style that needs no more processing and can be applied to some target.
   *
   * @param addClassNames Additional class names that the style has requested be appended.
   *                      Allows ScalaCSS styles to use classname-based CSS libraries like Bootstrap.
   */
  final case class StyleA(className: ClassName, addClassNames: Vector[ClassName], style: StyleS) {
    /** Value to be applied to a HTML element's `class` attribute. */
    val htmlClass: String =
      (className.value /: addClassNames)(_ + " " + _.value)
  }

  // ===================================================================================================================
  type NonEmptyVector[A] = OneAnd[Vector, A]

  object NonEmptyVector {
    @inline def apply[A](h: A, t: A*): NonEmptyVector[A] =
      OneAnd(h, t.toVector)

    def end[A](init: Vector[A], last: A): NonEmptyVector[A] =
      if (init.isEmpty)
        OneAnd(last, Vector.empty)
      else
        OneAnd(init.head, init.tail :+ last)

    @inline def maybe[A, B](v: Vector[A], empty: => B)(f: NonEmptyVector[A] => B): B =
      if (v.isEmpty) empty else f(OneAnd(v.head, v.tail))

    @inline def option[A](v: Vector[A]): Option[NonEmptyVector[A]] =
      maybe[A, Option[NonEmptyVector[A]]](v, None)(Some.apply)
  }

  implicit val nonEmptyVectorTraverse1: Traverse1[NonEmptyVector] =
    OneAnd.oneAndTraverse[Vector]

  implicit def nonEmptyVectorEquality[A: Equal]: Equal[NonEmptyVector[A]] =
    OneAnd.oneAndEqual[Vector, A]

  @inline implicit class NonEmptyVectorExt[A](val self: NonEmptyVector[A]) extends AnyVal {
    @inline def modt(f: Vector[A] => Vector[A]): NonEmptyVector[A] =
      OneAnd(self.head, f(self.tail))

    @inline def :+(a: A): NonEmptyVector[A] =
      modt(_ :+ a)

    @inline def +:(a: A): NonEmptyVector[A] =
      OneAnd(a, self.head +: self.tail)

    @inline def ++(as: GenTraversableOnce[A]): NonEmptyVector[A] =
      modt(_ ++ as)

    @inline def ++(b: NonEmptyVector[A]): NonEmptyVector[A] =
      ++(b.vector)

    def ++:(as: Vector[A]): NonEmptyVector[A] =
      if (as.isEmpty) self else OneAnd(as.head, as.tail ++ vector)

    @inline def vector: Vector[A] =
      self.head +: self.tail
  }

  /** Faster than Vector(a) */
  @inline private[scalacss] def Vector1[A](a: A): Vector[A] =
    Vector.empty :+ a
}