# ShopKeeper 📱🛒

[![CI Quality Gate](https://github.com/kawsar130/Shop-Purchase-Management-App/actions/workflows/ci.yml/badge.svg)](https://github.com/kawsar130/Shop-Purchase-Management-App/actions/workflows/ci.yml)
[![CodeQL Security](https://github.com/kawsar130/Shop-Purchase-Management-App/actions/workflows/codeql.yml/badge.svg)](https://github.com/kawsar130/Shop-Purchase-Management-App/actions/workflows/codeql.yml)
[![GitHub Release](https://img.shields.io/github/v/release/kawsar130/Shop-Purchase-Management-App?logo=github&color=blue)](https://github.com/kawsar130/Shop-Purchase-Management-App/releases/latest)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android_SDK-24_.._34-3DDC84.svg?logo=android&logoColor=white)](https://developer.android.com)
[![Compose BOM](https://img.shields.io/badge/Compose_BOM-2024.04.01-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Hilt](https://img.shields.io/badge/Dagger_Hilt-2.51.1-brightgreen.svg)](https://dagger.dev/hilt/)
[![Room](https://img.shields.io/badge/Room-2.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Unit Tests](https://img.shields.io/badge/Tests-28%20passing-brightgreen.svg)](app/src/test/)

An enterprise-ready, offline-first Android application designed for retail shop owners and staff to instantly look up **Buying Prices** and **Selling Prices**, check real-time profit margins, manage inventory, and seamlessly synchronize product data across multiple authorized Google accounts using **Cloud Firestore** and **Android WorkManager**.

---

## 🌟 Key Highlights

- **⚡ Instant Customer-Facing Lookups (<10ms)**:
  Uses an indexed **Room (SQLite)** local cache as the Single Source of Truth. Staff can instantly search by product code or name without network latency.
- **📊 Transparent Buying & Selling Pricing**:
  Displays clear, distinct **Selling Price** (retail, customer-facing) and **Buying Price** (cost price for staff). Automatically computes live unit profit, margin percentage, and markup percentage.
- **🔄 Guaranteed Background Synchronization**:
  Powered by Android Jetpack **WorkManager** with exponential backoff and network constraints. Changes made offline are safely stored locally and synchronized with Cloud Firestore as soon as connectivity is restored.
- **👥 Multi-User Staff Access (Zero Setup)**:
  Shop owners can invite staff Gmail addresses directly within the app (**Settings** > **Manage Staff Members**). Invited staff members log in on their devices and automatically link to the shop's real-time inventory.
- **📑 Clean CSV Export (Excel & Google Sheets Compatible)**:
  One-tap inventory export to CSV with standard ASCII/ISO currency headers and clean UTF-8 encoding (BOM-free), preventing Mojibake character corruption in mobile spreadsheet applications.
- **🎨 Android 15 Edge-to-Edge & Material 3 Dark/Light Themes**:
  Built with Jetpack Compose Material 3 supporting dynamic Light and Dark color schemes, high-contrast badges for stock alerts, and edge-to-edge system insets.
- **🔒 Strict Monetary Precision**:
  All financial amounts are stored as 64-bit integer minor units (`Long` cents), eliminating IEEE 754 floating-point rounding errors in stock valuations and margin calculations.
- **☁️ Zero Infrastructure Cost**:
  Runs directly on the Google Firebase Spark (free) tier with chunked write batches ($\le 400$ items) to ensure safety under free-tier quotas.

---

## 🛠️ Modern Tech Stack

| Layer | Technologies | Purpose |
| :--- | :--- | :--- |
| **Presentation** | Jetpack Compose, Material 3, Navigation Compose 2.7.7 | Declarative UI, Edge-to-Edge layout, Dark Theme |
| **Architecture** | MVI / MVVM, Unidirectional Data Flow, StateFlow | Reactive, lifecycle-aware state management |
| **Dependency Injection** | Dagger Hilt 2.51.1 | Decoupled, testable dependency graph |
| **Local Persistence** | Room 2.6.1, SQLite, Preferences DataStore | Offline Single Source of Truth, safe migrations (`MIGRATION_2_3`) |
| **Background Sync** | WorkManager 2.9.0, Hilt Work | Reliable offline-first sync with network constraints |
| **Cloud & Auth** | Cloud Firestore, Android Credential Manager 1.3.0, Google ID | Real-time multi-user inventory sync and Google Sign-In |
| **Observability** | Firebase Crashlytics, LeakCanary 2.14 (`dev`) | Production crash logging and debug memory leak detection |
| **Build & Tooling** | Gradle 8.6, R8 Minification, ProGuard, GitHub Actions | Automated CI testing, linting, and AAB/APK release bundling |

---

## 📁 Project Architecture

```
ShopKeeper/
├── app/
│   ├── build.gradle.kts                # Build configuration, flavors, dependencies
│   ├── proguard-rules.pro              # R8 minification & obfuscation rules
│   ├── schemas/                        # Exported Room database schema history (v1, v2, v3)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml     # App manifest, permissions, FileProvider
│       │   ├── java/com/shopkeeper/
│       │   │   ├── ShopApplication.kt  # @HiltAndroidApp application class & CrashLogger
│       │   │   ├── data/
│       │   │   │   ├── export/         # CsvExporter (ISO headers, UTF-8 clean export)
│       │   │   │   ├── local/          # Room AppDatabase, ProductDao, ProductEntity
│       │   │   │   ├── model/          # Product (Long cents), ShopInfo, SyncStatus
│       │   │   │   ├── remote/         # FirestoreService (chunked batch writes <= 400 ops)
│       │   │   │   ├── repository/     # ProductRepository & UserSessionRepository
│       │   │   │   └── sync/           # WorkManager SyncWorker & WorkManagerSyncScheduler
│       │   │   ├── di/                 # Hilt Modules (Database, Network, Repository, Sync)
│       │   │   ├── ui/
│       │   │   │   ├── MainActivity.kt # Navigation Host & Edge-to-Edge setup
│       │   │   │   ├── navigation/     # Screen sealed class (type-safe routes)
│       │   │   │   ├── screens/        # Auth, Setup, Products, Categories, Settings, Staff
│       │   │   │   ├── theme/          # Material 3 Color palettes, Theme, Typography
│       │   │   │   ├── util/           # FormatUtils (minor units to currency formatting)
│       │   │   │   └── viewmodel/      # ProductViewModel, AuthViewModel, SettingsViewModel
│       │   │   └── util/               # CrashLogger, NetworkMonitor
│       │   └── res/                    # Drawables, values, values-night, XML configurations
│       └── test/java/com/shopkeeper/   # 28 Unit Tests (Architecture, MockK, Turbine, Room)
├── docs/
│   ├── ARCHITECTURE.md                 # Deep-dive system design & sync engine specification
│   ├── CI_CD_WORKFLOW.md               # Trunk-based branching runbook & automated release pipeline
│   ├── adr/                            # Architecture Decision Records (ADRs)
│   └── sprints/                        # Sprint history, acceptance criteria & PR template
├── CHANGELOG.md                        # Formal release notes following Keep a Changelog
├── CONTRIBUTING.md                     # Developer onboarding, signing setup & PR quality gate
├── CODE_OF_CONDUCT.md                  # Contributor Covenant Code of Conduct v2.1
├── SECURITY.md                         # Responsible disclosure policy & security invariants
├── ROADMAP.md                          # Production readiness roadmap & sprint progress
├── build.gradle.kts                    # Root build script & plugin declarations
└── settings.gradle.kts                 # Dependency resolution & repository settings
```

---

## 🚀 Getting Started

### 1. Prerequisites
- **JDK 17** (Eclipse Temurin or OpenJDK recommended)
- **Android Studio** Iguana (2023.2.1) or newer
- Android device or emulator running **Android 7.0 (API 24)** or higher

### 2. Build Flavors

ShopKeeper defines two build flavors:

- **`dev`** (`applicationId: com.shopkeeper.dev`):
  - App Name: **ShopKeeper Dev**
  - Includes **LeakCanary 2.14** for automated memory leak detection.
  - Can be installed on the same device side-by-side with the production build.
- **`prod`** (`applicationId: com.shopkeeper`):
  - App Name: **ShopKeeper**
  - Full **R8 minification**, code shrinking, and ProGuard obfuscation enabled.
  - Includes **Firebase Crashlytics** error tracking.

### 3. Signing Setup (Optional for Release)
For local release builds, copy the example signing properties:
```bash
cp keystore.properties.example keystore.properties
```
Fill in your keystore credentials. If omitted, the build safely falls back to the debug signing key.

### 4. CLI Build & Test Commands

```bash
# Run all unit tests (28 tests, ~1-2 seconds)
./gradlew testProdDebugUnitTest

# Run static analysis and Android Lint
./gradlew lintProdDebug

# Assemble Dev Debug APK (with LeakCanary)
./gradlew assembleDevDebug

# Assemble Production Release APK (R8 Minified)
./gradlew assembleProdRelease

# Build Production Google Play App Bundle (AAB)
./gradlew bundleProdRelease
```

---

## 📱 Daily Shop Operations

1. **Initial Onboarding**:
   - Sign in with your Google account via modern Credential Manager.
   - Enter your **Shop Name** and select your primary **Currency** (`$`, `৳`, `₹`, `€`, `£`, etc.).
   - Tap **"Create Shop Database & Start"**—your cloud database is provisioned instantly.
2. **Instant Price Lookup**:
   - Type a Product Code (e.g., `BT-778`) or Name in the search bar.
   - View the large customer-facing **Selling Price**, staff **Buying Price**, and current **Profit Margin**.
3. **Inventory Management**:
   - Tap `+` to add or update items.
   - Profit, margin percentage, and markup percentage compute live as you enter prices.
4. **Category Stock Valuation**:
   - Tap the Category icon to review stock valuation breakdowns (total buying investment vs. selling value) grouped by category.
5. **Staff Delegation**:
   - Open **Settings** > **Manage Staff Members**.
   - Add your staff's Gmail addresses. When they install ShopKeeper, they gain immediate read/write access to your inventory.
6. **CSV Spreadsheet Export**:
   - Open **Settings** > **Export Inventory (CSV)** to generate and share a clean spreadsheet backup via Google Drive, WhatsApp, or Gmail.

---

## 📚 Project Documentation

- 🏛️ [**Architecture & System Design**](docs/ARCHITECTURE.md) - Deep dive into the offline-first sync engine, monetary modeling, and Firestore batch chunking.
- 📋 [**Architecture Decision Records (ADRs)**](docs/adr/README.md) - Foundational technical decisions, context, and trade-offs.
- 🔁 [**CI/CD & Branching Workflow**](docs/CI_CD_WORKFLOW.md) - Trunk-based branching model, automated PR quality gates, and zero-toil tag releases.
- 🤝 [**Contributing & Developer Onboarding**](CONTRIBUTING.md) - Setup guide, coding standards, and pre-PR quality gates.
- 📜 [**Code of Conduct**](CODE_OF_CONDUCT.md) - Community standards and Contributor Covenant v2.1.
- 🛡️ [**Security Policy**](SECURITY.md) - Vulnerability disclosure protocol and core defense-in-depth invariants.
- 📝 [**Changelog & Release Notes**](CHANGELOG.md) - Complete history of features, improvements, and fixes across all sprints.
- 🗺️ [**Production Roadmap**](ROADMAP.md) - Sprints 0–4 dashboard and governance rules.

---

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
