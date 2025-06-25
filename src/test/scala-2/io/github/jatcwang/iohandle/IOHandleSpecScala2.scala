package io.github.jatcwang.iohandle

import io.github.jatcwang.iohandle.testtypes._
import cats.effect._
import munit.CatsEffectSuite

class IOHandleSpecScala2 extends CatsEffectSuite {

    test("ioHandling: Handle errors") {

      val prog = ioHandling[MyError]:
        ioAbort(MyError.NotFound())
          .map(_ => Right(()))
      .rescueWith:
        case MyError.NotFound() => IO.pure(Left("not found"))
        case MyError.Broken() => IO.pure(Left("not good"))

      prog.assertEquals(Left("not found"))
    }

  }
