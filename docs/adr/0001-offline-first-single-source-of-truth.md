# ADR-0001: Offline-First Single Source of Truth via Room and WorkManager

## Status
**Accepted** (2026-11-01)

## Context
Retail shops frequently experience intermittent or zero internet connectivity. Shop owners and cashiers must be able to search products, look up Buying/Selling prices, record restocks, and update inventory with sub-10ms response times without being blocked by network requests or offline errors.

## Decision
1. **Local Single Source of Truth (SSOT)**:
   - The local Room SQLite database (`AppDatabase`) is the **only** data source queried or observed by the presentation layer.
   - The UI never calls Cloud Firestore directly.
2. **Immediate Local Persistence**:
   - Any write (add, edit, delete, restock) is committed immediately to Room with `isPendingSync = true`.
3. **Guaranteed Background Synchronization**:
   - Android Jetpack `WorkManager` (`SyncWorker`) handles synchronization when connectivity is available, applying exponential backoff and network constraints.

## Consequences
- **Positive**: Sub-10ms instantaneous UI responses; full app functionality when offline; zero ANRs from network waits.
- **Negative**: Requires conflict resolution and tracking `isPendingSync` flags locally.
