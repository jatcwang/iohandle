package iohandletest

import cats.data.NonEmptyList
import cats.effect.IO
import iohandle.*
import munit.CatsEffectSuite

class FixmeSpec extends CatsEffectSuite {

  test("fixme") {
    val prog = ioHandlingAccum[String]:
      val raiseAccum = summon[IORaiseAccum[String]]

      for
        _ <- raiseAccum.accumError("boo")
        _ <- raiseAccum.accumError("ba")
        _ <- raiseAccum.abortAccum
        _ <- IO.pure(42) // should not reach here
      yield ()
    .toEither
    prog.assertEquals(Left(NonEmptyList.of("ba", "boo")))
  }
}
