# Game-suite releases: channels, procedures and recovery

MegaMek, MegaMekLab and MekHQ are released together as one **suite** with one
version. There are two channels:

- **Core**: the stable line players get by default, built from a
  `releases/<line>` branch (for example `releases/0.51.02`) in each of the four
  repositories (megamek, megameklab, mekhq, mm-data).
- **Experimental**: the newest development work, built from `main` every
  Tuesday at 05:00 UTC and on demand. Players see it only after opting in.

Run every workflow from the **Actions** tab on `main`. The workflow file always
comes from `main`, even when it builds a release branch.

| Workflow | Builds | Publishes |
| --- | --- | --- |
| **Game suite release - Experimental** | `main` | an Experimental build (GitHub prerelease, never "latest") |
| **Game suite release - Core** | the newest `releases/` branch | a Core build (marked "latest") |
| **Game suite release - Cut Core** | `main` | creates `releases/<line>` in the 4 repositories, publishes the first Core build of the line, then moves `main` to the next line |

## Version numbers

Versions have four parts: `0.MM.LL.B`. `0.MM.LL` is the **line** (the
`Version.properties` major, minor and patch on that branch), and `B` is an
unpadded build counter. The counter keeps counting across both channels of a
line: the next build is the highest build number on the line among the three
repositories' tags and releases (drafts included), plus one.

Example:

1. `main` is on line `0.51.02`. Weekly Experimental builds publish
   `0.51.02.1` … `0.51.02.32`.
2. Cut Core with line `0.51.02`: `releases/0.51.02` is created and Core
   `0.51.02.33` is published. `main` moves to `0.51.03`.
3. A Core hotfix on `releases/0.51.02` publishes `0.51.02.34`. The next
   Experimental from `main` is `0.51.03.1`.

A tag or release occupies its number even if the publication around it
failed, so a retry after a partial failure moves to the next number. If you
delete the partial tags and releases first, the retry reuses the number.

## The four procedures

### First Core release and the next Experimental

1. Make sure `main` is ready to become Core and that `Version.properties` on
   `main` names the line you want (for example `0.51.01`).
2. Optionally rehearse: run **Cut Core** with that line and `dry_run=true`
   (the default). It reports the branches it would create and builds `main` as
   the new line, but creates and publishes nothing.
3. Run **Cut Core** with `dry_run=false` and approve the `suite-release-live`
   deployment. In one run it:
   - creates `releases/<line>` from `main` in all four repositories (a rerun keeps
     branches that already exist);
   - builds and publishes Core `<line>.N` from those branches;
   - commits the next line to `Version.properties` on `main` (for example
     `0.51.01` → `0.51.02`).
4. The next Experimental (scheduled or manual) builds `main` on the new line
   and publishes `<new line>.1`.

Cut Core refuses a line that is not `main`'s current line (unless a Core
release on that line was already published, which makes it a rerun), or when a
newer `releases/` branch already exists. While a `releases/<line>` branch exists for
`main`'s line, Experimental refuses to run until `main` moves on. If the bump
step failed, rerun Cut Core with the same line: it keeps the branches, and
since nothing changed it does not publish again but completes the bump. If
branch creation stopped partway and `main` has moved since, the rerun refuses
to mix snapshots: delete the partial `releases/<line>` branches and rerun.

### Patching a Core release (hotfix)

1. Merge the fix into the `releases/<line>` branch of each repository that
   needs it (a pull request targeting that branch; CI runs on `releases/**`).
   If `main` also needs the fix, implement it there separately: the branches
   diverge, so a cherry-pick may not apply.
2. Run **Game suite release - Core** (`dry_run=false`) and approve the
   deployment. It builds the newest `releases/` branch and publishes the next
   number on that line.

Core always builds the newest `releases/` branch; older lines are not
patched. If nothing changed since the last Core build, the run ends as a no-op.

### Weekly Experimental

Nothing to do. Every Tuesday at 05:00 UTC the schedule builds `main` and
publishes the next Experimental build, without approval (environment
`suite-release-weekly`). If nothing changed since the last Experimental build,
the run is a no-op and publishes nothing. A failed scheduled run opens (or
comments on) an issue titled "Scheduled Experimental suite release failed";
close it once the cause is fixed.

The schedule runs only in `MegaMek/megamek` and only while the Actions
variable `SUITE_RELEASES_ENABLED` is `true`. Set it to anything else to pause
it.

### Manual Experimental

Run **Game suite release - Experimental** with `dry_run=false` and approve the
`suite-release-live` deployment. It is identical to the weekly build. Use
`dry_run=true` to build and verify without publishing.

## What a run does

1. Freezes one commit per repository from the branch being built, and
   allocates the version.
2. Clones the four official repositories and builds, tests and verifies all
   three products from exactly those commits (MegaMekLab and MekHQ consume the
   MegaMek and MegaMekLab archives built earlier in the same run).
3. Seals the inventory and archives. A dry run or no-op stops here with
   `DRY_RUN_ONLY` or `NOOP`.
4. Mints a short-lived write token and rechecks the inventory, then:
   - generates release notes for each product from the pull requests merged
     since the previous build of the same channel;
   - creates the tag, release and archive in each repository;
   - uploads the suite record (`suite-record-<version>.json`) to the MegaMek
     release **last**.

The suite is published only when its record exists; the launcher and website
use the record's `channel`, not GitHub's prerelease badge. Later merges to the
branch do not affect a run in progress. No write is retried automatically.

Every run writes a job summary and uploads `inventory.json`,
`build_result.json` and `seal.json` as the `suite-state-*` artifact for 30
days. All three workflows share one concurrency group, so only one release
runs at a time.

## Configuration

- Actions variable `SUITE_RELEASE_APP_ID`: the suite release GitHub App.
- Environment secret `SUITE_RELEASE_APP_PRIVATE_KEY` on both
  `suite-release-live` and `suite-release-weekly`.
- Environments:
  - `suite-release-live`: required reviewers; used by manual live runs.
  - `suite-release-weekly`: no reviewer; used only by the schedule.
  - `suite-release-dry-run`: holds no key.

  Restrict the deployment branches of the live and weekly environments to
  `main`.
- Actions variable `SUITE_RELEASES_ENABLED`: live runs and the schedule
  require `true`.

The App needs Contents write on megamek, megameklab, mekhq and mm-data. Each
step requests the narrowest token it needs:

- branch creation: the four repositories;
- publication: megamek, megameklab and mekhq;
- moving `main`: megamek only.

Repository rules must allow the App to:

- create `releases/*` branches;
- push the version commit to MegaMek `main` (add the App as a bypass actor on
  the main-branch ruleset).

Protect `releases/*` like `main` (pull requests and CI), with the App as a
bypass actor for branch creation. Never put the private key in files, command
output or workflow inputs.

### Check App access without publishing

Run **Game suite App access check (manual)** on `main` and choose the
environment whose key to check. It requests the same four-repository token,
then only reads (the token's repository scope and each `main` ref). It does
not build, tag or publish anything, and works while releases are disabled.

## Recovery

- **Failed before publication** (build, test, verification): nothing was
  written. Fix the cause on the branch and rerun.
- **Failed during publication**: some tags, releases or archives may exist
  without a record. The suite is not published and the launcher ignores it.
  Inspect the three repositories and rerun. If you leave the incomplete tags
  and releases, the rerun takes the next build number. If you delete them in
  all three repositories first (drafts included), the rerun reuses the same
  number.
- **Broken published build**: do not replace its assets. Fix it on the
  branch and publish a new build; the launcher moves to the newest record of
  its channel.

## Diagnosing failures

Pinned Gradle failures name the product and task. Diagnostics prefer Gradle's
`What went wrong` section, are bounded, and omit credential-bearing lines.
Start from the job summary and the `suite-state-*` artifact. Never disable
verification or change the frozen commits to bypass a failure.

Offline checks (fake GitHub and Gradle, no remote writes):
`python3 -m unittest discover -s scripts -p 'test_suite_*.py'`.
The shared archive and data regression suites run with
`python3 -m unittest discover -s gradle -p 'test_suite_*.py'` from MegaMek with
the MegaMekLab, MekHQ and mm-data sibling checkouts present. Use clean sibling
checkouts: a developer checkout may contain ignored data or images that suite
builds intentionally reject.

The tooling requires Python 3.10+ (the workflows install 3.11). For local
suite Gradle builds, pass `-PsuitePythonExecutable=/path/to/python` if
`python`/`python3` is missing or too old. The workflows set `TMPDIR` to the
runner's root disk, because the builds exceed the RAM-backed `/tmp`.
