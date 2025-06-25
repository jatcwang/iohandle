package io.github.jatcwang.iohandle
import cats.Applicative
import cats.effect.IO
import cats.mtl.{Handle, Raise}
import cats.syntax.all.*

import scala.util.control.NoStackTrace

class IOHandleImpl[E] private[iohandle] (marker: AnyRef) extends IOHandle[E]:
  self =>

  override def handleWith[A](fa: IO[A])(f: E => IO[A]): IO[A] =
    fa.handleErrorWith {
      case Submarine(e, m) if m == marker => f(e.asInstanceOf[E])
      case e                              => IO.raiseError(e)
    }

  override def raise[E2 <: E, A](e: E2): IO[A] = IO.raiseError(Submarine(e, marker))

final private[iohandle] case class Submarine[E](e: E, marker: AnyRef) extends RuntimeException with NoStackTrace

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
