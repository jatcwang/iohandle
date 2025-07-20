package iohandletest

import cats.effect.*
import iohandletest.testtypes.*
import io.github.jatcwang.iohandle.{ioAbort, ioHandling}
import munit.CatsEffectSuite

class IOHandleSpec extends CatsEffectSuite {

  test(".rescueWith success case") {

    val prog = ioHandling[MyError] { implicit handle =>
      val _ = handle
      IO("success")
    }
      .rescueWith {
        case MyError.NotFound() => IO.pure("not found")
        case MyError.Broken()   => IO.pure("not good")
      }

    prog.assertEquals("success")
  }

  test(".rescueWith error case") {

    val prog = ioHandling[MyError] { implicit handle =>
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    }
      .rescueWith {
        case MyError.NotFound() => IO.pure("not found")
        case MyError.Broken()   => IO.pure("not good")
      }

    prog.assertEquals("not found")
  }

  test(".toEither error case") {

    val prog = ioHandling[MyError] { implicit handle =>
      ioAbort(MyError.NotFound())
        .as("shouldn't have succeeded")
    }.toEither

    prog.assertEquals(Left(MyError.NotFound()))
  }

}
