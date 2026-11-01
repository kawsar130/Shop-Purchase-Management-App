# 🤝 Contributing to ShopKeeper

Thank you for your interest in contributing to **ShopKeeper**! This guide provides detailed onboarding instructions, development standards, and contribution workflows to help you get started quickly.

---

## 📋 Table of Contents
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Environment Configuration](#-environment-configuration)
  - [1. Keystore Setup](#1-keystore-setup)
  - [2. Firebase Setup](#2-firebase-setup)
- [Build Variants & Flavors](#-build-variants--flavors)
- [Architectural & Coding Standards](#-architectural--coding-standards)
  - [1. Monetary Precision Invariant](#1-monetary-precision-invariant)
  - [2. Localization Invariant](#2-localization-invariant)
  - [3. Presentation Layer & State Management](#3-presentation-layer--state-management)
  - [4. Standardized Terminology](#4-standardized-terminology)
- [Pre-PR Quality Gate](#-pre-pr-quality-gate)
- [Git Commit Guidelines](#-git-commit-guidelines)

---

## 🛠 Prerequisites

Ensure your development environment meets the following specifications:

| Requirement | Recommended Version | Notes |
| :--- | :--- | :--- |
| **JDK** | JDK 17 (Eclipse Temurin or OpenJDK) | Required for Gradle 8.6 and Kotlin 1.9.23 |
| **Android Studio** | Iguana (2023.2.1) or newer | Recommended: Jellyfish / Koala |
| **Android SDK** | SDK 34 (Compile & Target) | Fully tested and compatible with Android 15 (API 35) |
| **Minimum SDK** | API 24 (Android 7.0 Nougat) | |

---

## 🚀 Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kawsar130/Shop-Purchase-Management-App.git
   cd Shop-Purchase-Management-App
   ```

2. **Open in Android Studio**:
   - Launch Android Studio, select **Open**, and navigate to the cloned directory.
   - Let Gradle sync completely.

3. **Verify Gradle Wrapper**:
   ```bash
   ./gradlew --version
   ```

---

## ⚙️ Environment Configuration

### 1. Keystore Setup

The project uses an external properties file (`keystore.properties`) to supply release signing keys without checking credentials into version control.

1. Copy the example configuration template:
   ```bash
   cp keystore.properties.example keystore.properties
   ```
2. For local debug builds, `keystore.properties` is **optional**—the build automatically falls back to `keystore/debug.keystore`.
3. For release signing, populate `keystore.properties` with your signing configuration:
   ```properties
   storeFile=release.keystore
   storePassword=your_store_password
   keyAlias=your_key_alias
   keyPassword=your_key_password
   ```

> [!CAUTION]
> Never commit `keystore.properties` or any `.keystore`/`.jks` file to Git. Both are ignored in `.gitignore`.

### 2. Firebase Setup

The application communicates with **Firebase Authentication** and **Cloud Firestore**.

1. Place your `google-services.json` file in the `app/` directory:
   ```
   app/google-services.json
   ```
2. Ensure Cloud Firestore and Google Sign-In are enabled in your Firebase console.

---

## 🎨 Build Variants & Flavors

ShopKeeper defines two environment flavors:

| Flavor | Package ID | App Name | Features / Diagnostics |
| :--- | :--- | :--- | :--- |
| **`dev`** | `com.shopkeeper.dev` | ShopKeeper Dev | LeakCanary 2.14 enabled, debug logging, side-by-side installation |
| **`prod`** | `com.shopkeeper` | ShopKeeper | R8 full minification, ProGuard obfuscation, Crashlytics, Production Firestore |

### Common CLI Tasks

- **Assemble Dev Debug APK**:
  ```bash
  ./gradlew assembleDevDebug
  ```
- **Assemble Production Release APK**:
  ```bash
  ./gradlew assembleProdRelease
  ```
- **Bundle Production Release (Google Play AAB)**:
  ```bash
  ./gradlew bundleProdRelease
  ```

---

## 📐 Architectural & Coding Standards

### 1. Monetary Precision Invariant

> [!IMPORTANT]
> **Floating-point types (`Double` / `Float`) MUST NEVER be used for monetary calculations or database storage.**

- **Data & Entity Models**: Store prices in minor currency units (e.g., cents or poisha) using `Long`:
  ```kotlin
  // Correct:
  val purchasePriceMinor: Long,
  val salesPriceMinor: Long
  ```
- **Arithmetic**: Perform all profit, margin, and markup calculations on integer minor units.
- **Display Conversion**: Convert minor units to formatted strings at the UI boundary using [`FormatUtils`](file:///Users/kawsar/Desktop/projects/personal/Android/shop-purchase-management-app/app/src/main/java/com/shopkeeper/ui/util/FormatUtils.kt):
  ```kotlin
  FormatUtils.centsToCurrency(product.salesPriceMinor, currencySymbol)
  ```

### 2. Localization Invariant

> [!IMPORTANT]
> **No hardcoded text strings in Composables.**

All user-facing labels, dialog text, button captions, and error messages must be defined in [`res/values/strings.xml`](file:///Users/kawsar/Desktop/projects/personal/Android/shop-purchase-management-app/app/src/main/res/values/strings.xml) and accessed via `stringResource(R.string.your_key)`.

### 3. Presentation Layer & State Management

- Use **MVI / MVVM** architecture with Unidirectional Data Flow (UDF).
- ViewModels expose a single, immutable `StateFlow<UiState>`.
- Collect state in Composables using `collectAsStateWithLifecycle()` to prevent background battery drain.
- Handle Edge-to-Edge system bars using Compose insets:
  - Apply `Modifier.statusBarsPadding()` on custom top bars.
  - Apply `Modifier.navigationBarsPadding()` on bottom action bars.

### 4. Standardized Terminology

Always adhere to the standardized terminology across all user-facing copy, code comments, and documentation:
- **"Buying Price"**: The cost price paid by the shop to acquire the item.
- **"Selling Price"**: The retail price charged to customers.
- Do **not** use "Purchase Price" or "Sales Price".

---

## 🚦 Pre-PR Quality Gate

Before opening a Pull Request, run the automated verification suite locally. Every PR must pass all checks:

```bash
# 1. Run full unit test suite (22 tests)
./gradlew testProdDebugUnitTest

# 2. Run static analysis & Android Lint
./gradlew lintProdDebug

# 3. Verify clean compilation of production debug build
./gradlew assembleProdDebug
```

> [!NOTE]
> Pull Requests with failing tests or lint errors will be rejected by GitHub Actions CI.

---

## 💬 Git Commit Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

- `feat:` A new user-facing feature.
- `fix:` A bug fix.
- `refactor:` Code change that neither fixes a bug nor adds a feature.
- `docs:` Documentation updates or additions.
- `test:` Adding or updating unit/integration tests.
- `chore:` Build scripts, Gradle updates, or toolchain changes.

**Example**:
```bash
git commit -m "feat(sync): add exponential backoff retry to WorkManager sync worker"
git commit -m "fix(ui): correct dark theme badge contrast for low stock alerts"
```
