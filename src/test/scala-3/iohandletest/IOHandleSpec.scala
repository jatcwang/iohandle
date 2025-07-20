package iohandletest

import cats.effect.IO
import cats.syntax.all.*
import iohandletest.testtypes.*
import io.github.jatcwang.iohandle.{ioHandling, ioAbort, IORaise, recoverUnhandled}
import munit.CatsEffectSuite

class IOHandleSpec extends CatsEffectSuite {

  test(".rescueWith success case") {
    val prog = ioHandling[MyError]:
      IO(Right("success"))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))

    prog.assertEquals(Right("success"))
  }

  test(".rescueWith error case") {

    val prog = ioHandling[MyError]:
      ioAbort(MyError.NotFound())
        .map(_ => Right(()))
    .rescueWith:
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))

    prog.assertEquals(Left("not found"))
  }

  test(".toEither error case") {
    val prog = ioHandling[MyError]:
      ioAbort(MyError.NotFound())
      .as("shouldn't have succeeded")
    .toEither

    prog.assertEquals(Left(MyError.NotFound()))
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

    prog.assertEquals(Left("inner"))
  }

  test("ioHandle recoverUnhandled") {
    val io = ioHandling[String]:
      boomStr.recoverUnhandled:
        case _ => Right(())
    .rescueWith(str => IO.pure(Left(str)))

    io.assertEquals(Left("boom!"))
  }

  def oops()(using IORaise[MyError]): IO[Int] = ioAbort(MyError.NotFound())
  def boomStr(using IORaise[String]): IO[Nothing] = ioAbort("boom!")
}
