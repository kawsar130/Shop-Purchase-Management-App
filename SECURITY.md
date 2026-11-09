# 🛡️ Security Policy

## 🔒 Supported Versions

We actively maintain and provide security patches for the following versions of **ShopKeeper**:

| Version | Supported | Notes |
| :--- | :---: | :--- |
| **`1.0.x`** | ✅ | Current production release |
| **`< 1.0.0`** | ❌ | Deprecated prototype releases |

---

## 🚨 Reporting a Vulnerability

If you discover a potential security vulnerability or sensitive data exposure in ShopKeeper, please report it responsibly. **Do not create public GitHub issues for security vulnerabilities.**

### Disclosure Protocol

1. **GitHub Private Vulnerability Advisory (Preferred)**:
   - Go to the repository's [Security Advisory tab](https://github.com/kawsar130/ShopKeeper/security/advisories).
   - Click **Report a vulnerability** to open a confidential report.
2. **Email Disclosure**:
   - If you cannot use GitHub Advisories, email the maintainer directly at: `kawsar130@gmail.com`
   - Include:
     - Component / Class affected.
     - Description of the vulnerability.
     - Minimal reproduction steps or Proof of Concept (PoC).
     - Potential impact on shop financial data or user isolation.

### Response Timeline
- **Initial Acknowledgment**: Within 48 hours.
- **Triage & Assessment**: Within 5 business days.
- **Remediation & Patch Release**: Fast-tracked via hotfix branch (`fix/<vulnerability>`) and published in an expedited patch release.

---

## 🛡️ Core Security Architecture & Invariants

ShopKeeper incorporates defense-in-depth principles across all application layers:

1. **Monetary Precision Invariant**:
   - IEEE 754 floating-point numbers (`Double`, `Float`) are strictly forbidden for pricing calculations or Room entity persistence.
   - Financial figures are stored as exact 64-bit integer minor currency units (`Long` cents), preventing financial truncation and rounding exploits.
2. **Offline-First Single Source of Truth (SSOT)**:
   - Presentation layer views never query remote backends directly; all UI state is derived reactively from Room SQLite.
   - Background synchronization uses Android Jetpack WorkManager with exponential backoff and network constraints.
3. **Multi-Tenant Data Isolation**:
   - Cloud Firestore Security Rules ([`firestore.rules`](firestore.rules)) strictly enforce shop owner and invited staff authorization.
   - Unauthenticated or cross-shop reads and writes are rejected at the Firestore engine level.
4. **Cryptographic Key & Credential Hygiene**:
   - Application API keys are cryptographically bound in Google Cloud Console to the authorized Android package (`com.shopkeeper`) and SHA-1 certificate fingerprints.
   - Production keystores (`*.jks`, `*.keystore`) are strictly gitignored and excluded from version control.
   - GitHub secret scanning and secret scanning push protection are actively enforced on all repository branches.
