package io.github.jatcwang

import cats.mtl.Raise
import cats.effect.IO

package object iohandle extends IOHandlePlatform {

  type IORaise[-E] = Raise[IO, E]

}
