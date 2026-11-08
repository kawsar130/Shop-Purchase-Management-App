# ADR-0002: Monetary Precision via 64-bit Integer Minor Units (`Long` Cents)

## Status
**Accepted** (2026-11-01)

## Context
Standard IEEE 754 floating-point types (`Double`, `Float`) suffer from binary fractional representation limits (e.g., `0.1 + 0.2 = 0.30000000000000004`). In a retail application, rounding errors in cost, price, stock valuation, and profit margins compound over thousands of transactions, leading to severe accounting discrepancies.
Alternative: `BigDecimal` introduces heavy memory allocation overhead, lacks native Room/SQLite scalar indexing, and degrades performance in large inventory list recalculations.

## Decision
- All purchase/buying and sales/selling prices, discounts, and margins are stored in data entities and domain models as 64-bit signed integers (`Long`) representing the **minor unit** of currency (cents, poisha, pence, etc.):
  ```kotlin
  val purchasePriceMinor: Long,
  val salesPriceMinor: Long
  ```
- Floating-point `Double` and `Float` are strictly prohibited for monetary calculations and Room database fields.
- Conversion to human-readable strings occurs exclusively at UI presentation boundaries using [`FormatUtils`](../../app/src/main/java/com/shopkeeper/ui/util/FormatUtils.kt).
- Enforced at compile-time and via automated architecture assertions in [`ArchitectureInvariantsTest`](../../app/src/test/java/com/shopkeeper/architecture/ArchitectureInvariantsTest.kt).

## Consequences
- **Positive**: Zero rounding errors; zero floating-point drift; fast primitive CPU arithmetic; compact SQLite integer storage; indexed searches on prices are instantaneous.
- **Negative**: Developers and AI agents must always remember that amounts are in cents (`150L` represents `$1.50`).
