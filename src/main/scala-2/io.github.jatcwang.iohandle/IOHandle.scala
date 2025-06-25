package io.github.jatcwang.iohandle

import cats.Applicative
import cats.effect.IO
import cats.mtl.{Handle, Raise}
import cats.syntax.all.*

trait IOHandle[E] extends Handle[IO, E] { self =>
  override def applicative: Applicative[IO] = IO.asyncForIO

  final def imap[E3](f: E => E3)(g: E3 => E): Handle[IO, E3] = new IOHandle[E3] {
    override def handleWith[A](fa: IO[A])(f2: E3 => IO[A]): IO[A] =
      self.handleWith(fa)(e => f2(f(e)))

    override def raise[E2 <: E3, A](e: E2): IO[A] =
      self.raise(g(e))
  }
}

