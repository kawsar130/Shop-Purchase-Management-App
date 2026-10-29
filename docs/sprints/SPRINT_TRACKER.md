# 📋 Sprint-by-Sprint Implementation Tracker

> **Single Source of Truth:** Use this document to track granular progress, acceptance criteria, impacted files, and verification commands for every sprint. Mark tasks as `[x]` only when all acceptance criteria and quality gates are satisfied.

---

## 📌 Status Legend
- ⚪ `PENDING`: Not started
- 🟡 `IN_PROGRESS`: Actively being worked on
- 🟢 `DONE`: Completed, tested, and merged into epic branch
- 🔴 `BLOCKED`: Blocked by a dependency

---

## 🚀 Sprint 0: Tooling Baseline & Test Safety Net

| Task ID | Task Title | Status | Estimated Effort |
| :--- | :--- | :---: | :---: |
| **S0-T1** | Restore Gradle Wrapper & Checkin Verification | 🟢 DONE | 1 hr |
| **S0-T2** | Setup Testing Stack Dependencies | 🟢 DONE | 1 hr |
| **S0-T3** | Baseline Unit Tests for Core Business Logic | 🟢 DONE | 3 hrs |
| **S0-T4** | Update GitHub Actions CI Workflow | 🟢 DONE | 1 hr |

### S0-T1: Restore Gradle Wrapper & Checkin Verification
- **Impacted Files:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`
- **Subtasks:**
  - [x] Generate standard Gradle 8.6 wrapper files.
  - [x] Set executable permissions (`chmod +x gradlew`).
  - [x] Verify `./gradlew --version` executes cleanly without requiring global Gradle.
- **Acceptance Criteria:**
  - Running `./gradlew --version` outputs Gradle 8.6 and JVM 19.
  - `git status` confirms wrapper scripts are properly tracked.

### S0-T2: Setup Testing Stack Dependencies
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts)
- **Subtasks:**
  - [x] Add `testImplementation("io.mockk:mockk:1.13.10")`.
  - [x] Add `testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")`.
  - [x] Add `testImplementation("app.cash.turbine:turbine:1.1.0")` for reactive Flow testing.
  - [x] Add `testImplementation("androidx.arch.core:core-testing:2.2.0")` for InstantTaskExecutorRule.
- **Acceptance Criteria:**
  - Gradle syncs with zero dependency resolution conflicts.

### S0-T3: Baseline Unit Tests for Core Business Logic
- **Impacted Files:** [`FormatUtilsTest.kt`](../../app/src/test/java/com/shopkeeper/ui/util/FormatUtilsTest.kt), [`ProductCalculationTest.kt`](../../app/src/test/java/com/shopkeeper/data/model/ProductCalculationTest.kt), [`ProductFilterLogicTest.kt`](../../app/src/test/java/com/shopkeeper/ui/viewmodel/ProductFilterLogicTest.kt)
- **Subtasks:**
  - [x] Test existing markup calculations in `Product.kt` across edge cases (zero cost, zero price, negative values).
  - [x] Test number formatters in `FormatUtils.kt` across standard and decimal values.
  - [x] Test product search and category filtering logic from `ProductViewModel`.
- **Acceptance Criteria:**
  - Tests verify existing baseline behavior with 100% pass rate.
  - Command: `./gradlew testDebugUnitTest`.

### S0-T4: Update GitHub Actions CI Workflow
- **Impacted Files:** [`.github/workflows/build-apk.yml`](../../.github/workflows/build-apk.yml), [`app/src/main/res/values/themes.xml`](../../app/src/main/res/values/themes.xml)
- **Subtasks:**
  - [x] Remove on-the-fly `gradle wrapper` generation step.
  - [x] Add `./gradlew testDebugUnitTest --stacktrace` to CI pipeline.
  - [x] Add `./gradlew lintDebug` to CI pipeline.
  - [x] Fix Android Lint `NewApi` error on `windowLightNavigationBar` in `themes.xml`.
- **Acceptance Criteria:**
  - CI runs tests, lint, and builds debug APK with zero errors.

---

## 🔒 Sprint 1: Security & Data Integrity Foundation

| Task ID | Task Title | Status | Estimated Effort |
| :--- | :--- | :---: | :---: |
| **S1-T1** | Room Schema Export & KSP Schema Configuration | 🟢 DONE | 1 hr |
| **S1-T2** | Remove Destructive Migration & Implement Safe Migrations | 🟢 DONE | 3 hrs |
| **S1-T3** | Monetary Precision: Migrate Prices to `Long` Minor Units | 🟢 DONE | 4 hrs |
| **S1-T4** | Fix Margin vs. Markup Calculation Formula | 🟢 DONE | 1 hr |
| **S1-T5** | Cloud Firestore Security Rules Hardening | 🟢 DONE | 3 hrs |
| **S1-T6** | Firestore WriteBatch Chunking (Max 400 Operations) | 🟢 DONE | 2 hrs |

### S1-T1: Room Schema Export & KSP Schema Configuration
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), [`AppDatabase.kt`](../../app/src/main/java/com/shopkeeper/data/local/AppDatabase.kt)
- **Subtasks:**
  - [x] Set `exportSchema = true` in `@Database` annotation.
  - [x] Configure `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` in `app/build.gradle.kts`.
  - [x] Add Room schemas directory to source sets for automated migration tests.
- **Acceptance Criteria:**
  - Building project generates `app/schemas/com.shopkeeper.data.local.AppDatabase/1.json`, `2.json`, and `3.json`.

### S1-T2: Remove Destructive Migration & Implement Safe Migrations
- **Impacted Files:** [`AppDatabase.kt`](../../app/src/main/java/com/shopkeeper/data/local/AppDatabase.kt), [`RoomMigrationTest.kt`](../../app/src/test/java/com/shopkeeper/data/local/RoomMigrationTest.kt)
- **Subtasks:**
  - [x] Remove `.fallbackToDestructiveMigration()`.
  - [x] Create explicit migration script `MIGRATION_2_3`.
  - [x] Write `MigrationTest` verifying user data is preserved after schema upgrades.
- **Acceptance Criteria:**
  - App updates over existing database version without data loss.
  - Automated migration test passes with zero SQLite errors.

### S1-T3: Monetary Precision: Migrate Prices to `Long` Minor Units
- **Impacted Files:** [`Product.kt`](../../app/src/main/java/com/shopkeeper/data/model/Product.kt), [`ProductEntity.kt`](../../app/src/main/java/com/shopkeeper/data/local/ProductEntity.kt), [`ProductDao.kt`](../../app/src/main/java/com/shopkeeper/data/local/ProductDao.kt), [`FirestoreService.kt`](../../app/src/main/java/com/shopkeeper/data/remote/FirestoreService.kt), [`FormatUtils.kt`](../../app/src/main/java/com/shopkeeper/ui/util/FormatUtils.kt), [`AddEditProductDialog.kt`](../../app/src/main/java/com/shopkeeper/ui/screens/AddEditProductDialog.kt)
- **Subtasks:**
  - [x] Change `purchasePrice: Double` and `salesPrice: Double` to `purchasePriceMinor: Long` and `salesPriceMinor: Long` in domain and entity models.
  - [x] Migrate SQLite schema safely (`Double` to `INTEGER`).
  - [x] Update `AddEditProductDialog` input parsing: convert user-entered string (e.g. `12.50`) into minor unit integer (`1250L`) using `parseToMinorUnits`.
  - [x] Update `FormatUtils` to format `Long` minor units into localized currency strings with comma separators.
- **Acceptance Criteria:**
  - Stored numbers are exact integers. No binary floating-point representation artifacts.
  - All inventory valuation sums match exact ledger values.

### S1-T4: Fix Margin vs. Markup Calculation Formula
- **Impacted Files:** [`Product.kt`](../../app/src/main/java/com/shopkeeper/data/model/Product.kt), [`ProductCard.kt`](../../app/src/main/java/com/shopkeeper/ui/screens/components/ProductCard.kt), [`AddEditProductDialog.kt`](../../app/src/main/java/com/shopkeeper/ui/screens/AddEditProductDialog.kt), [`ProductCalculationTest.kt`](../../app/src/test/java/com/shopkeeper/data/model/ProductCalculationTest.kt)
- **Subtasks:**
  - [x] Add explicit `markupPercentage`: `(salesPrice - purchasePrice) / purchasePrice * 100`.
  - [x] Add explicit `profitMarginPercentage`: `(salesPrice - purchasePrice) / salesPrice * 100`.
  - [x] Clarify UI labels so shopkeepers clearly distinguish profit markup vs gross margin.
- **Acceptance Criteria:**
  - Formulas pass all unit tests with zero division-by-zero crashes.

### S1-T5: Cloud Firestore Security Rules Hardening
- **Impacted Files:** [`firestore.rules`](../../firestore.rules)
- **Subtasks:**
  - [x] Require `request.resource.data.ownerUid == request.auth.uid` on `/shops/{shopId}` create.
  - [x] Add data validation functions on `/shops/{shopId}/products/{productId}`:
    - `salesPriceMinor is int && salesPriceMinor >= 0`
    - `purchasePriceMinor is int && purchasePriceMinor >= 0`
    - `quantity is int && quantity >= 0 && quantity <= 1000000`
    - `name is string && name.size() > 0 && name.size() <= 200`
    - `code is string && code.size() > 0 && code.size() <= 100`
- **Acceptance Criteria:**
  - Unauthorized writes or invalid payload schemas are rejected with `PERMISSION_DENIED`.

### S1-T6: Firestore WriteBatch Chunking (Max 400 Operations)
- **Impacted Files:** [`FirestoreService.kt`](../../app/src/main/java/com/shopkeeper/data/remote/FirestoreService.kt)
- **Subtasks:**
  - [x] Split operations into chunks of 400 max (`operations.chunked(400)`).
  - [x] Commit each batch sequentially with proper error handling and fallback.
- **Acceptance Criteria:**
  - Syncing 1,000+ pending offline modifications does not exceed Firestore's 500-op limit.

---

## 🏛️ Sprint 2: Architecture & Reliability Modernization

| Task ID | Task Title | Status | Estimated Effort |
| :--- | :--- | :---: | :---: |
| **S2-T1** | Add Dagger Hilt Plugin & Core Dependencies | 🟢 DONE | 1 hr |
| **S2-T2** | Create Hilt Dependency Injection Modules | 🟢 DONE | 3 hrs |
| **S2-T3** | Migrate ViewModels to `@HiltViewModel` & Purge Factories | 🟢 DONE | 2 hrs |
| **S2-T4** | Decouple `ProductRepository` from Android Context | 🟢 DONE | 3 hrs |
| **S2-T5** | Refactor `CsvExporter` (Separation of Concerns) | 🟢 DONE | 2 hrs |
| **S2-T6** | Migrate Google Sign-In to Credential Manager API | 🟢 DONE | 4 hrs |
| **S2-T7** | Implement WorkManager for Guaranteed Offline Sync | 🟢 DONE | 3 hrs |
| **S2-T8** | Network Validation via `NET_CAPABILITY_VALIDATED` | 🟢 DONE | 1 hr |

### S2-T1 to S2-T3: Dagger Hilt Integration
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), [`ShopApplication.kt`](../../app/src/main/java/com/shopkeeper/ShopApplication.kt), `di/*.kt`, ViewModels, [`MainActivity.kt`](../../app/src/main/java/com/shopkeeper/ui/MainActivity.kt)
- **Subtasks:**
  - [x] Apply `com.google.dagger.hilt.android` plugin.
  - [x] Annotate `ShopApplication` with `@HiltAndroidApp` and `MainActivity` with `@AndroidEntryPoint`.
  - [x] Create `DatabaseModule`, `NetworkModule`, `RepositoryModule`, `SyncModule`.
  - [x] Inject repositories into `ProductViewModel`, `AuthViewModel`, and `SettingsViewModel`.
  - [x] Delete `ProductViewModelFactory`, `AuthViewModelFactory`, and `SettingsViewModelFactory`.
- **Acceptance Criteria:**
  - Application builds cleanly with `@Inject` constructor dependencies.
  - Zero manual factories remaining.

### S2-T4: Decouple `ProductRepository` from Android Context
- **Impacted Files:** [`ProductRepository.kt`](../../app/src/main/java/com/shopkeeper/data/repository/ProductRepository.kt)
- **Subtasks:**
  - [x] Remove `Context` parameter from `ProductRepository` constructor.
  - [x] Abstract sync scheduling behind `SyncScheduler` interface.
  - [x] Decouple file export to accept `cacheDir: File`.
- **Acceptance Criteria:**
  - Repository contains only plain Kotlin / Coroutine / Room / Firestore code with no Android UI framework dependencies.

### S2-T5: Refactor `CsvExporter`
- **Impacted Files:** [`CsvExporter.kt`](../../app/src/main/java/com/shopkeeper/data/export/CsvExporter.kt), [`SettingsScreen.kt`](../../app/src/main/java/com/shopkeeper/ui/screens/SettingsScreen.kt)
- **Subtasks:**
  - [x] Refactor `CsvExporter` to only create and return `File`.
  - [x] Remove `Intent.FLAG_ACTIVITY_NEW_TASK` and `context.startActivity()` from the data layer.
  - [x] Move share intent launcher into `SettingsScreen` / pure helper `createShareIntent`.
- **Acceptance Criteria:**
  - Clean separation: data layer generates file; presentation layer launches system chooser.

### S2-T6: Credential Manager API Migration
- **Impacted Files:** [`AuthViewModel.kt`](../../app/src/main/java/com/shopkeeper/ui/viewmodel/AuthViewModel.kt), [`AuthScreen.kt`](../../app/src/main/java/com/shopkeeper/ui/screens/AuthScreen.kt), [`app/build.gradle.kts`](../../app/build.gradle.kts)
- **Subtasks:**
  - [x] Replace `play-services-auth` with `androidx.credentials:credentials` and `googleid`.
  - [x] Implement `GetCredentialRequest` with Google ID Option (`GetGoogleIdOption`).
  - [x] Handle bottom-sheet authentication seamlessly.
- **Acceptance Criteria:**
  - Users sign in with standard modern bottom sheet on Android 14/15 and older devices.

### S2-T7: WorkManager Guaranteed Offline Sync
- **Impacted Files:** `com/shopkeeper/data/sync/SyncWorker.kt`, [`ProductRepository.kt`](../../app/src/main/java/com/shopkeeper/data/repository/ProductRepository.kt), [`ShopApplication.kt`](../../app/src/main/java/com/shopkeeper/ShopApplication.kt)
- **Subtasks:**
  - [x] Create `SyncWorker: CoroutineWorker` annotated with `@HiltWorker`.
  - [x] Trigger `OneTimeWorkRequestBuilder` with `Constraints(NetworkType.CONNECTED)` whenever local changes are saved offline.
  - [x] Configure custom `HiltWorkerFactory` in `ShopApplication` and disable default initializer.
- **Acceptance Criteria:**
  - Offline changes sync automatically in background even after closing or swiping away the app.

### S2-T8: Network Validation via `NET_CAPABILITY_VALIDATED`
- **Impacted Files:** [`NetworkMonitor.kt`](../../app/src/main/java/com/shopkeeper/util/NetworkMonitor.kt)
- **Subtasks:**
  - [x] Update network capabilities check to verify both `NET_CAPABILITY_INTERNET` and `NET_CAPABILITY_VALIDATED`.
- **Acceptance Criteria:**
  - Captive portals with no true internet connectivity do not trigger false-positive sync attempts.

---

## 📦 Sprint 3: Release Engineering & Observability

| Task ID | Task Title | Status | Estimated Effort |
| :--- | :--- | :---: | :---: |
| **S3-T1** | Configure Build Flavors (`dev` vs `prod`) | ⚪ PENDING | 2 hrs |
| **S3-T2** | Enable R8 / Proguard Minification & Resource Shrinking | ⚪ PENDING | 2 hrs |
| **S3-T3** | Audit & Harden `proguard-rules.pro` | ⚪ PENDING | 2 hrs |
| **S3-T4** | Secure Keystore & Release Signing Configuration | ⚪ PENDING | 2 hrs |
| **S3-T5** | Integrate Firebase Crashlytics & Performance SDK | ⚪ PENDING | 2 hrs |
| **S3-T6** | Integrate LeakCanary for Debug Builds | ⚪ PENDING | 1 hr |
| **S3-T7** | Automated CI Workflow for Release AAB Bundle | ⚪ PENDING | 2 hrs |

### S3-T1: Build Flavors (`dev` vs `prod`)
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), `app/src/dev/`, `app/src/prod/`
- **Subtasks:**
  - [ ] Add `flavorDimensions += "environment"`.
  - [ ] Add `dev` flavor with `applicationIdSuffix = ".dev"`.
  - [ ] Add `prod` flavor with standard package name.
  - [ ] Move respective `google-services.json` to `app/src/dev/` and `app/src/prod/`.
- **Acceptance Criteria:**
  - Dev and Prod builds can be installed side-by-side on the same device without conflicting.

### S3-T2 & S3-T3: R8 Minification & Proguard Optimization
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), [`proguard-rules.pro`](../../app/proguard-rules.pro)
- **Subtasks:**
  - [ ] Set `isMinifyEnabled = true` and `isShrinkResources = true` in `release` build type.
  - [ ] Add keep rules for Room entities, Firestore data models, and Coroutines.
  - [ ] Verify release build starts cleanly without reflection crashes.
- **Acceptance Criteria:**
  - Release APK size reduced by > 30%.
  - Zero crashes on release build smoke test.

### S3-T4: Secure Keystore & Signing Configuration
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), `.gitignore`
- **Subtasks:**
  - [ ] Add `keystore/` and `*.keystore`, `*.jks`, `keystore.properties` to `.gitignore`.
  - [ ] Read release signing credentials from environment variables / secret file.
  - [ ] Remove hardcoded debug passwords from `build.gradle.kts`.
- **Acceptance Criteria:**
  - No secret keys or passwords committed to Git.

### S3-T5 & S3-T6: Crashlytics, Perf & LeakCanary
- **Impacted Files:** [`app/build.gradle.kts`](../../app/build.gradle.kts), [`ShopApplication.kt`](../../app/src/main/java/com/shopkeeper/ShopApplication.kt)
- **Subtasks:**
  - [ ] Add `com.google.firebase:firebase-crashlytics-ktx`.
  - [ ] Add `com.google.firebase:firebase-perf-ktx`.
  - [ ] Add `debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")`.
- **Acceptance Criteria:**
  - Test crash report uploads to Firebase console.
  - LeakCanary monitors memory leaks in debug mode.

---

## 🎨 Sprint 4: UI/UX Quality, System Standards & Localization

| Task ID | Task Title | Status | Estimated Effort |
| :--- | :--- | :---: | :---: |
| **S4-T1** | Extract All UI Copy to `strings.xml` | ⚪ PENDING | 3 hrs |
| **S4-T2** | Implement Android 15 Edge-to-Edge (`enableEdgeToEdge`) | ⚪ PENDING | 2 hrs |
| **S4-T3** | Fix Dark Theme Support & System Bar Insets | ⚪ PENDING | 2 hrs |
| **S4-T4** | Type-Safe Navigation Compose (2.8+ Serializable Routes) | ⚪ PENDING | 3 hrs |
| **S4-T5** | Structured UI Event & Snackbar Architecture | ⚪ PENDING | 2 hrs |
| **S4-T6** | Optimize Flow Collection (`WhileSubscribed(5_000)`) | ⚪ PENDING | 1 hr |

### S4-T1: String Resource Extraction
- **Impacted Files:** [`strings.xml`](../../app/src/main/res/values/strings.xml), all screen composables
- **Subtasks:**
  - [ ] Extract all dialog titles, buttons, error messages, and descriptions into `strings.xml`.
  - [ ] Replace hardcoded strings with `stringResource()`.
- **Acceptance Criteria:**
  - Zero hardcoded user-visible text in UI files.
  - Ready for multi-language localization.

### S4-T2 & S4-T3: Edge-to-Edge & Dark Theme
- **Impacted Files:** [`MainActivity.kt`](../../app/src/main/java/com/shopkeeper/ui/MainActivity.kt), [`Theme.kt`](../../app/src/main/java/com/shopkeeper/ui/theme/Theme.kt), [`themes.xml`](../../app/src/main/res/values/themes.xml)
- **Subtasks:**
  - [ ] Call `enableEdgeToEdge()` in `MainActivity.onCreate()`.
  - [ ] Delete deprecated `window.statusBarColor = ...` assignments.
  - [ ] Use `isSystemInDarkTheme()` in `ShopKeeperTheme`.
  - [ ] Apply proper window insets padding (`Modifier.systemBarsPadding()`, `safeDrawingPadding()`).
- **Acceptance Criteria:**
  - No white flashes when switching system themes.
  - Content draws behind system bars with proper scrims on Android 15+.

### S4-T4: Type-Safe Navigation Compose
- **Impacted Files:** [`MainActivity.kt`](../../app/src/main/java/com/shopkeeper/ui/MainActivity.kt)
- **Subtasks:**
  - [ ] Upgrade Navigation Compose to 2.8+.
  - [ ] Define routes as `@Serializable` Kotlin objects/classes:
    - `@Serializable object AuthRoute`
    - `@Serializable object SetupRoute`
    - `@Serializable object ProductsRoute`
    - `@Serializable object CategoriesRoute`
    - `@Serializable object SettingsRoute`
    - `@Serializable object StaffRoute`
  - [ ] Replace string literal navigation calls with type-safe route instances.
- **Acceptance Criteria:**
  - Navigation compile-time checked. No typos in route strings.

### S4-T5: Structured UI Events
- **Impacted Files:** ViewModels, [`MainActivity.kt`](../../app/src/main/java/com/shopkeeper/ui/MainActivity.kt), `ShopSnackbarHost`
- **Subtasks:**
  - [ ] Create `sealed interface UiMessage { data class Success(val text: String); data class Error(val text: String) }`.
  - [ ] Remove fragile substring sniffing (`msg.contains("error")`).
- **Acceptance Criteria:**
  - Snackbar styling is driven strictly by typed message models.

---

## 🎯 Verification Matrix (Final Sign-off Checklist)

Before promoting to Client Staging / Production:

- [ ] **Build:** `./gradlew clean assembleProdRelease bundleProdRelease` finishes with Exit Code 0.
- [ ] **Tests:** `./gradlew testProdReleaseUnitTest` 100% passing.
- [ ] **Lint:** `./gradlew lintProdRelease` reports zero fatal issues.
- [ ] **Database:** In-memory Room tests and migration tests pass without data corruption.
- [ ] **Security:** `firestore.rules` passes Firebase test emulator suite.
- [ ] **Obfuscation:** Release build mapping file generated; APK launches without R8 crashes.
- [ ] **Memory:** LeakCanary reports zero retained references during navigation and dialog flows.
- [ ] **Offline:** Data added offline automatically syncs via WorkManager upon reconnecting.
