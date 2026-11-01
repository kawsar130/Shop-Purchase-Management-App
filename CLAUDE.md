# Claude Code Guidelines

> See [`AGENTS.md`](./AGENTS.md) for the authoritative project instructions and architectural invariants.

## Essential Invariants
1. **Monetary Precision**: All prices and financial arithmetic MUST be in 64-bit integer minor units (`Long` cents). NEVER use `Double` or `Float` for money.
2. **Localization**: Never hardcode UI text strings. Always use `res/values/strings.xml`.
3. **Terminology**: Always use **"Buying Price"** (cost) and **"Selling Price"** (retail).
4. **Single Source of Truth**: Room is the single source of truth. UI observes Room, never Cloud Firestore directly.

## Verification Commands
- Unit tests: `./gradlew testProdDebugUnitTest`
- Android Lint: `./gradlew lintProdDebug`
- Release build: `./gradlew assembleProdRelease`
