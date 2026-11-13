---
name: evidence-based-completion
description: Use before declaring any coding task complete, reporting progress, or claiming a fix works. Requires observable evidence for every completion claim.
---

# Evidence-Based Completion

## Workflow
1. Restate the requested outcome and define observable acceptance criteria.
2. Inspect the current branch, working tree, and relevant files before editing.
3. Make the smallest change that satisfies the criteria.
4. Inspect the resulting diff; confirm only intended files changed.
5. Run the most relevant verification commands and observe their exit codes/output.
6. Match each acceptance criterion to evidence.
7. Report unresolved criteria as NOT VERIFIED, FAILED, or BLOCKED.

## Claim-to-evidence rules
- "Implemented": point to the changed file(s) and relevant diff.
- "Tested": name the exact test command and observed result.
- "Build passes": name the exact build command and observed result.
- "Fixed": demonstrate a regression test or reproducible before/after behavior.
- "Committed": provide the observed commit hash from Git.
- "Pushed": verify the remote branch contains the commit.
- "Deployed": verify the deployment system and deployed revision.
- "Secure": never make this blanket claim; state the checks and scope.

## Never
- Fabricate logs or test output.
- Infer success from the absence of an error message in a partial transcript.
- Treat code reading as runtime verification.
- Report a planned command as an executed command.
- Hide skipped tests, flaky tests, warnings, or blockers.

## Final report template
### Outcome
[What changed and what did not]

### Evidence
| Claim | Check performed | Result |
|---|---|---|
| ... | exact command/path/check | VERIFIED / FAILED / NOT RUN / BLOCKED |

### Changed files
- `path` — concise description

### Remaining risks
- List known limitations and unverified behavior.
