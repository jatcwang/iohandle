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
import iohandle.*

def transferMoney(source: AccountId, dest: AccountId, currency: Currency, amount: Amount): IO[Either[TransferError, Unit]] =
  ioHandling[TransferError]:
    for
      _ <- checkTransferDestination(dest, currency)
      _ <- makeTransfer(source, dest, currency, amount)
    yield ()  
  .toEither
  
def checkTransferDestination(dest: AccountId, currency: Currency)(using IORaise[UnsupportedCurrency | InvalidAccountDetails]): IO[Unit] =
  for
    accDetail <- fetchAccountDetails(dest).unwrapOrRaise(InvalidAccountDetails(dest))
    _ <- ioAbortIf(!accDetail.supportedCurrencies.contains(currency), UnsupportedCurrency(dest, currency))
```

# Error handling example: Uploading a file

Below is a small end-to-end example showing how to model and handle errors when uploading a file. The two possible domain errors are FileTooLarge and QuotaExceeded.

Scala 3:
```
import cats.effect.IO
import iohandle.*

def uploadFile(userId: UserId, parentPath: Path, file: File): IO[Either[UploadError, String]] =
  ioHandling[UploadError]:
    for
      _ <- if (file.size > MaxPerFileBytes) 
             ioAbort(FileTooLarge(MaxPerFileBytes, file.size))
           else IO.unit

      used <- getUsedQuota(userId)
      remaining = MaxUserQuotaBytes - used
      // ioAbortIf is a equivalent to `if (..) ioAbort(..) else IO.unit`
      _ <- ioAbortIf(remaining < file.size, QuotaExceeded(userId, remaining))

      url <- saveToStorage(userId, file)
    yield url
  .toEither
```

# Inspirations & Comparisons

- ["Submarine Error Handling" PR in cats-mtl](https://github.com/typelevel/cats-mtl/pull/619)
    - Difference: IOHandle library aims to provide additional safety/debugging, for example when the `Raise`/`Handle` 
      instance is leaked outside its original scope
- ValdemarGr's [catch-effect](https://github.com/ValdemarGr/catch-effect) library
    - Difference: We rely on `IO.raiseError` instead of IO cancellation
