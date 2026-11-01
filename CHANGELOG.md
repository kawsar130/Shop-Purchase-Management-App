# 📝 Changelog

All notable changes to the **ShopKeeper** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-11-01

### Summary
Initial production-ready release of **ShopKeeper**, transforming the application from an MVP prototype into an enterprise-ready, robust, and secure offline-first retail management application for Android.

---

### Added

#### Sprint 0: Tooling Baseline & Test Safety Net
- **Gradle 8.6 Wrapper**: Checked in standardized Gradle wrapper distribution (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) with executable permissions.
- **Testing Infrastructure**: Integrated modern testing dependencies:
  - `io.mockk:mockk:1.13.10` for idiomatic Kotlin mocking.
  - `org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0` for coroutine dispatchers and test scopes.
  - `app.cash.turbine:turbine:1.1.0` for reactive `Flow` and `StateFlow` testing.
  - `androidx.arch.core:core-testing:2.2.0` for architecture component rules.
- **Unit Test Baseline**: Established automated unit tests across business logic:
  - `ProductCalculationTest` for profit, margin, and markup calculations across edge cases.
  - `FormatUtilsTest` for currency formatting and input sanitization.
  - `ProductFilterLogicTest` for search queries and category filtering.
- **CI/CD Pipeline**: Configured GitHub Actions workflow (`.github/workflows/build-apk.yml`) to automatically execute unit tests, run Android Lint, build release APKs and App Bundles (AAB), and archive artifacts.

#### Sprint 1: Security & Data Integrity Foundation
- **Room Safe Migrations**:
  - Configured Room schema export via KSP (`app/schemas/com.shopkeeper.data.local.AppDatabase/`).
  - Implemented safe migration `MIGRATION_2_3` and removed destructive fallback migrations.
  - Added automated `RoomMigrationTest` to verify zero data loss across database upgrades.
- **Monetary Precision Engine**:
  - Migrated floating-point prices (`Double`) to 64-bit integer minor units (`Long` cents) in `Product.kt` and `ProductEntity.kt`.
  - Added `centsToCurrency()` and `currencyToCents()` in `FormatUtils.kt` to eliminate IEEE 754 precision errors.
- **Firestore Batch Write Chunking**:
  - Implemented chunking in `FirestoreService.kt` to cap write batches at 400 operations, protecting against Firestore's 500-operation ceiling.
- **Cloud Firestore Security Rules**:
  - Hardened Firestore security rules with granular read/write access control, owner-based multi-tenant isolation, and staff verification.

#### Sprint 2: Architecture & Reliability Modernization
- **Dagger Hilt Dependency Injection**:
  - Integrated Hilt 2.51.1 across the application lifecycle (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`).
  - Created modular DI containers: `DatabaseModule`, `NetworkModule`, `RepositoryModule`, and `SyncModule`.
- **WorkManager Offline-First Sync Engine**:
  - Replaced ad-hoc coroutine syncing with Android Jetpack `WorkManager` (`SyncWorker`, `WorkManagerSyncScheduler`).
  - Enforced network connectivity constraints, exponential backoff retries, and unique work policies.
- **Android Credential Manager API**:
  - Integrated modern `androidx.credentials:credentials:1.3.0` and `com.google.android.libraries.identity.googleid:googleid:1.1.1` for one-tap Google authentication.
  - Maintained fallback support for legacy Google Play Services Auth.
- **Clean Repository Decoupling**:
  - Refactored `ProductRepository` and `UserSessionRepository` to remove Android `Context` dependencies and accept injected singletons.

#### Sprint 3: Release Engineering & Observability
- **Build Flavors**:
  - Added `dev` flavor (`com.shopkeeper.dev`, app title "ShopKeeper Dev", debug tooling enabled).
  - Added `prod` flavor (`com.shopkeeper`, app title "ShopKeeper", production configuration).
- **R8 Optimization & Resource Shrinking**:
  - Configured `isMinifyEnabled = true` and `isShrinkResources = true` for release builds.
  - Authored comprehensive `proguard-rules.pro` keeping Room schemas, Firestore models, and Hilt entry points while stripping debug logs.
- **Release Signing Architecture**:
  - Created `keystore.properties.example` for secure out-of-tree signing credential storage.
  - Implemented safe fallback to debug keystore for unconfigured local developer and CI builds.
- **Observability**:
  - Integrated Firebase Crashlytics SDK for release error tracking.
  - Integrated `Square LeakCanary 2.14` in `debugImplementation` for automated memory leak detection.

#### Sprint 4: UI/UX Quality, System Standards & Localization
- **Android 15 Edge-to-Edge**:
  - Configured window insets handling with `Modifier.statusBarsPadding()` and `Modifier.navigationBarsPadding()`.
  - Configured night-specific window background in `values-night/themes.xml` (`#0F172A`) to prevent white navigation flashes.
- **Material 3 Dynamic Themes**:
  - Implemented full Light and Dark color schemes in `Theme.kt` with dark-adapted badge containers (`GreenContainerDark`, `AmberContainerDark`, `RedContainerDark`).
  - Introduced `GreenSuccessDark` (`#4ADE80`) mapped to `MaterialTheme.colorScheme.secondary` for high-contrast dark card readability.
- **String Localization**:
  - Migrated 100% of user-facing UI strings to `res/values/strings.xml`.
- **Type-Safe Compose Navigation**:
  - Replaced raw string routes with sealed `Screen` class navigation (`Auth`, `Setup`, `ProductList`, `Category`, `Settings`, `StaffManagement`).

---

### Changed
- **Terminology Standardization**:
  - Standardized terminology across the entire application to **"Buying Price"** (cost price) and **"Selling Price"** (retail price), eliminating legacy inconsistencies ("Purchase Price" and "Sales Price").
  - Updated all screens, dialogs, cards, category summaries, and export headers to reflect this standard.
- **CSV Export Architecture**:
  - Mapped currency symbols to clean ASCII/ISO currency headers (e.g., `$` -> `USD`, `৳` -> `BDT`, `€` -> `EUR`) in `CsvExporter.kt`.
  - Removed UTF-8 Byte Order Mark (BOM) bytes (`0xEF, 0xBB, 0xBF`) to ensure clean rendering in mobile spreadsheet applications (Google Sheets, Microsoft Excel).

---

### Fixed
- **Status Bar Overlap**: Fixed top content clipping beneath the system status bar in `ProductListScreen.kt`.
- **Category Auto-Scroll**: Added `rememberLazyListState()` and reactive scroll positioning in `CategoryChips.kt` to ensure selected categories remain visible.
- **Theme Transition Flash**: Eliminated white window surface flicker during Dark Theme screen transitions by wiring Activity window backgrounds and root `Surface` components.
- **CSV Mojibake**: Resolved Latin-1 character corruption (`à§³`) in mobile spreadsheet viewers when opening exported inventory CSVs.
- **Cell A1 Header Corruption**: Resolved `ï»¿Product Code` display issue in Google Sheets Android.

---

### Security
- Hardened Firestore Security Rules with role-based checks for shop owners and authorized staff members.
- Enforced ProGuard/R8 obfuscation on release builds to protect business logic.
- Keystore credentials, secrets, and private keys strictly isolated from source control.
