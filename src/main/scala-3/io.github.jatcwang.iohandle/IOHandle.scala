package io.github.jatcwang.iohandle
import cats.effect.IO
import cats.syntax.all.*

private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) extends AnyVal {
  inline def apply[A](inline f: IOHandle[E] ?=> IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
    convert(f),
    handle,
  )

  private inline def convert[A, B](inline f: A ?=> B): A => B =
    implicit a: A => f
}

private object impl:
  def createIOHandle[E]: IOHandle[E] =
    new IOHandleImpl[E](new AnyRef)
end impl

// TODO: eliminate allocation?
private[iohandle] class IOHandlePendingRescue[E, A](
  private val body: IOHandle[E] => IO[A],
  private val ioHandle: IOHandle[E],
) {

  def rescueWith(handler: E => IO[A]): IO[A] = {
    ioHandle.handleWith(body(ioHandle))(handler)
  }

  def toEither: IO[Either[E, A]] = {
    ioHandle.handleWith(body(ioHandle).map(Right(_)))(e => IO.pure(Left(e)))
  }
}
