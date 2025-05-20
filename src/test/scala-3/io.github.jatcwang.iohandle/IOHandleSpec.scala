package io.github.jatcwang.iohandle

import io.github.jatcwang.iohandle.testtypes.MyError
import io.github.jatcwang.iohandle.{ioAbort, ioHandling}
import munit.CatsEffectSuite
import cats.syntax.all.*
import cats.effect.IO

class IOHandleSpec extends CatsEffectSuite {

  test("Handle errors") {
    val io = ioHandling[MyError]:
      ioAbort(MyError.NotFound()).map(_ => Right(()))
    .rescueWith {
      case MyError.NotFound() => IO.pure(Left("not found"))
      case MyError.Broken()   => IO.pure(Left("not good"))
    }
    io.assertEquals(Left("not found"))
  }
}
