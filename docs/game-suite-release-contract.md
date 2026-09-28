# Game-suite release record, version 1 (draft handoff)

This is a **future release contract**, not a release mechanism. Nothing in this
change publishes, schedules, reserves a version, creates tags, or changes the
public launcher. A release coordinator must arrange the builds and verify
their outputs before attaching **one complete JSON file** to the MegaMek
GitHub Release **last**. Never publish a partial record or a mutable "latest"
pointer. A record is a snapshot of exactly one release.

## Build input

`-PsuiteReleaseVersion=0.51.01` overrides the Gradle version of the root and
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
With the input, a source revision is suppressed, including in the packaged
resource. The existing `extraVersion` build suffix remains available and
is not part of the release record version.

## JSON contract

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
`https://github.com/MegaMek/mekhq`; each product version and tag must equal
the suite identity. `releaseId` identifies the product's GitHub Release
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

## Future coordinator handoff

Allocate the next numeric version **across the suite**, treating unpadded
aliases as the same number and leaving the optional fourth component to
historical special point releases. Build all three products at that
identity against explicitly pinned source commits and mm-data commit.
Check the actual tag and GitHub release ID in each product repository, inspect
each produced asset's ID and name and calculate its SHA-256 and byte size.
Validate the completed record,
verify the pinned commits and download digests against the real artifacts,
then attach the single JSON record to the MegaMek GitHub Release only after
all three product assets exist. Cross-repository orchestration, allocation,
tagging, uploads, website and launcher consumption are future work.
