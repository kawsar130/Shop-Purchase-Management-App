# ADR-0005: Trunk-Based Branching, Pre-Push Hooks & Automated PR CI Quality Gate

## Status
**Accepted** (2026-11-07)

## Context
In a solo-developer agentic workflow, manual testing and ad-hoc git branching lead to regressions, broken main branches, and drift. Long-lived feature branches create massive merge conflicts. Without server-side branch protection, accidental broken pushes or direct edits can compromise the release.

## Decision
1. **Trunk-Based Development**:
   - `main` is always releasable.
   - All work happens in short-lived branches (`feat/<name>`, `fix/<name>`, `chore/<name>`).
2. **Server-Side Branch Protection on `main`**:
   - Direct pushes to `main` are blocked.
   - Merging requires an automated Pull Request where CI status check (`Test, Lint & Release Compilation`) passes.
   - Zero required human approvals (`required_approving_review_count: 0`), allowing solo developers to self-merge green PRs immediately.
   - Force pushes and branch deletions are permanently disabled.
3. **Local Pre-Push Hooks**:
   - Version-controlled `.githooks/pre-push` executes `./gradlew testProdDebugUnitTest lintProdDebug` locally before permitting any push.
4. **Zero-Toil Tagged Releases**:
   - Pushing a version tag (`v*`) triggers [`.github/workflows/release.yml`](../../.github/workflows/release.yml) to build release APKs, Google Play AABs, and publish a GitHub Release automatically.

## Consequences
- **Positive**: Complete defense against broken pushes; rapid automated PR verification (30-60s); clean linear git history with squash-merging; reproducible tag releases.
- **Negative**: Requires opening a PR even for small fixes (automated via `gh pr create` & `gh pr merge`).
