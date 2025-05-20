package io.github.jatcwang.iohandle

import cats.mtl.Raise
import cats.effect.IO
import scala.reflect.ClassTag

type IORaise[E] = Raise[IO, E]

inline def ioHandling[E]: IOHandlePartiallyApplied[E] =
  new IOHandlePartiallyApplied[E]

inline def ioAbort[E, E1 <: E](e: E1)(using raise: IORaise[E]): IO[Nothing] = raise.raise(e)
