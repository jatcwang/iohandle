package io.github.jatcwang.iohandle

import cats.effect.IO

trait IOHandlePlatform {

  def ioHandling[E]: IOHandlePartiallyApplied[E] = {
    val handle = impl.createIOHandle[E]
    new IOHandlePartiallyApplied[E](handle)
  }

  private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) {
    def apply[A](f: IOHandle[E] => IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
      f,
      handle,
    )
  }

  def ioAbort[E, E1 <: E](e: E1)(implicit raise: IORaise[E]): IO[Nothing] =
    raise.raise(e)

  // TODO:  eliminate allocation
//  private[iohandle] class IOHandlePartiallyApplied[E](val handle: IOHandle[E]) {
//    def apply[A](inline f: IOHandle[E] ?=> IO[A]): IOHandlePendingRescue[E, A] = new IOHandlePendingRescue(
//      convert(f),
//      handle,
//    )
//
//    private inline def convert[A, B](inline f: A ?=> B): A => B =
//    implicit a: A => f
//  }
//
//  inline def ioAbort[E, E1 <: E](e: E1)(using raise: IORaise[E]): IO[Nothing] = raise.raise(e)
//
//  extension [A](io: IO[A]) {
//    def recoverUnhandled[B >: A](pf: PartialFunction[Throwable, B]): IO[B] = io.recoverWith {
//      case e: Submarine[?] => IO.raiseError(e)
//      case e: Throwable    => io.recover(pf)
//    }

}
