# 🚀 Solo-Developer CI/CD & Branch Management Runbook

> **Audience:** Single developer / solo-founder utilizing agentic AI pairing for ShopKeeper.  
> **Philosophy:** High automation, zero administrative toil, zero production regressions.

---

## 📌 Master Architecture Overview

```
                      ┌──────────────────────────────────────┐
                      │        Agentic Local Machine         │
                      │  - Plan & Execute with AI Agent      │
                      │  - Verify via Compose Previews & CLI │
                      └──────────────────┬───────────────────┘
                                         │
                                         ▼
                               .githooks/pre-push
                        (Auto Unit Tests & Android Lint)
                                         │
                                         ▼
                      ┌──────────────────────────────────────┐
                      │    Pull Request (Branch ➔ main)      │
                      └──────────────────┬───────────────────┘
                                         │
                                         ▼
                         .github/workflows/ci.yml
                      - Unit Tests (25/25)
                      - Android Lint
                      - Production R8 Assembly Check
                                         │
                                         ▼
                               Squash & Merge ➔ main
                                         │
                                         ▼
                               Git Tag: vX.Y.Z
                                         │
                                         ▼
                      .github/workflows/release.yml
                      - Builds Signed APK & Google Play AAB
                      - Packages ProGuard/R8 Mapping
                      - Publishes Official GitHub Release
```

---

## 🌿 1. Branching Strategy: Trunk-Based Development

As a solo engineer, avoid complex multi-branch GitFlow (which introduces merge hell and synchronization debt). Use **Trunk-Based Development with Semantic Tags**:

| Branch / Ref | Purpose | Rules & Protection |
| :--- | :--- | :--- |
| **`main`** | **Single Source of Truth** (Always stable, buildable, releasable) | Protected. Direct pushes disabled. Merges arrive via Pull Requests. |
| **`feat/<name>`** | New feature development (e.g. `feat/barcode-camerax`) | Created from `main`. Short-lived (1–2 days max). |
| **`fix/<name>`** | Bug fix or regression patch (e.g. `fix/button-wrap`) | Created from `main`. Merged back via PR. |
| **`v*` (Tags)** | Immutable Production Releases (e.g. `v1.0.0`) | Created on `main` when cutting an official release. |

---

## 🛠️ 2. Day-to-Day Feature Development Workflow

### Step 1: Create a Short-Lived Branch
```bash
git checkout main
git pull origin main
git checkout -b feat/barcode-scanner
```

### Step 2: Implement with Agentic Pairing
- Instruct your AI assistant to implement the feature following the invariants in [`AGENTS.md`](../AGENTS.md).
- Use the **Android Compose Preview** sidecar to inspect UI changes rapidly:
  ```bash
  # Check status of compose preview sidecar
  curl -s http://127.0.0.1:49258/api/status
  ```

### Step 3: Local Verification & Commit
Before pushing, verify your work with the fast quality gates:
```bash
./gradlew testProdDebugUnitTest lintProdDebug
git add .
git commit -m "feat(scanner): implement CameraX barcode viewfinder with ML Kit"
```

### Step 4: Push Branch (Protected by Local Pre-Push Hook)
```bash
git push -u origin feat/barcode-scanner
```
> The tracked `.githooks/pre-push` hook will automatically run unit tests and Android Lint before sending bytes over the wire. If any check fails, the push is safely halted.

### Step 5: Open PR & Merge
1. Open a Pull Request on GitHub targeting `main`.
2. The `.github/workflows/ci.yml` pipeline automatically triggers:
   - Tests: `./gradlew testProdDebugUnitTest` (25/25 passing).
   - Static analysis: `./gradlew lintProdDebug` (0 errors).
   - Obfuscation check: `./gradlew assembleProdRelease`.
3. Once the green checkmark appears, click **Squash and merge**.
4. Delete the remote feature branch.

---

## 📦 3. Zero-Toil Production Release Workflow

When you are ready to cut a production release:

### Step 1: Sync Local `main`
```bash
git checkout main
git pull origin main
```

### Step 2: Create and Push Semantic Version Tag
Follow [Semantic Versioning](https://semver.org/) (`vMAJOR.MINOR.PATCH`):
```bash
git tag -a v1.0.0 -m "Release v1.0.0: Initial production launch"
git push origin v1.0.0
```

### Step 3: Automated Pipeline Execution
Pushing the tag triggers [`.github/workflows/release.yml`](../.github/workflows/release.yml), which automatically:
1. Validates tests and lint prerequisites.
2. Builds the release APK (`ShopKeeper-1.0.0-release.apk`).
3. Builds the Google Play App Bundle (`ShopKeeper-1.0.0-release.aab`).
4. Extracts the ProGuard/R8 de-obfuscation mapping file (`ShopKeeper-1.0.0-mapping.txt`).
5. Publishes a new **GitHub Release** with auto-generated changelogs and attached downloadable assets.

---

## 🔒 4. Recommended GitHub Repository Settings

To lock in this automation on GitHub:

1. **Enable Branch Protection on `main`**:
   - Go to your repository on GitHub $\rightarrow$ **Settings** $\rightarrow$ **Branches** $\rightarrow$ **Add branch ruleset** (or branch protection rule).
   - Set **Branch pattern**: `main`.
   - Enable:
     - [x] **Require a pull request before merging**.
     - [x] **Require status checks to pass before merging**:
       - Add status check: `Test, Lint & Release Compilation`.
     - [x] **Require linear history** (or enforce squash merging).
     - [x] **Do not allow bypassing the above settings**.

2. **Configure Release Signing Secrets (Optional for Google Play Distribution)**:
   - Go to **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions**.
   - If using a private production release keystore, add:
     - `KEYSTORE_FILE`: Path or base64 decoded file.
     - `KEYSTORE_PASSWORD`: Keystore password.
     - `KEY_ALIAS`: Key alias name.
     - `KEY_PASSWORD`: Key password.
   - *Note:* If secrets are omitted, the build automatically and safely falls back to the bundled debug keystore configuration.

---

## 🛡️ 5. Setting Up Git Hooks on a New Machine

Whenever you clone the repository onto a new machine:
```bash
./scripts/setup-git-hooks.sh
```
This registers `.githooks/` as Git's active `core.hooksPath`.
