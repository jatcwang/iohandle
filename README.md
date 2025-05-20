# IOHandle - Ergonomic error handling for cats.effect.IO 

Status: Not yet released / open for critique

IOHandle is a small library that provides ergonomic type-safe error handling
for cats-effect IO.

It is based on cats-mtl's [Handle](https://typelevel.org/cats-mtl/mtl-classes/handle.html) and [Raise](https://typelevel.org/cats-mtl/mtl-classes/raise.html) capabilities,
but specialized for `cats.effect.IO` with some additional safety.

# Inspirations & Comparisons

- ["Submarine Error Handling" PR in cats-mtl](https://github.com/typelevel/cats-mtl/pull/619)
    - Difference: We provide additional safety/debugging in case the Raise/Handle capability is leaked outside its context
- ValdemarGr's [catch-effect](https://github.com/ValdemarGr/catch-effect) library
    - Difference: We rely on `IO.raiseError` instead of IO cancellation

