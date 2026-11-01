# GitHub Copilot Instructions

> Reference: See [`AGENTS.md`](../AGENTS.md) for full project rules, architecture, and standards.

## Critical Invariants
- **Monetary Calculations**: Stored as minor units `Long` cents. No `Double`/`Float` arithmetic for money. Format via `FormatUtils`.
- **String Localization**: No raw strings in Composables. Use `stringResource(R.string.key)`.
- **Terminology**: Always use **"Buying Price"** and **"Selling Price"**.
- **Offline SSOT**: UI queries local Room DB. Background sync handled by WorkManager.

## Verification
- Test: `./gradlew testProdDebugUnitTest`
- Lint: `./gradlew lintProdDebug`
