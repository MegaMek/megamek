# Design Exercise - In-App Updater for the MegaMek Suite

**Status:** exploration only. No code written, nothing committed. 2026-08-05.

**Origin:** Discord thread (Hammer / Illiani). Proposal as stated:

> A "new version is available" message appears in the top-left of the launcher. An `Update`
> button sits with the normal buttons on the right. Pressing it when nothing is available
> says "no update". If there is an update, the updater checks a diff file we include in the
> build, downloads the build from GitHub, and switches out the old for the new.
> Nightlies behind a client setting, default off. All three products. A flag file in the
> build tells the updater which package to pull.

This document records what the codebase actually supports today, the three findings that
change the shape of the proposal, and a phased design.

---

## 1. Executive summary

The UI half of this is easy and the codebase is ready for it. The download half runs into
one hard number:

**A MegaMek release is 639 MB, and about 85% of it is data that almost never changes.**

That single fact makes Hammer's "diff file" the load-bearing part of the whole feature rather
than an optimisation. Without it, a nightly tester downloads 639 MB to receive roughly 200 KB
of changed Java.

Three findings reshape the proposal:

1. **The code/data split already exists upstream and is thrown away at packaging time.**
   `mm-data` is a separate public repo that publishes its own independently versioned
   `data.tgz`. Recovering that split in the release assets is the highest-leverage change
   available, and it is a build-side change, not a client-side one.
2. **Nightlies are not downloadable today.** They are GitHub Actions artifacts, which cannot
   be fetched anonymously, are double-zipped, expire, and ship in two JDK variants. The
   nightly channel cannot be built on them as they stand.
3. **The flag file Illiani asked for is mandatory, not a nicety.** The MekHQ distribution
   physically contains MegaMek and MegaMekLab. Updating a MekHQ install with the MegaMek
   tarball would delete MekHQ.

Recommended sequencing: ship the notification and a browser hand-off first (Phase 1, near-zero
risk, delivers most of the perceived value), and treat the actual self-replacement as a
separate later project gated on release-automation work.

---

## 2. What exists today

### 2.1 There is no update check anywhere in the suite

No `VersionChecker`, no `checkForUpdate`, no call to `api.github.com`, in MegaMek or MekHQ.
The only version comparison code is the client/server handshake gate
(`Server.java:674`, `AbstractClient.java:478-487`) and the savegame gate
(`MegaMekGUI.validateSaveVersion`, `:823-857`).

The closest existing infrastructure is the milestone metadata, which already knows how to
build GitHub release URLs:

- `megamek/mmconf/milestoneReleases.yml` - hand-maintained, current tail entry `v0.51.0`
- `common/util/milestoneReleaseInformation/MilestoneReleaseLoader.java:56` - Jackson YAML loader
- `MilestoneData.java:53-80` - holds `https://github.com/MegaMek/` + `/releases/tag/` URL parts
- `SuiteConstants.LAST_MILESTONE` (`SuiteConstants.java:68`) - **currently unused inside MegaMek**;
  it exists for MekHQ compatibility checks. This is a natural hook.

There is no HTTP client dependency in `megamek/build.gradle`. The only HTTP code in the repo is
`Server.registerWithServerBrowser` using legacy `HttpURLConnection`. Java 21 ships
`java.net.http.HttpClient`, and Jackson is already a dependency, so **no new dependency is needed
for the check itself** (extraction is a different story - see 5.4).

### 2.2 The launcher has room for exactly what was asked for

`MegaMekGUI.showMainMenu()` (`client/ui/clientGUI/MegaMekGUI.java:311`) is a two-column
`GridBagLayout`:

- `gridx=0, gridy=0, gridheight=9` - the splash `RawImagePanel`
- `gridx=1`, one row each: `labVersion` (gridy=0), then `hostB`, `loadB`, `scenarioB`,
  `connectB`, `editB`, `skinEditB`, `quitB` (gridy=1..7)

Two conveniences fall out of this:

- The splash reserves `gridheight=9` but the right column only uses **8** rows. An `Update`
  button can be inserted above `quitB` with no other layout change.
- The splash panel's `paint()` override (`getRawImagePanel`, `:466-499`) already composites
  Tip of the Day (bottom-left), the medal, and the logo. **Nothing is drawn in the top-left.**
  That is precisely the free real estate the proposal asks for.

`TipOfTheDay` (`common/util/TipOfTheDay.java:67`) has a `Position` enum with no `TOP_LEFT`
member; either add one or draw the banner directly in the same `paint()`.

For the "open the download page" fallback, the canonical helper already exists:
`UIUtil.browse(String url, @Nullable Component parent)` (`client/ui/util/UIUtil.java:122-135`).

### 2.3 Versioning

`Version.properties` (`megamek/resources/`) holds `major=0 / minor=51 / patch=01`. The root
`build.gradle:17-28` reads it to set the Gradle project version, and - importantly - **MekHQ's
`build.gradle:19` reads MegaMek's copy too.** All three products therefore share one version
number and are released together (all three v0.51.0 assets were published within two seconds
of each other on 2026-06-06). That is a genuinely useful property for an updater.

Two gotchas that will bite an implementation:

- **`Version.compareTo` deliberately ignores `extra`** (`Version.java:280`). So
  `0.51.01-nightly-2026-08-05` and `0.51.01-nightly-2026-08-04` compare **equal**.
  `isHigherThan()` cannot drive the nightly channel. Note also that `equals()` *does* include
  `extra`, so `is()`/`equals` is stricter than `compareTo == 0`.
- **`new Version(String)` rejects a leading `v`**, and every GitHub tag is `v0.51.0`. Strip it.

Nightly builds are identifiable: `nightly-ci.yml:37-38` writes `branch=nightly` and
`gitHash=<date>` into the gitignored `extraVersion.properties`, and passes
`-PextraVersion="nightly-<date>"`, producing `MegaMek-0.51.01-nightly-2026-08-05.tar.gz`.

### 2.4 What a release actually looks like

Measured from the GitHub API and the local tree:

| Product | Latest asset | Size |
|---|---|---|
| MegaMek | `MegaMek-0.51.0.tar.gz` | 639 MB |
| MegaMekLab | `MegaMekLab-0.51.0.tar.gz` | 628 MB |
| MekHQ | `MekHQ-0.51.0.tar.gz` | 690 MB |
| mm-data | `data.tgz` (own repo, own tag `v0.3.1`) | 545 MB |

Local `megamek/data/` uncompressed, by subdirectory:

```
images        637M
boards        127M
mekfiles       47M
forcegenerator 22M
fonts          20M
rat           8.4M
everything else under 8M
--------------------
data total    867M     vs.   build/libs (the code)    13M
```

Stable releases land roughly quarterly (Oct, Nov, Dec, Mar, Jun over the last year).

### 2.5 The three products are nested, not siblings

From `MekHQ/MekHQ/build.gradle:390-470` and `megameklab/megameklab/build.gradle:325-340`:

```
MegaMek dist      = MegaMek.jar + lib/ + data/ + mmconf/ + MegaMek.exe
MegaMekLab dist   = the above  + MegaMekLab.jar + MegaMekLab.exe
MekHQ dist        = the above  + MekHQ.jar + MekHQ.exe
```

All three share **one** `data/` directory and one `mmconf/`. So `MM subset-of MML subset-of MHQ`.
This is exactly why Illiani's flag file is required: MegaMek running inside a MekHQ install must
never fetch the MegaMek package.

### 2.6 User state that must survive an update

- `mmconf/` - `clientsettings.xml`, `gameoptions.xml`, `princessBehaviors.xml`,
  `customWeaponOrder.xml`, `recent_boards.yml`, `*.preferences`, `skins/`
- `savegames/`, `logs/`
- `userdata/` - the sanctioned override tree, documented in `userdata/README.md`
- `data/mekfiles/Customs/` - the recommended location for user units

Helpfully, `copyFiles` in `megamek/build.gradle:314-323` **already excludes** user config from
the distribution, so a fresh tarball contains no `clientsettings.xml` to clobber. A naive
"delete tree, extract new" would still destroy savegames, customs and userdata, so the swap
must be additive-with-exclusions rather than a wipe.

---

## 3. The three findings in detail

### 3.1 The code/data split already exists and is discarded at packaging time

`MegaMek/mm-data` is a separate public repo, pulled in via `includeBuild('../mm-data')` in
`settings.gradle` and Sync'd into `data/` by the `stageData*` tasks. It publishes its own
releases on its own cadence:

| mm-data tag | Published | Asset |
|---|---|---|
| v0.3.1 | 2026-01-24 | data.tgz (545 MB) |
| v0.3.0 | 2025-08-14 | data.tgz (544 MB) |
| v0.2.0 | 2025-08-04 | data.tgz (549 MB) |

Data moves roughly twice a year. Code moves nightly. The distribution flattens the two into
one 639 MB blob, and that flattening is the entire reason an updater looks expensive.

Restoring the split at release time turns the problem into arithmetic:

| Update scenario | Naive (today) | Component-split |
|---|---|---|
| Nightly, code only | 639 MB | ~90 MB (or ~15 MB for jars only) |
| Stable, same data version | 639 MB | ~90 MB |
| Stable, data bumped too | 639 MB | ~635 MB (unavoidable) |

A second, deeper option is available essentially for free: because `mm-data` is a **public
repo**, any individual data file is already fetchable at a stable, anonymous, per-tag URL:

```
https://raw.githubusercontent.com/MegaMek/mm-data/<tag>/data/images/units/...
```

That is per-file hosting with zero new infrastructure, which is what makes a true file-level
delta (Phase 3) practical rather than theoretical. Note the caveat in 5.5 about image atlasing.

**One gap to close:** `mm-data` does not stamp a version into what it emits, and
`mm-data/build.gradle.kts` has no version property at all. The installed app currently has no
way to know which data snapshot it is carrying. A `data/data-version.properties` written at
stage time is a prerequisite.

### 3.2 Nightlies are not downloadable

`nightly-ci.yml:70-77` publishes via `actions/upload-artifact@v7` as
`mm-release-tar-gz-temurin-jdk21` and `...-jdk25`. GitHub Actions artifacts:

- **require an authenticated token** with `actions:read` to download, even on public repos -
  there is no anonymous URL
- are **wrapped in an extra .zip** by GitHub, so you get a `.zip` containing a `.tar.gz`
- **expire** on the retention policy
- exist in **two JDK variants** per night, with no signal about which one a user should take

So the answer to "do we track nightlies?" is: not with the current pipeline. Making the nightly
channel real requires publishing nightlies as a **GitHub prerelease against a rolling tag**
(for example tag `nightly`, `prerelease: true`, asset replaced each run). That is anonymously
downloadable, never expires, and is a small addition to the existing workflow.

Related: **there is no release/tag/publish workflow in the repo at all.** The nine workflows
present are checkstyle, ci, code-coverage, codeql, dependency-scan, java-doc x2, nightly-ci and
qa-test-build. Releases are cut by hand. Any manifest or flag file the client depends on has to
be produced by new release automation, so that automation is a hard prerequisite for Phases 2
and 3, not a follow-up.

### 3.3 The flavor flag

Illiani's proposal is correct and there is a cheap, robust way to do it. Two independent
mechanisms, belt and braces:

**Declared** - a `manifest` file written into the distribution at package time, naming the
product. This is authoritative.

**Inferred** - a fallback for installs predating the manifest: look for `MekHQ.jar` then
`MegaMekLab.jar` in the install root and `lib/`, and take the outermost product found. There
is precedent for exactly this kind of sibling-detection in
`ClientGUI.printToMegaMekLab` (`ClientGUI.java:2846-2916`), which auto-detects the MML
executable and uses `ProcessHandle.current().info().command()` to find the JVM.

One subtlety worth flagging: because stable releases of all three go out together with one
shared version number, the stable channel is uniform across the suite. **Nightlies are not** -
each repo runs its own nightly CI independently, so MM-nightly and MHQ-nightly can be out of
step. The flavor flag therefore has to select the *repo* to query, not just the asset to
download.

---

## 4. Proposed design

### 4.1 Component layout

Everything lives in MegaMek (MML and MHQ both depend on it), so one implementation serves all
three products. Suggested new package `megamek.common.update` for the non-GUI half and
`megamek.client.ui.dialogs.update` for the GUI half.

```
UpdateManifest         record - parsed from the manifest file in the install root
UpdateChannel          enum   - STABLE, NIGHTLY
ProductFlavor          enum   - MEGAMEK, MEGAMEKLAB, MEKHQ  (+ detect() fallback)
ReleaseCatalog         queries the GitHub Releases API, returns available versions
UpdateAvailability     record - what is available, how big, what changed
UpdateChecker          orchestrates: manifest + channel + catalog -> UpdateAvailability
```

`UpdateChecker` runs off the EDT, on a `SwingWorker` or a virtual thread, and must never block
the launcher. If the network is unavailable the launcher must look and behave exactly as it
does today.

### 4.2 Client settings

Follow the four-step `ClientPreferences` idiom (constant, `store.setDefault` in the
constructor, getter, setter). Because `copyFiles` excludes `clientsettings.xml` from the
distribution, a fresh install has no settings file at all, so **every default must work
standing alone.**

| Constant | Default | Purpose |
|---|---|---|
| `UPDATE_CHECK_ENABLED` (`"UpdateCheckEnabled"`) | `true` | check on launcher show |
| `UPDATE_CHANNEL` (`"UpdateChannel"`) | `"STABLE"` | Illiani's nightly opt-in |
| `UPDATE_LAST_DISMISSED_VERSION` (`"UpdateLastDismissedVersion"`) | `""` | so "not now" is not asked again every launch |

Exposed as a checkbox plus a channel combo in `CommonSettingsDialog` via the standard
`checkboxEntry(...)` wiring (field declaration, `comps.add`, load in `setVisible`, persist in
`okAction`). Message keys go in
`megamek/resources/megamek/client/messages.properties` under the `CommonSettingsDialog.` prefix,
English only - never copy English into the de/es/ru files.

The one-shot-nag precedent to model is `LicensingDialog.showIfNeeded(JFrame)`
(`:214-235`) backed by `GUIPreferences.NAG_FOR_README`.

### 4.3 The manifest ("diff file")

One file at the install root, YAML (Jackson YAML is already a dependency and
`milestoneReleases.yml` sets the precedent). It carries the flavor flag, the component
versions, and - later - the per-file hashes.

```yaml
product: MEKHQ            # the flavor flag
version: "0.51.01"
channel: stable
components:
  code:
    version: "0.51.01"
    asset:  "MekHQ-code-0.51.01.tar.gz"
    sha256: "..."
  data:
    version: "0.3.1"      # mm-data release tag
    repo:    "MegaMek/mm-data"
    asset:   "data.tgz"
    sha256:  "..."
files:                    # Phase 3 only
  "lib/MegaMek.jar":  "sha256..."
  "data/images/...":  "sha256..."
```

Read at two levels:

- **Component level (Phase 2)** - compare local `components.data.version` against the remote
  manifest to decide whether data needs fetching at all. This is what turns a 639 MB nightly
  into a 90 MB one.
- **File level (Phase 3)** - diff local against remote file hashes and fetch only what changed,
  using the raw.githubusercontent per-tag URLs for data. Also gives a free "verify/repair
  install" command, which is independently valuable for support.

### 4.4 The swap

This is the genuinely hard part, and it is hard for one reason: **on Windows you cannot
overwrite the files of a running application.** `MegaMek.exe` (launch4j, `dontWrapJar = true`)
is locked while running, and the JVM memory-maps `lib/*.jar`.

Proposed state machine, with `update/` as a scratch directory inside the install root (same
filesystem, so renames are cheap and near-atomic):

1. **Download** to `update/staging/`. Resumable, checksummed against the manifest.
2. **Verify** the archive hash, then extract to `update/staged/`. Refuse to proceed on mismatch.
3. **Record** `update/pending.yml` and tell the user an update is ready, apply on restart.
4. **Apply**, on user confirmation: spawn a small external updater process, then `System.exit(0)`.
   The updater waits for the parent to exit (`ProcessHandle.of(pid).onExit().join()`), moves the
   old files to `update/backup/`, moves `staged/` into place, and relaunches.
5. **Confirm** on the next successful start: delete `update/backup/`. If the new version fails
   to start, the backup is still there to roll back from.

The updater helper must be a **standalone jar that is not on the running classpath** (otherwise
the JVM locks it too) with zero dependencies. `ClientGUI.printToMegaMekLab` is the precedent for
finding the JVM and spawning a sibling process.

The swap must be **additive with exclusions**, never a wipe: preserve `mmconf/`, `savegames/`,
`logs/`, `userdata/`, and `data/mekfiles/Customs/` (see 2.6).

### 4.5 The UI

Two touchpoints in `MegaMekGUI`, both small:

- **Banner** - draw in the splash panel's existing `paint()` override, top-left, using the same
  compositing approach as Tip of the Day. Only painted when an update is available.
- **Button** - a `MegaMekButton` with `UIComponents.MainMenuButton.getComp()`, inserted at
  gridy=8 above `quitB`, wired to the shared `actionListener` with a new action command
  alongside the existing `ClientGUI.FILE_GAME_NEW` and friends.

Per the proposal the button is **always present and always enabled**, so it doubles as a manual
"check for updates" affordance. Pressing it with nothing available shows the "no update" message.

All strings via `Messages.getString` under a `MegaMek.update.` prefix.

Per the project's diagnostic-logging rule, tag every gate and transition `[Updater]` and log the
*reason* on each failure path - channel selected, flavor detected, remote version found,
comparison result, why the button did nothing, why a download was skipped. A playtest report
will be "it says no update but there is one", and that has to be answerable from `megamek.log`
alone.

---

## 5. Risks and open questions

These are the things I would want decided before any code is written.

**5.1 Antivirus and SmartScreen.** A Java process that downloads executables and overwrites
`MegaMek.exe` is a textbook AV heuristic trigger, and the exe is unsigned. This has a real
chance of generating more support load than the feature saves. Worth a spike on a clean Windows
box before committing to Phase 2.

**5.2 Write permission.** Installs under `Program Files` are not writable by a normal user.
The updater must detect this up front and fall back cleanly to `UIUtil.browse(...)` on the
releases page rather than failing halfway through a swap.

**5.3 Multiplayer and save compatibility.** `Server.java:674` gates the client/server handshake
on an exact version match. If one player in a group auto-updates, the group breaks. More
seriously, **MekHQ has a milestone-based campaign save compatibility model**
(`milestoneReleases.yml`, `SuiteConstants.LAST_MILESTONE`), so auto-updating a MekHQ user across
a milestone boundary risks their campaign. At minimum the updater must consult the milestone
data and warn hard; arguably MekHQ should decline to auto-update across a milestone at all.

**5.4 tar.gz.** `distZip` is explicitly disabled (`megamek/build.gradle:448-452`); tar.gz is the
only archive produced. **Java has no built-in tar reader.** Either add `commons-compress` (a new
dependency, and `commons-io` is already present) or re-enable `distZip` for the update assets.
This is a real decision with a dependency cost attached.

**5.5 Image atlasing breaks a naive file-level delta.** The build runs `createImageAtlases`
(`megamek.utilities.CreateImageAtlases`) and then `deleteAtlasedImages`, so the files in an
*installed* `data/images` do not correspond one-to-one with the files in `mm-data`. Phase 3
must diff against the *packaged* manifest, not against the mm-data repo tree, and the
raw.githubusercontent shortcut only works for non-atlased assets. This meaningfully complicates
Phase 3 and is the main reason I would not commit to it up front.

**5.6 GitHub API rate limits.** 60 requests/hour unauthenticated per IP. One check per launch is
fine, but shared/NAT'd networks and any retry loop are not. Cache the result with the response
ETag and a timestamp, and fail silent.

**5.7 Bandwidth.** Release asset bandwidth is unmetered for public repos, but nightly testers
pulling 639 MB daily is not a neutral act. Another argument for doing 3.1 first.

**5.8 Partial application.** Power loss between "old moved to backup" and "staged moved into
place" leaves a broken install. The `pending.yml` state machine must be resumable and the
backup must not be deleted until the new version has started successfully once.

---

## 6. Recommended sequencing

I would deliberately split this into four pieces and ship the first one on its own.

**Phase 0 - release automation (prerequisite, no user-visible change).**
Publish nightlies as a rolling GitHub prerelease. Split release assets into code and data
components. Emit the manifest, including the product flag. Stamp a data version in `mm-data`.
Nothing else can be trusted until this exists.

**Phase 1 - notify only.** Banner in the top-left, `Update` button, channel setting,
"no update" message. The button opens the releases page via `UIUtil.browse`. No download, no
swap, no new dependency, nothing that can corrupt an install. **This delivers most of the
perceived value at near-zero risk and can ship independently of Phase 0** if it targets the
stable channel only, since the Releases API already provides everything it needs.

**Phase 2 - component-level download and swap.** Requires Phase 0. This is where the real
engineering risk lives: the helper process, the state machine, AV behaviour, rollback.

**Phase 3 - file-level delta.** Requires Phase 0 and a resolution to 5.5. Optional; treat as a
research spike rather than a commitment.

---

## 7. Answers to the specific questions raised in the thread

**"Do we track nightlies?"** Not with the current pipeline - GitHub Actions artifacts cannot be
downloaded anonymously and expire. Publishing nightlies as a rolling prerelease is a small
workflow change that makes the whole nightly channel possible. Also note that
`Version.compareTo` ignores the `extra` field, so nightly-to-nightly comparison has to be done
on the embedded date, not with `isHigherThan`.

**"Would this be for just MegaMek or all three?"** One implementation in MegaMek serves all
three, because MML and MHQ both depend on it. The flavor flag decides what gets fetched.

**"I run MekHQ but when I open MM I get an alert that is just for MM."** Solved by the flag
file. MegaMek launched from a MekHQ install reads `product: MEKHQ` from the manifest and offers
the MekHQ package. The inference fallback (look for `MekHQ.jar`, then `MegaMekLab.jar`) covers
installs that predate the manifest. This works cleanly for stable because all three products
share one version number sourced from MegaMek's `Version.properties`; it needs more care for
nightly, where the three repos build independently.

---

## 8. Things this document does not establish

- Whether AV/SmartScreen actually misbehaves on a real swap. Untested.
- Whether the maintainers want release automation at all - Phase 0 is a meaningful change to how
  releases are cut, and that is a process decision, not a technical one.
- Any measurement of a real code-only nightly delta. The ~90 MB and ~15 MB figures are derived
  from `build/libs` (13 MB) plus the runtime jars, not from an actual split build.
- Whether MekHQ should refuse to auto-update across a milestone boundary. That is a MekHQ
  campaign-integrity call.
