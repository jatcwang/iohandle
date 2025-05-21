package io.github.jatcwang.iohandle

import cats.effect.IO
import cats.mtl.{Handle, Raise}

import scala.compiletime.summonFrom

type IORaise[-E] = Raise[IO, E]
type IOHandle[E] = Handle[IO, E]

inline def ioHandling[E]: IOHandlePartiallyApplied[E] = {
  val handle = impl.createIOHandle[E]
  new IOHandlePartiallyApplied[E](handle)
//  summonFrom {
//    case handle: IOHandle[E] => {
//      new IOHandlePartiallyApplied[E](handle)
//    }
//    case given IORaise[E] => scala.compiletime.error("A Raise[IO, E] is already in scope. FIXME Consider reusing it ")
//    case _ =>
//      val handle = impl.createIOHandle[E]
//      new IOHandlePartiallyApplied[E](handle)
//  }
}

inline def ioAbort[E, E1 <: E](e: E1)(using raise: IORaise[E]): IO[Nothing] = raise.raise(e)
