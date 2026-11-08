# 🏛️ Architecture Decision Records (ADRs)

This directory documents the foundational architectural and design decisions for **ShopKeeper**.
Each record explains the **Context**, **Decision**, **Rationale**, and **Consequences** of choices made in the codebase.

## 📋 Index of Architecture Decisions

| ADR | Title | Status | Date |
| :--- | :--- | :---: | :---: |
| [**ADR-0001**](0001-offline-first-single-source-of-truth.md) | Offline-First Single Source of Truth via Room and WorkManager | ✅ Accepted | 2026-11-01 |
| [**ADR-0002**](0002-monetary-precision-minor-units-long.md) | Monetary Precision via 64-bit Integer Minor Units (`Long` Cents) | ✅ Accepted | 2026-11-01 |
| [**ADR-0003**](0003-firestore-batch-chunking-400-ops.md) | Cloud Firestore Batch Write Chunking ($\le 400$ Operations) | ✅ Accepted | 2026-11-01 |
| [**ADR-0004**](0004-jetpack-compose-mvi-stateflow-presentation.md) | Jetpack Compose + MVI/MVVM Presentation with `StateFlow` | ✅ Accepted | 2026-11-01 |
| [**ADR-0005**](0005-trunk-based-branching-automated-ci-quality-gate.md) | Trunk-Based Branching, Pre-Push Hooks & Automated PR CI Quality Gate | ✅ Accepted | 2026-11-07 |

---

> [!IMPORTANT]
> **Instructions for AI Agents**: Before proposing structural refactorings or introducing new storage/pricing/network primitives, consult these ADRs. Changes that contradict accepted ADRs without an explicit approved superseding ADR are strictly forbidden.
