An experiment in generating Java bytecode at runtime using
the [ASM library](https://asm.ow2.io/).

### Functionality

The library defines the `CompactList` interface,
which contains a small subset of the functionality of Kotlin's `MutableList`
interface.

The library also contains 2 implementations of `CompactList`:

* `CompactListGeneric<T>` - a statically defined dynamic array in pure Kotlin, storing values in `Array<T>`. The
  implementation can store arbitrary types of values, depending on the type parameter `T`.
* `CompactListInt` - a dynamic array implementation generated in byte code form at runtime, storing exclusively
  primitive `int` values.