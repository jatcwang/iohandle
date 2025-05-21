package io.github.jatcwang.iohandle

import io.github.jatcwang.iohandle.testtypes.MyError
import io.github.jatcwang.iohandle.{ioAbort, ioHandling}
import munit.CatsEffectSuite
import cats.syntax.all.*
import cats.effect.IO

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

  test("ioHandling reuses existing IOHandle instance in scope") {
    ???
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

}
