# ADR-0003: Cloud Firestore Batch Write Chunking ($\le 400$ Operations)

## Status
**Accepted** (2026-11-01)

## Context
Google Cloud Firestore enforces a strict hard ceiling of **500 operations per write batch**. If an offline user creates or modifies more than 500 items, committing them in a single batch causes the entire transaction to crash with `IllegalArgumentException: A write batch can contain at most 500 operations`.
Additionally, keeping batches small reduces network payload size on flaky mobile cellular connections.

## Decision
In [`FirestoreService.kt`](../../app/src/main/java/com/shopkeeper/data/remote/FirestoreService.kt), all bulk operations (batch write, delete, export sync) are partitioned into chunks of **at most 400 operations**:
```kotlin
products.chunked(400).forEach { batchItems ->
    val batch = firestore.batch()
    // execute batch
    batch.commit().await()
}
```

## Consequences
- **Positive**: 100% safety buffer below Firestore's 500 limit; immunity against bulk import crashes; resilient recovery on mobile networks.
- **Negative**: Very large catalogs execute multiple sequential network commits.
