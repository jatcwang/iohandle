package iohandle
import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.Ref

import iohandle.utils3.convert

import scala.util.control.NoStackTrace

trait IORaiseAccum[-E] {
  def accumError(e: E): IO[Unit]
  def abortAccum: IO[Nothing]
}

trait IOHandleAccum[E] extends IORaiseAccum[E] {
  def handle[A](io: IO[A])(handleErrors: NonEmptyList[E] => IO[A]): IO[A]
}

object IOHandleAccum {

  final class IOHandleAccumAbort extends Throwable with NoStackTrace {
    override def getMessage: String = s"fixme"
  }

  class IOHandleAccumPartiallyApplied[E] {
    inline def apply[A](inline body: IORaiseAccum[E] ?=> IO[A]): IOHandleAccumPendingRescue[E, A] = {
      // FIXME:
      import cats.effect.unsafe.implicits.global
      val ref = Ref.of[IO, List[E]](List.empty).unsafeRunSync()
      // FIXME: no anonymous class
      val ioHandleAccum = new IOHandleAccum[E] {
        override def handle[A](io: IO[A])(handleErrors: NonEmptyList[E] => IO[A]): IO[A] = {
          io.recoverWith { case _: IOHandleAccumAbort =>
            ref.get.flatMap {
              case Nil => IO.raiseError(new RuntimeException("IOHandleAccumAbort raised but no errors accumulated"))
              case head :: tail =>
                val nel = NonEmptyList(head, tail)
                handleErrors(nel)
            }
          }
        }

        override def accumError(e: E): IO[Unit] = ref.update(errors => e :: errors)

        override def abortAccum: IO[Nothing] = IO.raiseError(new IOHandleAccumAbort)

      }

      new IOHandleAccumPendingRescue(convert(body), ioHandleAccum)
    }
  }

  class IOHandleAccumPendingRescue[E, A](body: IORaiseAccum[E] => IO[A], ioHandleAccum: IOHandleAccum[E]) {
    def toEither: IO[Either[NonEmptyList[E], A]] = {
      ioHandleAccum.handle[Either[NonEmptyList[E], A]](body(ioHandleAccum).map(Right(_)))(errors =>
        IO.pure(Left(errors)),
      )
    }
  }

}
