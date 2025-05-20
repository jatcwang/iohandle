package io.github.jatcwang.iohandle
import cats.effect.IO
import cats.mtl.Handle
import cats.Applicative
import scala.util.control.NoStackTrace

class IOHandle[E] private[iohandle] (marker: AnyRef) extends Handle[IO, E]:
  override def applicative: Applicative[IO] = IO.asyncForIO

  override def handleWith[A](fa: IO[A])(f: E => IO[A]): IO[A] =
    fa.handleErrorWith {
      case Submarine(e, m) if m == marker => f(e.asInstanceOf[E])
      case e                              => IO.raiseError(e)
    }

  override def raise[E2 <: E, A](e: E2): IO[A] = IO.raiseError(Submarine(e, marker))

final private[iohandle] case class Submarine[E](e: E, marker: AnyRef) extends RuntimeException with NoStackTrace

private[iohandle] class IOHandlePartiallyApplied[E] {
  inline def apply[A](inline f: Handle[IO, E] ?=> IO[A]) = new HandledIO(convert(f))

  private inline def convert[A, B](inline f: A ?=> B): A => B =
    implicit a: A => f
}

private[iohandle] class HandledIO[E, A](private val body: Handle[IO, E] => IO[A]) extends AnyVal {
  def createIOHandle: IOHandle[E] =
    new IOHandle[E](new AnyRef)
    
  def rescueWith(handler: E => IO[A]): IO[A] = {
    val ioHandle = createIOHandle

    ioHandle.handleWith(body(ioHandle))(handler)
  }

  inline def toEither: IO[Either[E, A]] = {
    val ioHandle = createIOHandle

    ioHandle.handleWith(body(ioHandle).map(Right(_)))(e => IO.pure(Left(e)))
  }
}
