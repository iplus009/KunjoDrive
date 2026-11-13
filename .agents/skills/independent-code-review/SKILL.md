---
name: independent-code-review
description: Use after implementation or before merging a pull request. Review changes skeptically for correctness, regressions, security, maintainability, and missing tests.
---

# Independent Code Review

Review the implementation as if another engineer authored it. Do not assume the implementation agent's summary is accurate.

## Workflow
1. Read the task requirements and acceptance criteria.
2. Inspect the actual Git diff, surrounding code, and relevant call sites.
3. Trace important data and control flows; check edge cases and failure paths.
4. Look for regressions, incorrect assumptions, unsafe input handling, authorization gaps, secret exposure, race conditions, resource leaks, and compatibility issues where relevant.
5. Check whether tests cover the changed behavior and meaningful failure cases.
6. Run focused tests or static checks when available; do not claim checks you did not run.
7. Report findings by severity with exact file and line references where possible.

## Finding severity
- CRITICAL: exploitable vulnerability, data loss, or severe production outage.
- HIGH: major correctness/security issue likely to affect users.
- MEDIUM: meaningful bug, missing validation, or regression risk.
- LOW: minor issue or maintainability concern.
- INFO: observation or optional improvement.

## Review rules
- Findings must be specific, reproducible, and tied to code evidence.
- Distinguish confirmed defects from hypotheses.
- Do not invent vulnerabilities or claim "no issues" after a superficial scan.
- If no findings are identified, say "No findings identified in the reviewed scope" and state what was reviewed and what was not.
- Do not modify code during a review unless explicitly asked to fix findings.

## Output
1. Findings, highest severity first.
2. Missing or weak tests.
3. Checks actually run and results.
4. Scope limitations and residual risks.
