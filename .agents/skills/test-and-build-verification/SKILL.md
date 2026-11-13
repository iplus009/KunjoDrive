---
name: test-and-build-verification
description: Use when implementing or fixing code, preparing a release, or verifying a bug fix. Select and run meaningful tests, lint, type checks, and builds without misreporting results.
---

# Test and Build Verification

## Workflow
1. Inspect package manifests, scripts, CI workflows, test configuration, and project documentation. Do not guess the stack or invent commands.
2. Identify the smallest relevant tests, then broader checks appropriate to the change.
3. If fixing a bug, add or identify a regression test that fails before the fix when practical.
4. Run checks in a sensible order: focused test, related suite, lint, type check, build. Use the repository's actual scripts.
5. Observe each command's completion and exit status. If a command times out or is interrupted, mark it INCOMPLETE, not passed.
6. If a check cannot run, state the exact reason and what remains unverified.
7. Re-run relevant checks after any code changes made in response to failures.

## Test integrity
- Do not delete, weaken, skip, or mock away meaningful assertions to get green results.
- Do not change production behavior just to satisfy an incorrect test without investigating.
- Separate pre-existing failures from failures introduced by the current change when evidence allows.
- Report flaky tests as flaky and show repeat attempts; do not cherry-pick a successful run.
- Never claim full-suite coverage if only focused tests ran.

## Result format
For each check, include:
- Exact command
- Scope (which tests/files/packages)
- Result: PASSED / FAILED / NOT RUN / BLOCKED / INCOMPLETE
- Relevant concise output or failure summary

A successful build does not prove runtime correctness. A passing test suite does not prove absence of bugs.
