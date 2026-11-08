# ADR-0004: Jetpack Compose + MVI/MVVM Presentation with `StateFlow`

## Status
**Accepted** (2026-11-01)

## Context
Legacy Android View XML layouts require boilerplate data binding, fragile state synchronization, and complex lifecycle observation. Modern retail POS applications demand fluid animations, high-contrast dynamic themes (Dark/Light), Edge-to-Edge displays (Android 15 ready), and deterministic UI state rendering.

## Decision
- Standardize 100% of UI presentation on **Jetpack Compose** (Material 3).
- Architecture follows **MVI / MVVM** with Unidirectional Data Flow (UDF).
- ViewModels expose state strictly via immutable `StateFlow<UiState>`.
- Composables collect state using `collectAsStateWithLifecycle()` to prevent background battery drain.
- Zero hardcoded strings in `@Composable`—all strings must be defined in `strings.xml`.

## Consequences
- **Positive**: Declarative UI; automated Edge-to-Edge inset handling; deterministic UI rendering; high testability with Turbine and Compose testing.
- **Negative**: Requires strict discipline to avoid unnecessary recompositions (using stable models and keys in LazyColumns).
