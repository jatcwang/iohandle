# IOHandle - Ergonomic error handling for cats.effect.IO 

Status: Not yet released (snapshot only) / Looking for feedback

IOHandle is a small library that provides ergonomic type-safe error handling
for cats-effect IO.

It is based on cats-mtl's [Handle](https://typelevel.org/cats-mtl/mtl-classes/handle.html) and [Raise](https://typelevel.org/cats-mtl/mtl-classes/raise.html) capabilities,
but specialized for `cats.effect.IO` with some additional helpers and user-friendliness.

# Installation

```
libraryDepedencies ++= Seq("com.github.jatcwang" %% "iohandle" % "<VERSION>")
```

# Usage

Use `ioHandling` and specify an error type. Within the scope, you can use `ioAbort`
to short-circuit the execution. The error will be handled by the hander attached to
`ioHandling`. If you squint a bit, it is similar to `try` and `catch` but for cats.effect.IO!

Scala 3:
```
def checkIn(passport: PassportInfo): IO[Either[CheckInError, Unit]] = {
  (ioHandling[CheckInError] {
    for {
      validPassport <- checkPassport()
      _ <- if (!validPassport) ioAbort(InvalidPassport()) else IO.unit
      booking <- getBooking()
      _ <- if (booking.isEmpty) ioAbort(NoBooking("no booking found for passenger ${passport.name}")) else IO.unit
    }
    yield cartContent
  })
    .toEither
}
```

# Inspirations & Comparisons

- ["Submarine Error Handling" PR in cats-mtl](https://github.com/typelevel/cats-mtl/pull/619)
    - Difference: IOHandle library aims to provide additional safety/debugging, for example when the `Raise`/`Handle` 
      instance is leaked outside its original scope
- ValdemarGr's [catch-effect](https://github.com/ValdemarGr/catch-effect) library
    - Difference: We rely on `IO.raiseError` instead of IO cancellation

