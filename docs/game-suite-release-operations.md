# Manual game-suite coordinator: setup and recovery

The coordinator and suite archive tooling require Python 3.10+; the manual
workflow installs 3.11. For local suite Gradle builds/verification, use
`-PsuitePythonExecutable=/absolute/path/to/python` when `python` on Windows
or `python3` elsewhere is missing or too old.

`.github/workflows/game-suite-release.yml` is **manual only**. No Tuesday
schedule is enabled and Nightlies are independent. Dispatch with membership
Weekly, Milestone, or Development. `dry_run=true` is the default: it performs
remote-byte preflight, frozen-source pinned builds and archive verification,
but creates **no** tags, releases, assets, or record. A verified unchanged
Weekly is a no-op (no new record). An unchanged Milestone or Development
creates a new complete record reusing all three verified product assets; the
first/bootstrap record must build all three products.

## Configuration before permitting live runs

Keep repository/org Actions variable `SUITE_RELEASES_ENABLED` unset or anything
other than the literal `true` until an operator has reviewed all three product
verifiers on official main and exercised dry runs. Configure
`SUITE_RELEASE_APP_ID` (Actions variable) and
`SUITE_RELEASE_APP_PRIVATE_KEY` (Actions secret). The GitHub App installation
must grant Contents write to **exactly** `MegaMek/megamek`,
`MegaMek/megameklab`, `MegaMek/mekhq`; do not grant access to `mm-data`.
Protect the `suite-release-live` environment with required human reviewers
and restrict deployment branches to the reviewed coordinator branch before
setting the enable variable to `true`. The dry-run environment is
`suite-release-dry-run`; it does not receive the App token. Workflow `GITHUB_TOKEN`
has only Contents read. The App token is minted only *after* all build work
and is requested for the three named repositories with Contents write.
Never put the private key in files, command output, or a workflow input.

## Check App access without publishing

After this workflow is merged to official main, run **Game suite App access check (manual)**
from the Actions tab on `main` and approve its `suite-release-live` environment request.
This check works while `SUITE_RELEASES_ENABLED` remains `false`; it does not enable releases,
build games, or publish tags, releases, assets, or a suite record.

The check uses the stored App ID and private key to request the same three-repository,
Contents-write token as the publisher. Successful token issuance confirms the requested
permission is granted; only GET requests then verify the token's exact repository scope and
read each repository's `main` ref. The action revokes its temporary token during job cleanup.
The key and token are never printed or uploaded.

A passing run verifies the configured credentials and current access, not an actual release
write, tag rules, archive validity, or future access. It does not replace a successful coordinator
dry run or authorization of the first live publication. If authentication or a GET fails, fix
the reported configuration/access problem before enabling live releases.

## Bootstrap and publication

Use `bootstrap=true` and an explicitly reviewed canonical
`bootstrap_floor` (for example `0.51.00`) **only** for the first record.
Existing records reject bootstrap; absent records reject normal operation.
The floor is not a reservation. Before bootstrap, inspect existing tags,
releases and assets for a prior incomplete attempt. Do not overwrite/delete
partial artifacts or automatically retry a failed write.

The workflow clones full history of four public official repositories into
`sources/{megamek,megameklab,mekhq,mm-data}`. After environment approval,
preparation captures each repository's `main` commit once; pinned staging creates
a clean sibling checkout at each frozen SHA. This snapshot is the source authority
for Weekly, Milestone, and Development, including verified Weekly no-ops. Later
merges to `main` do not change the snapshot or block publication, even during
uploads. A change arriving after capture is considered by a later run; cancel
the current run if it must include that change.

The preparation phase streams and
attests every prior archive (including companion jars and external Gradle
verifiers), retains these bytes, saves `suite-state/inventory.json`, builds
and verifies changed products without publishing, and saves `build_result.json`.
`seal.json` binds those files and the resulting archives. No publishing token
exists during preparation. A dry run ends with an explicit `DRY_RUN_ONLY`
or `DRY_RUN_VERIFIED_WEEKLY_NOOP` result. The live phase independently
rechecks version allocation, releases, tags, record and archive bytes
before any write; uploads the single complete record **last** and confirms
its bytes. No write has an automatic retry. A failure after a tag/release
write requires manual reconciliation of all affected repositories and the
latest complete record before another dispatch. Do not mistake a successful
build or a partial release for a published suite.

Gradle producers, archive verifiers and the record validator use the
repository-configured dependency resolution (including downloads on a fresh
runner); no pre-primed dependency cache is required. The sources remain pinned
to the frozen Git commits. Dependency resolution failures stop the run rather
than switching source commits or publishing unverified bytes.
Missing tasks, incomplete official commits, changed release tag targets,
unavailable dependencies, and changed release inventories are blockers, not reasons to switch to
an unreviewed worktree or fall back to a previous record.

### GitHub release presentation and channel identity

New MegaMek releases hosting a complete record are titled, for example,
`Weekly suite 0.51.01`. New Lab/HQ product releases include the product
version and publishing suite, for example,
`MekHQ 0.51.01 - Weekly suite 0.51.01`. Descriptions identify the channel,
link the exact complete record and each product archive, and list frozen sources.
These labels apply to Weekly, Development and Milestone. The GitHub
`prerelease=false` setting is unchanged; GitHub badges and version numbers do
not determine channel membership.

The record's `membership` is authoritative for the launcher and website.
The suite is complete only when its record is available; descriptions are
created before upload and do not imply successful completion. A record-only
suite host lists the actual reused product versions, which can differ from the
suite version. Reused product releases retain their original titles and
descriptions even if their archives appear in a later suite or another channel.
Existing published releases are not relabeled by this change.

## Diagnosing failed dry runs

Pinned Gradle failures identify the product and requested task. Subprocess
diagnostics inspect both stderr and stdout and prefer Gradle's
`What went wrong` section over its trailing advice. Excerpts remain bounded
and omit credential-bearing or URL-bearing lines and redact opaque strings;
they are not full build logs. For other commands, diagnostics retain a bounded
tail from each stream.

A failed preparation is not a verified bootstrap: dry-run completion and
publication must not be inferred from earlier successful steps. Check the
failed step and confirm the App-token/publication steps were skipped. Correct
the reported cause before a reviewed retry; never disable verification or
change the frozen commits to bypass a failure.

Focused offline checks (fake GitHub/Gradle, no remote writes):
`python3 -m unittest discover -s scripts -p 'test_suite_*.py'`.
The shared archive and canonical-data regression suites (including external
adapter task graphs) run with `python3 -m unittest discover -s gradle -p 'test_suite_*.py'`
from MegaMek, with the Lab/HQ/mm-data sibling checkouts present. Merge the
MegaMek shared verifier before Lab/HQ, whose builds load it from that sibling.
Use clean disposable sibling checkouts for producer checks: a normal developer
worktree may contain ignored custom images or other unpinned package inputs,
which suite builds intentionally reject even when `git status` is clean.
