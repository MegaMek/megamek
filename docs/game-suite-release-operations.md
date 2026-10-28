# Game-suite coordinator: setup, schedule and recovery

The coordinator and suite archive tooling require Python 3.10+; the
workflow installs 3.11. For local suite Gradle builds/verification, use
`-PsuitePythonExecutable=/absolute/path/to/python` when `python` on Windows
or `python3` elsewhere is missing or too old. The workflow sets `TMPDIR` to the
runner's temporary directory on the root disk: the frozen worktrees and builds
exceed the RAM-backed `/tmp` (about 7.8 GB) on Ubuntu 26.04 runners.

`.github/workflows/game-suite-release.yml` runs two ways:

- **Scheduled Weekly**: every Tuesday at 06:00 UTC (Monday night in the
  Americas). A scheduled run is always membership Weekly, live
  (`dry_run=false`), without bootstrap, and uses the `suite-release-weekly`
  environment, which publishes without a reviewer. It runs only on
  `MegaMek/megamek` and only while `SUITE_RELEASES_ENABLED` is `true`; set the
  variable to anything else to pause the schedule (the job is skipped, not failed).
- **Manual dispatch** from `main`: membership Weekly (a corrective Weekly
  between schedules), Milestone, or Development. `dry_run=true` is the
  default and uses `suite-release-dry-run`; a manual live run uses
  `suite-release-live` and waits for a required reviewer.

## Version numbers

- **Weekly** versions are allocated automatically: the newest *published*
  three-part release across the three repositories (draft releases are
  ignored) plus the next unused fourth-component revision. After `v0.51.0`
  and `v0.51.00.1`, the next Weekly is `0.51.00.2`, then `0.51.00.3`, and so
  on. Leave the `version` input empty; it is rejected for Weekly.
- **Development and Milestone** releases (the regular X.YY.ZZ releases, such as
  the quarterly `0.51.01`) are dispatched manually with an explicit `version`.
  It must be canonical three-part, unused by any tag or release (including
  drafts), and newer than every published release. Once it is published, the
  following Weekly becomes `0.51.01.1`.

The ordering is `0.51.00` < `0.51.00.2` < `0.51.01` < `0.51.01.1`, which matches
MegaMek's `Version` and the launcher. The packaged `Version.properties` carries
the Weekly number as `revision`.

After every live publication (Weekly, Milestone or Development, but not a dry
run or a verified no-op) the workflow records the released version in the
tracked `megamek/resources/Version.properties` on megamek `main`. That keeps
local and nightly builds of all three products reporting the current version.
It runs `./gradlew :megamek:setVersionProperties -PnewVersion=<version>` on
current `main`, which rewrites only `major`, `minor`, `patch` and `revision`
(setting `revision` for a Weekly and commenting it out otherwise). It then
commits that one file as the App bot with `[skip ci]` and pushes to `main`. The
same task can be run by hand to fix the file.

Allocation never reads this file: Weekly versions still come from published
releases and tags. A MegaMek `main` whose only difference from the previous
record is in the non-comment version lines of `Version.properties` is treated as
unchanged. `prepare` freezes MegaMek at the previous record's commit, so that
commit alone never makes the next Weekly a release. Any other change to the
file, such as `notes` or `splashTag`, still counts as a MegaMek change.

If a version-commit step fails, the release is still complete and needs no
reconciliation; only the commit is missing. Do not re-dispatch to get it: a
re-run Weekly is a verified no-op that skips the commit, and a re-run Milestone
or Development would publish another release. Instead run the task locally and
merge the result through a normal pull request.

A dry run performs
remote-byte preflight, frozen-source pinned builds and archive verification,
but creates **no** tags, releases, assets, or record. A verified unchanged
Weekly is a no-op (no new record). An unchanged Milestone or Development
creates a new complete record reusing all three verified product assets; the
first/bootstrap record must build all three products.

Every run writes a job summary (suite version, membership, each product's
version, built or reused, archive SHA-256, frozen source commits, and the
result or the `BLOCKED` reason) and uploads the sealed `inventory.json`,
`build_result.json` and `seal.json` as the `suite-state-*` artifact for 30 days.
A failed **scheduled** run opens an issue titled
"Scheduled Weekly suite release failed", or comments on it if it is still open.
Close the issue once the cause is resolved.

## Configuration before permitting live runs

Keep repository/org Actions variable `SUITE_RELEASES_ENABLED` unset or anything
other than the literal `true` until an operator has reviewed all three product
verifiers on official main and exercised dry runs. Configure
`SUITE_RELEASE_APP_ID` (Actions variable) and
`SUITE_RELEASE_APP_PRIVATE_KEY` (environment secret on **both**
`suite-release-live` and `suite-release-weekly`). The GitHub App installation
must grant Contents write to **exactly** `MegaMek/megamek`,
`MegaMek/megameklab`, `MegaMek/mekhq`; do not grant access to `mm-data`.
Protect the `suite-release-live` environment with required human reviewers.
Restrict deployment branches of `suite-release-live` and
`suite-release-weekly` to `main`; the workflow selects `suite-release-weekly`
only for scheduled events, so a manual dispatch cannot use the unattended
environment. Set the enable variable to `true` only after this is in place.
The dry-run environment is
`suite-release-dry-run`; it does not receive the App token. Workflow `GITHUB_TOKEN`
has only Contents read (plus Issues write in the separate failure-report job).
The App token is minted only *after* all build work
and is requested for the three named repositories with Contents write.
After publication a second token, requested for `MegaMek/megamek` alone, pushes
the `Version.properties` commit to `main`. If `main` is protected by a branch
rule or ruleset, the App must be allowed to bypass it for direct pushes, or that
step fails after an otherwise complete release.
Never put the private key in files, command output, or a workflow input.

## Check App access without publishing

After this workflow is merged to official main, run **Game suite App access check (manual)**
from the Actions tab on `main`, choose the environment whose key to check
(`suite-release-live` needs its reviewer approval; check `suite-release-weekly`
after adding its copy of the key).
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

Use `bootstrap=true` **only** for the first record (a manual run; scheduled
runs never bootstrap). It builds all three products and allocates the version
as above, so no floor is needed.
Existing records reject bootstrap; absent records reject normal operation.
Before bootstrap, inspect existing tags,
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
`Weekly suite 0.51.00.2`. New Lab/HQ product releases include the product
version and publishing suite, for example,
`MekHQ 0.51.00.2 - Weekly suite 0.51.00.2`. Descriptions identify the channel,
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

### Recovering from a non-portable published archive

The first Weekly `0.51.01` (published October 2026) contained case-only
generated startup-script aliases such as `bin/MegaMek` and `bin/megamek`, which
the launcher correctly refuses. The packaging/verifier fix now rejects such
archives before publication. Its releases and record were removed. No complete
record exists, so the next live run must use `bootstrap=true`; it yields Weekly
`0.51.00.2` (after the published `v0.51.0` and the `v0.51.00.1` Core Rules
release). Scheduled runs never bootstrap: until that record exists, a scheduled run is
blocked ("absent records reject normal operation") and opens the failure issue,
so keep `SUITE_RELEASES_ENABLED` off until the bootstrap, then turn it on
(which also starts the schedule).

The `v0.51.01` tags and draft releases prepared outside the coordinator occupy
that version, so a coordinator-run `0.51.01` is blocked until they are removed.
Releases go through the coordinator so the launcher sees them; coordinate with
the release manager to delete those drafts and tags (their notes can be copied
to the coordinator's releases) before dispatching `0.51.01`. Afterwards the next
Weekly becomes `0.51.01.1`.

If a future published archive is found to be broken, do not replace its
assets or change its record hashes. Merge the fix and run a manual live Weekly
(or the next schedule) with `bootstrap=false`; the source change rebuilds the
affected products, historical bytes are attested read-only with their pinned
contract, and the launcher moves to the new record.

## Diagnosing failed dry runs

Pinned Gradle failures identify the product and requested task. Subprocess
diagnostics inspect both stderr and stdout and prefer Gradle's
`What went wrong` section over its trailing advice. Excerpts remain bounded
and omit credential-bearing or URL-bearing lines and redact opaque strings;
they are not full build logs. For other commands, diagnostics retain a bounded
tail from each stream.

A failed preparation is not a verified bootstrap: dry-run completion and
publication must not be inferred from earlier successful steps. Start from the
job summary and the `suite-state-*` artifact, then check the
failed step and confirm the App-token/publication steps were skipped. Correct
the reported cause before a reviewed retry; never disable verification or
change the frozen commits to bypass a failure.

Focused offline checks (fake GitHub/Gradle, no remote writes):
`python3 -m unittest discover -s scripts -p 'test_suite_*.py'`.
The **Game suite release tooling tests** workflow runs both suites on pull
requests and pushes that touch the suite scripts, shared verifier, record
fixtures or `game-suite-*` workflows.
The shared archive and canonical-data regression suites (including external
adapter task graphs) run with `python3 -m unittest discover -s gradle -p 'test_suite_*.py'`
from MegaMek, with the Lab/HQ/mm-data sibling checkouts present. Merge the
MegaMek shared verifier before Lab/HQ, whose builds load it from that sibling.
Use clean disposable sibling checkouts for producer checks: a normal developer
worktree may contain ignored custom images or other unpinned package inputs,
which suite builds intentionally reject even when `git status` is clean.
