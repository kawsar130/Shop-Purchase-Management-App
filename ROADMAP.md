# 🗺️ ShopKeeper Production Readiness Roadmap

> **Engineering Vision:** Transform ShopKeeper from an MVP prototype into an enterprise-ready, robust, and secure Android retail management application ready for production client release on Google Play and private distribution.

---

## 📊 Sprint Status Dashboard

| Sprint | Focus Area | Status | Progress | Target Milestone |
| :--- | :--- | :---: | :---: | :--- |
| **Sprint 0** | Tooling Baseline & Test Safety Net | 🟢 COMPLETED | 100% | Gradle Wrapper, Testing Harness, Baseline Regression Suite |
| **Sprint 1** | Security & Data Integrity Foundation | 🟢 COMPLETED | 100% | Safe Room Migrations, Exact Money (`Long`), Firestore Rules |
| **Sprint 2** | Architecture & Reliability Modernization | 🟡 NEXT UP | 0% | Hilt DI, Clean Repo, Credential Manager, WorkManager |
| **Sprint 3** | Release Engineering & Observability | ⚪ PLANNED | 0% | Proguard/R8, Signing, Build Flavors, Crashlytics, LeakCanary |
| **Sprint 4** | UI/UX Quality, System Standards & i18n | ⚪ PLANNED | 0% | `strings.xml`, Dark Theme, Android 15 Edge-to-Edge, Type-Safe Nav |

---

## 🎯 High-Level Architecture Target

```
┌──────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│   Jetpack Compose (Material 3) + MVI/MVVM + Type-Safe Nav    │
│   (UiState + Sealed UiEvents + Localization via strings.xml) │
└──────────────────────────────▲───────────────────────────────┘
                               │
┌──────────────────────────────┴───────────────────────────────┐
│                    Domain Layer (Optional)                   │
│   UseCases (ProductValidation, PricingCalculations)          │
└──────────────────────────────▲───────────────────────────────┘
                               │
┌──────────────────────────────┴───────────────────────────────┐
│                      Data Layer                              │
│   Repositories (Hilt-Injected, Decoupled from Android Context)│
├──────────────────────────────┬───────────────────────────────┤
│    Local Data Source         │    Remote Data Source         │
│    - Room Database           │    - Cloud Firestore          │
│    - Safe Migrations (Long)  │    - Batch Chunking (<=400)   │
│    - Preferences DataStore   │    - Credential Manager       │
└──────────────────────────────┴───────────────────────────────┘
                               │
┌──────────────────────────────┴───────────────────────────────┐
│                  Background Sync Engine                      │
│   WorkManager (Guaranteed Offline-First Sync with Constraints)│
└──────────────────────────────────────────────────────────────┘
```

---

## 🛡️ Drift Prevention & Governance Rules

To ensure that development progresses without regressions, missed edge-cases, or half-finished tasks:

1. **Strict Dependency Order:**
   - Tasks must be executed in order: **Sprint 0 ➔ 1 ➔ 2 ➔ 3 ➔ 4**.
   - No feature from a later sprint may be started until the current sprint's Definition of Done (DoD) is satisfied.

2. **Branching Strategy (One Sprint = One Epic Branch):**
   - Epic branch format: `epic/sprint-<number>-<name>` (e.g., `epic/sprint-0-tooling-baseline`).
   - Task branch format: `task/S<sprint>-T<task>-<slug>` (e.g., `task/S0-T1-gradle-wrapper`).
   - PRs merge into the `epic/*` branch with task checklist verification.
   - The `epic/*` branch merges to `main` only when all tasks in the sprint are `[x]` marked.

3. **Universal Quality Gate (Every PR Must Pass):**
   ```bash
   ./gradlew clean lintDebug testDebugUnitTest assembleDebug
   ```
   No PR may be merged if it introduces compile warnings as errors, lint errors, or failing tests.

4. **Detailed Tracking Document:**
   See [`docs/sprints/SPRINT_TRACKER.md`](docs/sprints/SPRINT_TRACKER.md) for the complete, granular task-by-task checklist with acceptance criteria, impacted files, and verification commands.
