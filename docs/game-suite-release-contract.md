# Game-suite release record (schema version 2)

This document describes the suite build input and release-record format. They
let future release jobs give each changed product its own version and check a proposed
record describing a complete set of three game downloads.
**These Gradle tasks do not publish a record or a release.** The separate
coordinator workflows publish: the scheduled Experimental build runs unattended,
and manual live runs need a protected approval. See
[game-suite release operations](game-suite-release-operations.md) for setup and recovery.
These Gradle tasks add no schedule, version reservation, or launcher change.

## Implemented in this PR

### Build input

`-PsuiteReleaseVersion=0.51.01` (or `-PsuiteMegaMekVersion=0.51.01`
alongside it) overrides the Gradle version of the root and
its subprojects and the packaged `Version.properties` used by `new Version()`.
It does not edit the tracked source file. `:megamek:verifySuiteVersion` compares
the processed resource to the effective project version (excluding the
optional `-extraVersion` suffix); `:megamek:processResources` creates the
packaged file. The input accepts only canonical decimal versions
`major.minor.patch[.revision]`: major and revision have no leading zeros
(revision is 1 or more); minor and patch use two digits for 0–99 and no
leading zeros above 99. Each component must fit in a Java signed 32-bit
integer (0–2147483647). Thus `0.51.01`, `0.51.02`, `0.51.100` and the build
`0.51.00.2` are distinct, while `0.51.1`, `0.51.001` and `0.51.00.02` are
*not* alternate allocatable slots. A missing revision orders as 0, so
`0.51.00` < `0.51.00.2` < `0.51.01`. Without the input, the existing
source-based version and optional fourth-component revision are unchanged.
In suite mode the effective MegaMek version is `suiteMegaMekVersion`, defaulting
to `suiteReleaseVersion` for all-equal builds. With the input, the source
revision is replaced by the input's revision (omitted for three-part versions),
including in the packaged resource. The existing `extraVersion` build suffix
remains available and is not part of the release record version.

### MegaMek suite product archive (local preparation only)

Build with `./gradlew :megamek:buildMegaMekPackage
-PsuiteReleaseVersion=0.51.01 -PsuiteMegaMekCommit=<sha>
-PsuiteMegaMekLabCommit=<sha> -PsuiteMekHQCommit=<sha>
-PsuiteMmDataCommit=<sha>` (on one command line). Each pin is a lowercase
40-character SHA-1. Before any packaging tasks run, the suite tar task checks
that MegaMek, sibling `megameklab`, `mekhq`, and `mm-data` are separate Git
checkouts at the supplied HEADs with no tracked changes or unknown untracked
or ignored package-source inputs. Ignored local data mirrors are allowed only
when byte-identical to tracked mm-data files; known generated outputs and
excluded private settings and user data are not source inputs. Build and Gradle cache
outputs are outside this check. `extraVersion` cannot be
used for a suite product archive. The normal MegaMek distribution and historical
point-release path remain available without `suiteReleaseVersion`.

The full launcher-compatible `MegaMek-<version>.tar.gz` contains bundled
mm-data and a root-level `suite-build.properties` with `schemaVersion=1`,
`product=MegaMek`, `version`, `megamekCommit`, `megameklabCommit`,
`mekhqCommit`, `mmDataCommit`, and `minimumJavaVersion=21`. The
`:megamek:verifySuiteMegaMekArchive` task checks the archive root, file name,
identity, packaged jar version, launcher and bundled data, and rejects user
settings. `buildMegaMekPackage` runs this verification after packaging.
For local inspection of an existing archive, supply
`-PsuiteArchiveFile=/path/to/MegaMek-<version>.tar.gz` to the verification
task (with the suite version and four pins); this does not rebuild the archive
or check local Git inputs. It inspects the archived root and lib jars, not
possibly stale local build outputs.

The three suite archive tasks use the **same** read-only verifier in
`megamek/gradle/suite_archive_verifier.py`, invoked through
`megamek/gradle/suite_archive_adapter.gradle` by each composite build. The
suite build/verification tasks require an executable Python 3.10 or newer
(`python` on Windows, `python3` elsewhere); set
`-PsuitePythonExecutable=/absolute/path/to/python` when it is not on PATH.
The suite workflow installs Python 3.11. A missing, unusable or older
interpreter fails preflight before verification or data staging. Suite
producers stage tracked mm-data **Git blob bytes** at the declared pin in
their own build directories, including all four generated ZIPs; the
mm-data working tree is not rewritten. Non-suite builds continue to stage
the working checkout and its usual mm-data ZIP tasks. External archive
verification does not stage data or schedule any producer. The product
Sync image/loose-data patterns (HQ's unrestricted image copy is
implicit) and the verifier's expected pinned mm-data selection share
`megamek/gradle/suite_data_rules.json`; the verifier
also checks generated ZIP members against the pinned source tree and accounts
for the product's image-atlas substitutions. TAR headers/extensions and JAR
ZIP directories are bounded before parsing; payloads are streamed, never
extracted. `scripts/suite_archive_attestation.py` uses this scanner for its
preflight and retains the record/ref/SHA checks before invoking the same
Gradle verification tasks. In external `-PsuiteArchiveFile` mode no producer
task is scheduled; Lab also needs `-PsuiteMegaMekArchiveFile`, and HQ needs
both `-PsuiteMegaMekArchiveFile` and `-PsuiteMegaMekLabArchiveFile`.
Companion archives may have older *unrelated successor* commit pins, but
their own source/data identity, runtime version and packaged JAR bytes must
match the consuming archive. The shared regression suite is
`megamek/gradle/test_suite_archive_verifier.py`.

Suite builds generate only the canonical `MegaMek`, `MegaMekLab` and `MekHQ`
startup scripts, not case-only aliases from Gradle's default generator.
The shared adapter clears stale generated scripts before regenerating the
canonical scripts. Ordinary non-suite builds are unchanged.
The archive namespace must also be portable: paths and implicit parents are
checked for case/NFC aliases, file/directory conflicts, Windows device names,
trailing dots/spaces, colons and reserved launcher state. These checks apply
before payload parsing in both producer and external verification. The
512 UTF-16-code-unit limit applies to the decoded raw entry name, including
a directory's trailing slash, before normalization, matching the launcher.

Every suite rebuilds all three archives; archives proposed for publication must
satisfy the current portable-path, identity and dependency-closure checks.


**Merge prerequisite:** land MegaMek's shared `gradle/` verifier, adapter
and data rules before merging Lab or HQ changes that reference sibling
`../megamek/gradle/`. Keep all three suite adapters together when releasing;
normal non-suite distribution tasks retain their existing names and behavior.

### JSON record and local validation

`megamek/testresources/suite-records/complete.json` is a **synthetic example**
(fake commits, release and asset IDs, digests and sizes, not a real release). Validate a candidate
without publication using
`./gradlew :megamek:validateSuiteRecord -PsuiteRecordFile=/path/to/record.json`
or invoke `megamek.release.SuiteRecordValidator` with one file argument on
the MegaMek runtime classpath. An error exits nonzero. The fixture tests
cover invalid and incomplete records. This checks internal shape and
identity, **not** whether any commit, tag, download, size or digest exists
remotely or matches the actual bytes.

Schema version 2 has exactly these top-level keys: integer `schemaVersion: 2`,
canonical four-part `version` (for example `0.51.02.7`), `channel` (`core` or
`experimental`), `tag` (`v` followed by the version), `products`, and `mmData`.
The fourth component is an unpadded build counter starting at 1. An optional
`minimumLauncherVersion` is allowed; its absence makes no promise about
launcher compatibility. This is an independently versioned launcher version:
three numeric components without leading zeroes (for example `0.14.5`), not
the padded suite version; suffixes and extra components are rejected.
`products` has exactly the keys `MegaMek`, `MegaMekLab`, and `MekHQ`.
Each product has exactly `repository`, `commit`, `version`, `tag`,
`releaseId`, and `asset`. Repositories are respectively
`https://github.com/MegaMek/megamek`,
`https://github.com/MegaMek/megameklab`, and
`https://github.com/MegaMek/mekhq`. Every suite rebuilds all three products,
so each product's version must equal the suite version and its tag must equal
the suite tag. `releaseId` identifies the product's GitHub Release
within that repository. `asset` has exactly `assetId`, `name`, `sha256`, and
`size`. `assetId` identifies the download within its product release. Both
IDs must be positive JSON integers representable as signed 64-bit values
(not strings or decimals). Each name must be exactly
`<Product>-<version>.tar.gz`; digests are lowercase 64-character hexadecimal
SHA-256, sizes are positive integer bytes.
`mmData` has exactly `repository` (`https://github.com/MegaMek/mm-data`)
and `commit`; all four commits are lowercase 40-character git SHA-1 IDs.
Unknown fields, duplicate JSON keys, missing fields, wrong types and
additional JSON documents are rejected. A record describes one downloadable
asset per product and one pinned mm-data source commit; it contains no
live URLs or floating branches.

The `channel` is authoritative: the launcher and website must use it, never
GitHub's prerelease badge or the version number, to decide where a suite
belongs. Schema 1 records (with `membership`) are no longer produced or
accepted.

## Coordinator integration

The coordinator (`scripts/suite_release_*.py`, driven by the
`game-suite-*` workflows) freezes one commit per repository from a single
branch (`main` for Experimental, `releases/<line>` for Core), builds all
three products from those commits, and allocates the version as the highest
build number on the line across the three repositories' tags and releases
(drafts included) plus one. It checks the real tags and GitHub release IDs,
inspects each uploaded asset's ID and name, and verifies its SHA-256 and byte
size against the built bytes.

Only after all three product assets exist and pass those checks does the
coordinator attach **one complete JSON record** to the MegaMek GitHub Release
as its last publication step. A partial record or mutable "latest" pointer
must not be published. The website and launcher use these records
to find complete releases. Update this document alongside the validator,
record example, and consumers if the format or publication rules change.
