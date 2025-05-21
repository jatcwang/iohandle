package io.github.jatcwang.iohandle

import cats.effect.IO
import cats.syntax.all.*
import io.github.jatcwang.iohandle.testtypes.MyError
import io.github.jatcwang.iohandle.{ioAbort, ioHandling}
import munit.CatsEffectSuite

class IOHandleSpec extends CatsEffectSuite {

  test("ioHandling: Handle errors") {

    val prog = ioHandling[MyError]:
      ioAbort(MyError.NotFound())
        .map(_ => Right(()))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))

    prog.assertEquals(Left("not found"))
  }

  test("Nesting works") {
    val prog =
      ioHandling[MyError.NotFound]:
        ioHandling[MyError]:
          oops()
        .rescueWith:
          case MyError.NotFound() => IO.pure(Left("inner"))
          case MyError.Broken()   => IO.pure(Left("broken"))
      .rescueWith:
        case MyError.NotFound() => IO.pure(Left("outer"))

    prog.assertEquals(Left("outer"))
  }

  test("ioHandling fails to compile if a Raise[IO, E] (but not Handle[IO, E]) instance is already in scope") {
    val io = ioHandling[MyError]:
      ioHandling[MyError.NotFound]:
        ioAbort(MyError.NotFound()).map(_ => Right(()))
      .rescueWith:
        case MyError.NotFound() => IO.pure(Left("not found inner"))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))
    io.assertEquals(Left("not found"))
  }

  test("ioHandle recoverUnhandled") {
    val io = ioHandling[String]:
      boom.recoverUnhandled:
        case _ => Right(())
    .rescueWith(str => IO.pure(Left(str)))

    io.assertEquals(Left("boom!"))
  }

  def oops()(using IORaise[MyError]): IO[Int] = ioAbort(MyError.NotFound())
  def boom(using IORaise[String]): IO[Nothing] = ioAbort("boom!")
}
