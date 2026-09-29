# Game-suite release record (schema version 1)

This document describes the suite build input and release-record format. They
let future release jobs give each changed product its own version and check a proposed
record describing a complete set of three game downloads.
**These tasks do not publish a record or a release.** They do not schedule builds,
reserve versions, create tags, or change the launcher.

## Implemented in this PR

### Build input

`-PsuiteReleaseVersion=0.51.01` (or `-PsuiteMegaMekVersion=0.51.01`
alongside it) overrides the Gradle version of the root and
its subprojects and the packaged `Version.properties` used by `new Version()`.
It does not edit the tracked source file. `:megamek:verifySuiteVersion` compares
the processed resource to the effective project version (excluding the
optional `-extraVersion` suffix); `:megamek:processResources` creates the
packaged file. The input accepts only canonical three-component decimal
versions: major has no leading zeros; minor and patch use two digits for
0–99 and no leading zeros above 99. Each component must fit in a Java
signed 32-bit integer (0–2147483647). Thus `0.51.01`, `0.51.02`,
`0.51.100` are distinct, while `0.51.1` and `0.51.001` are *not*
alternate allocatable slots. Without the input, the existing source-based
version and optional historic fourth-component revision are unchanged.
In suite mode the effective MegaMek version is `suiteMegaMekVersion`, defaulting
to `suiteReleaseVersion` for all-equal builds. With the input, a source revision is suppressed, including in the packaged
resource. The existing `extraVersion` build suffix remains available and
is not part of the release record version.

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

Schema version 1 has exactly these top-level keys: integer `schemaVersion: 1`,
canonical `version`, `membership` (`weekly`, `development`, or `milestone`),
`tag` (`v` followed by the version), `products`, and `mmData`. An optional
`minimumLauncherVersion` is allowed; its absence makes no promise about
launcher compatibility. This is an independently versioned launcher version:
three numeric components without leading zeroes (for example `0.14.5`), not
the padded suite version; suffixes and extra components are rejected.
`products` has exactly the keys `MegaMek`, `MegaMekLab`, and `MekHQ`.
Each product has exactly `repository`, `commit`, `version`, `tag`,
`releaseId`, and `asset`. Repositories are respectively
`https://github.com/MegaMek/megamek`,
`https://github.com/MegaMek/megameklab`, and
`https://github.com/MegaMek/mekhq`; each product version may be older than the
suite version (but never newer). Its tag is `v` plus its **own** version.
The bundle versions must be ordered MegaMek ≤ MegaMekLab ≤ MekHQ ≤ suite:
each newer dependency changes its consumer's distributable contents.
`releaseId` identifies the product's GitHub Release
within that repository. `asset` has exactly `assetId`, `name`, `sha256`, and
`size`. `assetId` identifies the download within its product release. Both
IDs must be positive JSON integers representable as signed 64-bit values
(not strings or decimals). Each name must be exactly
`<Product>-<product version>.tar.gz`; digests are lowercase 64-character hexadecimal
SHA-256, sizes are positive integer bytes.
`mmData` has exactly `repository` (`https://github.com/MegaMek/mm-data`)
and `commit`; all four commits are lowercase 40-character git SHA-1 IDs.
Unknown fields, duplicate JSON keys, missing fields, wrong types and
additional JSON documents are rejected. A record describes one downloadable
asset per product and one pinned mm-data source commit; it contains no
live URLs or floating branches.

The top-level version/tag identify a new complete Weekly (or other suite)
record, not three newly published artifacts. Allocate suite versions
monotonically. Only advance a product version when its distributable contents
change; reuse an unchanged release ID, asset ID, digest and bytes verbatim.
Dependency closure matters: a Lab-only change advances Lab and MekHQ (the
latter bundles the new Lab), but retains the existing MegaMek artifact and its
version. Changes to MegaMek or mm-data advance every affected bundle.
Validate the archived `suite-build.properties` and dependency jars against
the actual asset bytes and source/data pins before reusing an asset; this JSON
validator checks shape and relative versions, not dependency closure or
remote asset identity. An older MegaMek archive can legitimately contain
older, unrelated Lab/HQ pins; those are *not* claims that its bytes were built
from the newer suite's Lab/HQ sources.

## Planned for later PRs (not implemented here)

The other product-build PRs must produce full archives at their assigned product
versions from explicitly pinned source commits and an mm-data commit. Unchanged
product archives can instead be reused without republishing them. A coordinating
release job must reserve the next unused numeric version across the suite,
treating unpadded aliases as the same number and leaving the optional fourth
component to historical special point releases. It must check the real tags
and GitHub release IDs, inspect each uploaded asset's ID and name, and verify
its SHA-256 and byte size against the downloaded bytes.

Only after all three product assets exist and pass those checks should the
coordinator attach **one complete JSON record** to the MegaMek GitHub Release
as its last publication step. A partial record or mutable "latest" pointer
must not be published. The website and launcher can later use these records
to find complete releases. Update this document alongside the validator,
record example, and consumers if the format or publication rules change.
