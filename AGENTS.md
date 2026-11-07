# 🤖 AGENTS.md — Instructions for AI Coding Agents

> **Project:** ShopKeeper — Enterprise-Grade, Offline-First Retail Inventory & Pricing Assistant for Android  
> **Target Audience:** All AI coding assistants (Antigravity, Gemini Code Assist, Claude Code, Cursor, Copilot Workspace, OpenAI Codex).

---

## 📌 Project Architecture & Tech Stack

- **Architecture:** Clean Architecture + MVI / MVVM with Unidirectional Data Flow (UDF).
- **Presentation:** Jetpack Compose (Material 3), Navigation Compose 2.7.7, Edge-to-Edge display (Android 15 ready).
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

## ⛔ Non-Negotiable Invariants

Any agent making changes to this repository **MUST** adhere to the following rules:

### 1. Monetary Precision Invariant
- **Strict Prohibition:** `Double` and `Float` **MUST NEVER** be used for monetary calculations or storage in data models/entities.
- **Minor Units (`Long`):** All purchase/buying and sales/selling prices must be represented as 64-bit integers (`Long`) in minor currency units (cents, poisha, pennies):
  ```kotlin
  val purchasePriceMinor: Long,
  val salesPriceMinor: Long
  ```
- **Formatting:** Convert to formatted currency strings strictly at presentation boundaries using [`FormatUtils`](app/src/main/java/com/shopkeeper/ui/util/FormatUtils.kt).

### 2. Localization Invariant
- **Strict Prohibition:** Hardcoded strings in `@Composable` functions or dialogs are **STRICTLY FORBIDDEN**.
- **Declaration:** Every user-facing text, error message, label, and title must be declared in [`res/values/strings.xml`](app/src/main/res/values/strings.xml) and accessed via `stringResource(R.string.key)`.

### 3. Terminology Standardization
Always use the standardized pricing terminology across UI, code comments, and documentation:
- **"Buying Price"** (the acquisition/cost price paid by the shop).
- **"Selling Price"** (the retail price charged to customers).
- Do **not** revert to "Purchase Price" or "Sales Price".

### 4. Offline-First Single Source of Truth (SSOT)
- The local Room database is the **only** data source observed by the presentation layer.
- The UI **MUST NEVER** query Cloud Firestore directly.
- Writes are committed immediately to Room with `SyncStatus.PENDING_SYNC`, and WorkManager handles background synchronization to Cloud Firestore.

---

## ⚡ Fast CLI Commands & Quality Gates

When verifying your work, **use these exact targeted commands** (avoid running generic slow tasks like `./gradlew test`):

```bash
# 1. Run full unit test suite (25 tests, ~1-2 seconds)
./gradlew testProdDebugUnitTest

# 2. Run static analysis & Android Lint
./gradlew lintProdDebug

# 3. Assemble production release APK
./gradlew assembleProdRelease

# 4. Inspect project modules and variant paths via Android CLI
android describe

# 5. Build, install, and launch on connected device/emulator
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
