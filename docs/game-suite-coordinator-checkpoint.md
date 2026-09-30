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

Use `bootstrap=true` and an explicitly reviewed canonical
`bootstrap_floor` (for example `0.51.00`) **only** for the first record.
Existing records reject bootstrap; absent records reject normal operation.
The floor is not a reservation. Before bootstrap, inspect existing tags,
releases and assets for a prior incomplete attempt. Do not overwrite/delete
partial artifacts or automatically retry a failed write.

The workflow clones full history of four public official repositories into
`sources/{megamek,megameklab,mekhq,mm-data}`; pinned staging creates a
clean sibling checkout at each frozen SHA. The preparation phase streams and
attests every prior archive (including companion jars and external Gradle
verifiers), retains these bytes, saves `suite-state/inventory.json`, builds
and verifies changed products without publishing, and saves `build_result.json`.
`seal.json` binds those files and the resulting archives. No publishing token
exists during preparation. A dry run ends with an explicit `DRY_RUN_ONLY`
or `DRY_RUN_VERIFIED_WEEKLY_NOOP` result. The live phase independently
rechecks remote heads, versions, releases, tags, record and archive bytes
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
Missing tasks, incomplete official commits, mutable refs, unavailable
dependencies, and changed inventories are blockers, not reasons to switch to
an unreviewed worktree or fall back to a previous record.

Focused offline checks (fake GitHub/Gradle, no remote writes):
`python3 -m unittest discover -s scripts -p 'test_suite_*.py'`.
The shared archive and canonical-data regression suites (including external
adapter task graphs) run with `python3 -m unittest discover -s gradle -p 'test_suite_*.py'`
from MegaMek, with the Lab/HQ/mm-data sibling checkouts present. Merge the
MegaMek shared verifier before Lab/HQ, whose builds load it from that sibling.
Use clean disposable sibling checkouts for producer checks: a normal developer
worktree may contain ignored custom images or other unpinned package inputs,
which suite builds intentionally reject even when `git status` is clean.
