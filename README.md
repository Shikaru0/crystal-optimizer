# crystal-optimizer
Optimizes client-sided packets related to Crystal PvP

## What is it?
A lightweight Fabric mod for crystal PvP.
Allows 2 attacks per tick (1 if ping <50ms) on crystals by removing entity client-side after first hit.

## Server opt-out
Uses same opt-out implementation as [fast-xp](https://github.com/Shikaru0/fast-xp),
but with its own id:
```java
public static final CustomPayload.Id<OptOutPayload> ID =
new CustomPayload.Id<>(Identifier.of("crystaloptimizer", "opt_out"));
```