package io.github.jatcwang.iohandle

import cats.Applicative
import cats.effect.IO
import cats.mtl.Handle

import scala.util.control.NoStackTrace

trait IOHandle[E] extends Handle[IO, E] { self =>
  override def applicative: Applicative[IO] = IO.asyncForIO

  final def imap[E3](f: E => E3)(g: E3 => E): Handle[IO, E3] = new IOHandle[E3] {
    override def handleWith[A](fa: IO[A])(f2: E3 => IO[A]): IO[A] =
      self.handleWith(fa)(e => f2(f(e)))

    override def raise[E2 <: E3, A](e: E2): IO[A] =
      self.raise(g(e))
  }
}

class IOHandleImpl[E] private[iohandle] (marker: AnyRef) extends IOHandle[E] { self =>
  override def handleWith[A](fa: IO[A])(f: E => IO[A]): IO[A] =
    fa.handleErrorWith {
      case Submarine(e, m) if m == marker => f(e.asInstanceOf[E])
      case e                              => IO.raiseError(e)
    }

  override def raise[E2 <: E, A](e: E2): IO[A] = IO.raiseError(Submarine(e, marker))
}

final private[iohandle] case class Submarine[E](e: E, marker: AnyRef) extends RuntimeException with NoStackTrace

private object impl {
  def createIOHandle[E]: IOHandle[E] =
    new IOHandleImpl[E](new AnyRef)
}

// TODO: eliminate allocation?
private[iohandle] class IOHandlePendingRescue[E, A](
  private val body: IOHandle[E] => IO[A],
  private val ioHandle: IOHandle[E],
) {

  def rescueWith(handler: E => IO[A]): IO[A] = {
    ioHandle.handleWith(body(ioHandle))(handler)
  }

  def toEither: IO[Either[E, A]] = {
    ioHandle.handleWith[Either[E, A]](body(ioHandle).map(Right(_)))(e => IO.pure(Left(e)))
  }
}
