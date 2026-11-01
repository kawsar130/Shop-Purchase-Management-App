# Pull Request: [Title / Description]

## 📌 Reference & Scope
- **Sprint / Phase:** `Phase X: <Name>`
- **Task ID(s):** (as defined in `ROADMAP.md` or `docs/V2_EVOLUTION_SPEC.md`)
- **Type of Change:** `[ ] Feature` | `[ ] Bugfix` | `[ ] Refactor` | `[ ] Docs` | `[ ] Chore`

---

## 📝 Summary of Changes
- *Bullet point 1 of what was changed*
- *Bullet point 2 of what was changed*

---

## 🛡️ Quality Gate & Verification Checklist
- [ ] Code compiles cleanly with zero warnings/errors.
- [ ] Unit tests added / updated for new or modified logic.
- [ ] Pre-push quality gate executed successfully:
  ```bash
  ./gradlew lintProdDebug testProdDebugUnitTest assembleProdDebug
  ```
- [ ] Monetary values strictly use 64-bit integer minor units (`Long` cents). No `Double`/`Float` arithmetic.
- [ ] All user-facing strings are in `res/values/strings.xml` (no hardcoded strings in Composables).
- [ ] Consistent pricing terminology used: `"Buying Price"` and `"Selling Price"`.
- [ ] No hardcoded credentials, keystores, or secrets committed.
- [ ] Verified manually on an Android emulator or physical device.

---

## 📸 Screenshots / Test Output (If Applicable)
*(Attach test results or UI before/after screenshots here)*
