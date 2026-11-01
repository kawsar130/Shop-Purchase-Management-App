# 🗺️ ShopKeeper Production Roadmap

> **Engineering Vision:** Transform ShopKeeper from an MVP prototype into an enterprise-ready, robust, and secure Android retail management and Point-of-Sale (POS) application ready for commercial deployment.

---

## 📊 Milestone Status Dashboard

### Milestone 1.0: Production Readiness (Completed 🟢)
| Sprint | Focus Area | Status | Progress | Target Milestone |
| :--- | :--- | :---: | :---: | :--- |
| **Sprint 0** | Tooling Baseline & Test Safety Net | 🟢 COMPLETED | 100% | Gradle Wrapper, Testing Harness, Baseline Regression Suite |
| **Sprint 1** | Security & Data Integrity Foundation | 🟢 COMPLETED | 100% | Safe Room Migrations, Exact Money (`Long`), Firestore Rules |
| **Sprint 2** | Architecture & Reliability Modernization | 🟢 COMPLETED | 100% | Hilt DI, Clean Repo, Credential Manager, WorkManager |
| **Sprint 3** | Release Engineering & Observability | 🟢 COMPLETED | 100% | Proguard/R8, Signing, Build Flavors, Crashlytics, LeakCanary |
| **Sprint 4** | UI/UX Quality, System Standards & i18n | 🟢 COMPLETED | 100% | `strings.xml`, Dark Theme, Android 15 Edge-to-Edge, Type-Safe Nav |

---

### Milestone 2.0: Commercial Evolution & Advanced Capabilities (Active 🚀)
*See detailed engineering design in [docs/V2_EVOLUTION_SPEC.md](docs/V2_EVOLUTION_SPEC.md).*

| Phase | Focus Area | Status | Progress | Key Deliverables & Skills |
| :---: | :--- | :---: | :---: | :--- |
| **Phase 1** | Baseline Audits & Binary Optimization | 🟢 COMPLETED | 100% | `play-policy-insights`, `r8-analyzer`, reports archived in `docs/reports/` |
| **Phase 2** | Retail Utility (Barcode Scanner + Shortcuts) | ⚪ PENDING | 0% | `camerax`, ML Kit Barcode Scanning, Launcher Quick Shortcuts |
| **Phase 3** | Adaptive Layouts & Tablet POS | ⚪ PENDING | 0% | `adaptive`, Two-Pane List-Detail Dashboard, Foldable support |
| **Phase 4** | On-Device Inventory AI Assistant | ⚪ PENDING | 0% | `ml-kit-genai-prompt-api`, `litert-runtime`, Local NLP query parser |

---

## 🎯 Architecture Target (v2.0 Evolution)

```
┌──────────────────────────────────────────────────────────────┐
│                    Presentation Layer                        │
│   Jetpack Compose (Material 3) + MVI/MVVM + Type-Safe Nav    │
│   (Two-Pane Adaptive POS + CameraX Barcode Viewfinder)       │
└──────────────────────────────▲───────────────────────────────┘
                               │
┌──────────────────────────────┴───────────────────────────────┐
│                    Intelligence Layer                        │
│   On-Device AI Assistant (LiteRT / Gemini Nano Prompt API)   │
│   (Local Context Serializer - 100% Private, Zero Cloud Bills)│
└──────────────────────────────▲───────────────────────────────┘
                               │
┌──────────────────────────────┴───────────────────────────────┐
│                      Data Layer                              │
│   Repositories (Hilt-Injected, Decoupled from Android Context)│
├──────────────────────────────┬───────────────────────────────┤
│    Local Data Source         │    Remote Data Source         │
│    - Room Database (SSOT)    │    - Cloud Firestore          │
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

## 🛡️ Governance Rules & Quality Gates

1. **Strict Phase Dependency Order:**
   - Tasks must be executed in order: **Phase 1 ➔ 2 ➔ 3 ➔ 4**.
   - No feature from a later phase may be merged until the current phase's verification criteria are met.

2. **Universal Quality Gate (Every PR Must Pass):**
   ```bash
   ./gradlew clean lintProdDebug testProdDebugUnitTest assembleProdDebug
   ```
   No change may be merged if it introduces compile warnings as errors, lint errors, or failing unit tests.

3. **Detailed Specifications:**
   - Full technical specification: [`docs/V2_EVOLUTION_SPEC.md`](docs/V2_EVOLUTION_SPEC.md)
   - Sprint tracking checklist: [`docs/sprints/SPRINT_TRACKER.md`](docs/sprints/SPRINT_TRACKER.md)
   - Architectural reference: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
