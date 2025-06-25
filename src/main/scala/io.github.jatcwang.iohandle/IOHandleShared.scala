package io.github.jatcwang.iohandle

import cats.mtl.Raise
import cats.effect.IO

object IOHandleShared {
  
  type IORaise[-E] = Raise[IO, E]

}
