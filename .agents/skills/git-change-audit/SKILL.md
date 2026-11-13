---
name: git-change-audit
description: Use before and after code changes, before committing, and when reporting repository progress. Verify branch, working tree, diff, commits, and remote state from Git evidence.
---

# Git Change Audit

## Before work
1. Run `git status --short --branch`.
2. Record the current branch and HEAD commit.
3. Inspect existing modifications before touching files.
4. Preserve unrelated user changes. Never use reset, clean, checkout-over-user-work, or force push as a shortcut.

## After work
1. Run `git status --short`.
2. Run `git diff --check`.
3. Inspect `git diff --stat` and the full relevant diff.
4. Check untracked files separately; ordinary `git diff` does not show them.
5. Verify that every changed file is relevant to the task.
6. If committing was requested, inspect staged changes before commit and verify the resulting commit hash afterward.
7. If pushing was requested, verify the remote branch/commit after push. A local commit is not proof of a push.
8. If a pull request or CI run is involved, inspect its actual status and checks.

## Safety
- Never stage or commit unrelated changes.
- Never amend, rebase, force-push, or discard user work without explicit authorization.
- Do not commit secrets, credentials, private keys, or local environment files.
- Ask before destructive Git operations.

## Report
State branch, starting HEAD (when captured), changed files, diff summary, untracked files, `diff --check` result, commit SHA if created, and remote verification if performed. Use NOT VERIFIED when a remote check was not done.
