# 🤖 AGENTS.md — Instructions for AI Coding Agents

> **Project:** ShopKeeper — Enterprise-Grade, Offline-First Retail Inventory & Pricing Assistant for Android  
> **Target Audience:** All AI coding assistants (Antigravity, Gemini Code Assist, Claude Code, Cursor, Copilot Workspace, OpenAI Codex) and human engineers.

---

## 📌 Project Architecture & Tech Stack

- **Architecture:** Clean Architecture + MVI / MVVM with Unidirectional Data Flow (UDF).
- **Presentation:** Jetpack Compose (Material 3), Navigation Compose 2.7.7, Edge-to-Edge display (Android 15 & 16 ready).
- **Dependency Injection:** Dagger Hilt 2.51.1 (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
- **Data Layer:**
  - **Local (SSOT):** Room Database 2.6.1 (`AppDatabase` v3 with safe migrations, schema exported via KSP).
  - **Session & Settings:** Preferences DataStore 1.0.0.
  - **Remote:** Cloud Firestore (write batches chunked to $\le 400$ ops).
  - **Background Sync:** Android Jetpack WorkManager 2.9.0 with network constraints.
  - **Authentication:** Android Credential Manager API 1.3.0 + Google ID Token + Firebase Auth.
- **Build Variants:**
  - `dev` (`com.shopkeeper.dev`): "ShopKeeper Dev", LeakCanary 2.14 enabled.
  - `prod` (`com.shopkeeper`): "ShopKeeper", R8 minified, ProGuard obfuscated, Crashlytics.

---

## ⛔ The 8 Non-Negotiable Invariants

Any agent making changes to this repository **MUST STRICTLY ADHERE** to these invariants. Violations fail automated quality gates and will be rejected.

### 1. Monetary Precision Invariant
- **Strict Prohibition:** `Double` and `Float` **MUST NEVER** be used for monetary calculations, prices, or database persistence.
- **Minor Units (`Long`):** All purchase/buying and sales/selling prices must be represented as 64-bit integers (`Long`) in minor currency units (cents, poisha, pennies):
  ```kotlin
  val purchasePriceMinor: Long,
  val salesPriceMinor: Long
  ```
- **Formatting:** Convert to formatted currency strings strictly at presentation boundaries using [`FormatUtils`](app/src/main/java/com/shopkeeper/ui/util/FormatUtils.kt).

### 2. Localization & `UiText` Invariant
- **Zero Hardcoded Strings:** Hardcoded string literals in `@Composable` functions or dialogs are **STRICTLY FORBIDDEN**. Every user-facing text, error message, label, and title must be declared in [`res/values/strings.xml`](app/src/main/res/values/strings.xml) and accessed via `stringResource(R.string.key)`.
- **ViewModel Decoupling via `UiText`:** ViewModels must **never** reference Android `Context` or emit raw `String` for user feedback. All user notifications, snackbars, toasts, and errors must emit typed [`UiText`](app/src/main/java/com/shopkeeper/ui/util/UiText.kt) (`UiText.StringResource` or `UiText.DynamicString`).

### 3. Design System & Semantic Color Invariant
- **Zero Static Alert Colors in UI:** Composable screens must **never** directly import raw alert colors (`RedAlert`, `RedLight`, `RedAlertDark`, `RedContainerDark`, `GreenSuccess`, `GreenLight`) or define inline `Color(0x...)`.
- **Semantic Mapping:** All colors must resolve from `MaterialTheme.colorScheme` (e.g., `error`, `errorContainer`, `onErrorContainer`, `primary`, `surface`).
- **Standardized Shapes:** Use canonical `MaterialTheme.shapes` (`ShopShapes` token hierarchy in `Shape.kt`).

### 4. Canonical Reusable Component Invariant
- **Zero Ad-Hoc Modals:** Do not write custom `AlertDialog`, `BasicAlertDialog`, or bespoke card dialogs. All dialogs must use [`ShopDialog`](app/src/main/java/com/shopkeeper/ui/screens/components/ShopDialog.kt) or [`ShopConfirmationDialog`](app/src/main/java/com/shopkeeper/ui/screens/components/ShopDialog.kt).
- **Canonical Search:** All search inputs must use [`SearchBarComponent`](app/src/main/java/com/shopkeeper/ui/screens/components/SearchBarComponent.kt).
- **Canonical Empty States:** All empty lists, search no-results, or zero-state containers must use [`ShopEmptyState`](app/src/main/java/com/shopkeeper/ui/screens/components/ShopEmptyState.kt).
- **Standardized Input Fields:** Use [`ShopTextField`](app/src/main/java/com/shopkeeper/ui/screens/components/ShopTextField.kt) and [`QuantityUnitRow`](app/src/main/java/com/shopkeeper/ui/screens/components/QuantityUnitRow.kt).

### 5. Offline-First Single Source of Truth (SSOT)
- The local Room database is the **only** data source observed by the presentation layer.
- The UI and ViewModels **MUST NEVER** query Cloud Firestore directly.
- Writes are committed immediately to Room with `SyncStatus.PENDING_SYNC`, and WorkManager handles background synchronization to Cloud Firestore.

### 6. WCAG 2.5.5 Touch Target Hygiene
- Every interactive element (icon buttons, action chips, checkboxes) must maintain a minimum touch target size of **$\ge 40\text{dp}$**, with **$48\text{dp}$** standard for all primary action buttons.
- Preserve minimum 4dp spacing between adjacent touch targets.

### 7. Terminology Standardization
Always use standardized pricing terminology across UI, code comments, and documentation:
- **"Buying Price"** (the acquisition/cost price paid by the shop).
- **"Selling Price"** (the retail price charged to customers).
- Do **not** revert to "Purchase Price" or "Sales Price".

### 8. Fluid Motion & Progressive Disclosure Invariant
- **List Dynamics:** All `LazyColumn` and `LazyRow` items must declare `Modifier.animateItemPlacement()`.
- **Accordion & Progressive Disclosure:** Collapsible sections, spec codes, and error alert banners must use `AnimatedVisibility(enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut())`.

---

## 📚 Canonical Components Reference Catalog

| Component | Source File | Mandatory Usage Scenario |
| :--- | :--- | :--- |
| `ShopDialog` | `ui/screens/components/ShopDialog.kt` | All custom forms, selections, and content dialogs |
| `ShopConfirmationDialog` | `ui/screens/components/ShopDialog.kt` | All destructive or single/dual-action confirmation prompts |
| `ShopEmptyState` | `ui/screens/components/ShopEmptyState.kt` | All empty lists, search no-results, and initial zero-state screens |
| `SearchBarComponent` | `ui/screens/components/SearchBarComponent.kt` | All search inputs across screens, sheets, and dialogs |
| `ShopTextField` | `ui/screens/components/ShopTextField.kt` | Standard validated text input fields with semantic error states |
| `QuantityUnitRow` | `ui/screens/components/QuantityUnitRow.kt` | All quantity increment/decrement steppers and unit selectors |
| `SyncStatusIndicator` | `ui/screens/components/SyncStatusIndicator.kt` | Top-bar and settings cloud sync status badge |

---

## 📋 Mandatory Agent Pre-Flight Architecture Checklist

Every agent formulating a plan or submitting code must explicitly verify:
- [ ] **1. Monetary Precision:** Are all money fields 64-bit `Long` minor units? (Zero `Float`/`Double`).
- [ ] **2. Localization & UiText:** Are all user-facing strings in `strings.xml` and emitted from ViewModels via `UiText`?
- [ ] **3. Semantic Tokens:** Are all colors derived from `MaterialTheme.colorScheme` and shapes from `MaterialTheme.shapes`?
- [ ] **4. Canonical Components:** Does the change reuse `ShopDialog`, `ShopEmptyState`, `SearchBarComponent`, or `ShopTextField` instead of custom wheels?
- [ ] **5. Touch Targets:** Are all interactive touch targets $\ge 40\text{dp}$ / $48\text{dp}$?
- [ ] **6. Machine Guardrails:** Do `./gradlew testProdDebugUnitTest` and `./gradlew lintProdDebug` pass with 0 errors/warnings?
- [ ] **7. Visual Regression:** Does `./gradlew verifyRoborazziProdDebug` pass with 100% golden pixel match?

---

## 🛡️ Machine-Enforced Guardrails

All non-negotiable invariants are machine-enforced by [`ArchitectureInvariantsTest`](app/src/test/java/com/shopkeeper/architecture/ArchitectureInvariantsTest.kt):
- `room entities must never use Double or Float for database persistence`
- `monetary fields in data models must be Long minor units`
- `format utilities must never accept floating-point types`
- `inventory viewmodels must observe Room SSOT via Repositories and never reference Firestore directly`
- `viewmodels must never expose raw String flows for user-facing feedback - must use UiText`
- `presentation layer must never import static alert color tokens directly`
- `presentation layer must never import raw Material AlertDialog directly`

---

## ⚡ Fast CLI Commands & Quality Gates

When verifying your work, **use these exact targeted commands** (avoid running generic slow tasks like `./gradlew test`):

```bash
# 1. Run full unit test suite (88+ tests, ~1-2 seconds)
./gradlew testProdDebugUnitTest

# 2. Verify visual screenshots against golden reference images (Roborazzi)
./gradlew verifyRoborazziProdDebug

# 3. Record/update visual screenshot golden reference images
./gradlew recordRoborazziProdDebug

# 4. Run static analysis & Android Lint
./gradlew lintProdDebug

# 5. Assemble production release APK
./gradlew assembleProdRelease

# 6. Inspect project modules and variant paths via Android CLI
android describe

# 7. Build, install, and launch on connected device/emulator
android run --variant=devDebug
```

---

## 🌿 Branching & CI/CD Workflow Model

- **Trunk-Based Development:** All features and fixes branch from `main` using short-lived branches (`feat/<name>`, `fix/<name>`, `chore/<name>`).
- **Pull Requests & Automated CI:** Changes merge to `main` via Pull Requests. The `.github/workflows/ci.yml` pipeline enforces 100% test pass rate, 0 lint errors, and successful R8 release compilation before merging.
- **Local Git Hooks:** Configured via `.githooks/pre-push` (`./scripts/setup-git-hooks.sh`) to block broken pushes locally.
- **Tagged Releases:** Production releases are cut by pushing a Git tag (`vX.Y.Z`), triggering `.github/workflows/release.yml` to build release APK/AAB and publish a GitHub Release.
- Detailed runbook: [`docs/CI_CD_WORKFLOW.md`](docs/CI_CD_WORKFLOW.md).

---

## 📁 File Placement & Package Conventions

- **Presentation:**
  - Screens: `app/src/main/java/com/shopkeeper/ui/screens/`
  - Reusable UI Components: `app/src/main/java/com/shopkeeper/ui/screens/components/`
  - ViewModels: `app/src/main/java/com/shopkeeper/ui/viewmodel/`
  - Navigation: `app/src/main/java/com/shopkeeper/ui/navigation/Screen.kt`
  - Theme & Design Tokens: `app/src/main/java/com/shopkeeper/ui/theme/`
- **Data & Domain:**
  - Room Entities & DAO: `app/src/main/java/com/shopkeeper/data/local/`
  - Remote Services: `app/src/main/java/com/shopkeeper/data/remote/`
  - Repositories: `app/src/main/java/com/shopkeeper/data/repository/`
  - Sync & WorkManager: `app/src/main/java/com/shopkeeper/data/sync/`
  - Export (CSV): `app/src/main/java/com/shopkeeper/data/export/`
- **Dependency Injection:**
  - Hilt Modules: `app/src/main/java/com/shopkeeper/di/`
- **Unit Tests:**
  - Test suites: `app/src/test/java/com/shopkeeper/`

---

## 💡 Common Pitfalls to Avoid

- **Do NOT add non-ASCII currency symbols to CSV headers**: Mobile spreadsheet applications (Google Sheets, Excel) will corrupt them (Mojibake). Always use `getSafeCurrencyHeader()` in `CsvExporter.kt`.
- **Do NOT write a UTF-8 BOM (`0xEF, 0xBB, 0xBF`) to CSV exports**: Google Sheets Android renders BOM bytes as `ï»¿Product Code`.
- **Do NOT omit status bar insets**: Custom top bars in full-screen Composables must include `Modifier.statusBarsPadding()`.
- **Do NOT hardcode dark mode colors**: Use semantic tokens from `MaterialTheme.colorScheme` or dark-adapted container tokens from `Color.kt`.
