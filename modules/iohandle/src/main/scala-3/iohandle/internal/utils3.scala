package iohandle.utils3

private[iohandle] inline def convert[A, B](inline f: A ?=> B): A => B =
  implicit a: A => f
