---
name: rsa-validate-release-pom
description: Validate this branch's release POMs on Maven Central against the known previous baseline. Reads each module's version from the branch's RudderStackBuildConfig.kt (never from Maven's <release>/<latest>, so betas can't leak in), fetches that version's POM for all 8 modules (core, android, adjust, appsflyer, braze, facebook, firebase, sprig), compares each against the known-good baseline, and reports pass/fail. Use when the user asks to validate release POMs, check release artifacts, verify release publish, or mentions "validate release pom", "check release", "verify release".
---

# Release POM Validator

Validates the release POMs published by **this branch** against the **known previous baseline**.

The version to validate for each module is read from the branch's build config — `buildSrc/src/main/kotlin/RudderStackBuildConfig.kt` — not from Maven Central's `<release>`/`<latest>` pointer. The build config is the single source of truth for what this branch releases, and the publishing logic in `gradle/publishing/publishing.gradle.kts` derives every POM version from it. Reading the branch version directly makes validation deterministic and keeps pre-release/beta versions out of the comparison entirely.

The structural baseline (expected metadata, dependencies, scopes, pinned versions, third-party ranges) and the known previous release version per module live in `references/release-validation-reference.md`.

## Constants

```
BASE_URL    = https://repo1.maven.org/maven2
SDK_GROUP   = com.rudderstack.sdk.kotlin
INT_GROUP   = com.rudderstack.integration.kotlin
BUILD_CONFIG = buildSrc/src/main/kotlin/RudderStackBuildConfig.kt   (relative to repo root)
```

Modules:

| Shortname | groupId | artifactId | Branch version field in `RudderStackBuildConfig.kt` |
|---|---|---|---|
| core | `com.rudderstack.sdk.kotlin` | `core` | `SDK.Core.VERSION_NAME` |
| android | `com.rudderstack.sdk.kotlin` | `android` | `SDK.Android.VERSION_NAME` |
| adjust | `com.rudderstack.integration.kotlin` | `adjust` | `Integrations.Adjust.versionName` |
| appsflyer | `com.rudderstack.integration.kotlin` | `appsflyer` | `Integrations.AppsFlyer.versionName` |
| braze | `com.rudderstack.integration.kotlin` | `braze` | `Integrations.Braze.versionName` |
| facebook | `com.rudderstack.integration.kotlin` | `facebook` | `Integrations.Facebook.versionName` |
| firebase | `com.rudderstack.integration.kotlin` | `firebase` | `Integrations.Firebase.versionName` |
| sprig | `com.rudderstack.integration.kotlin` | `sprig` | `Integrations.Sprig.versionName` |

## The hard rule: never read versions from Maven

Do **NOT** fetch `maven-metadata.xml` to discover `<release>` or `<latest>`, and never treat any version containing a qualifier (`-beta`, `-alpha`, `-rc`, `-SNAPSHOT`, or any `-…` suffix) as a release.

The only two version sources are:

1. **This branch version** — read from `RudderStackBuildConfig.kt` (the version being validated).
2. **Known previous version** — read from the baseline reference file (the version compared against).

This is the fix for the failure mode that prompted the redesign: a pre-release (beta) had become android's Maven `<release>` pointer, which poisoned both the validated version and the integrations' expected android range. Deriving everything from the build config removes that path completely.

## Arguments

- No arguments: validate all 8 modules at their branch version (from `RudderStackBuildConfig.kt`).
- Module name(s): validate only the specified modules (e.g. `core android braze`).
- Version override: `module=version` (e.g. `android=1.7.0`) to validate a specific version instead of the branch version. The override must be an explicit, full release version — never a qualifier version.

## Workflow

### Step 1: Read this branch's versions

Read `buildSrc/src/main/kotlin/RudderStackBuildConfig.kt` from the repo root and extract each module's version:

- `core` → the `VERSION_NAME` inside `object Core`
- `android` → the `VERSION_NAME` inside `object Android`
- each integration → the `versionName` inside its `object` (e.g. `object Adjust { … versionName = "1.4.1" }`)

These are the versions to validate. If a version override argument was supplied for a module, use that instead.

### Step 2: Read the baseline

Read the reference file:

```
.claude/skills/rsa-validate-release-pom/references/release-validation-reference.md
```

It provides, per module:

- The structural expectations (metadata, licence/developer/SCM blocks, dependency identity, scopes, pinned versions, third-party ranges) used for Checks 1–8.
- The **known previous version** (the last released GA), used for the previous-vs-branch report and the version-bump sanity check.

### Step 3: Fetch and parse the branch-version POM

For each module, build the POM URL from the branch version (`groupPath` = groupId with dots replaced by slashes):

```
{BASE_URL}/{groupPath}/{artifactId}/{version}/{artifactId}-{version}.pom
```

Fetch with `curl -s`.

- If the fetch returns the POM XML → validate it (Steps 4–5).
- If the fetch returns 404 / no content → the branch version is **not published yet**. Report the module as `NOT PUBLISHED` (this is the expected state when the skill is run before the release is pushed to Maven Central — it is not a structural failure).

### Step 4: Validate against baseline

For each fetched POM, run these checks.

**Check 1: Metadata invariants**

- `groupId` matches expected
- `artifactId` matches expected
- `packaging` matches expected (`aar` for android/integrations, absent for core)
- `name` = `Analytics Kotlin SDK`
- `description` = `RudderStack's SDK for android`
- `url` = `https://github.com/rudderlabs/rudder-sdk-kotlin`

**Check 2: Licence block**

- licence name = `Elastic License 2.0 (ELv2)`
- licence url = `https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/LICENSE.md`
- licence distribution = `repo`

**Check 3: Developer block**

- developer id = `Rudderstack`
- developer name = `Rudderstack, Inc.`

**Check 4: SCM block**

- connection = `scm:git:git://github.com/rudderlabs/rudder-sdk-kotlin.git`
- developerConnection = `scm:git:git://github.com:rudderlabs/rudder-sdk-kotlin.git`
- url = `https://github.com/rudderlabs/rudder-sdk-kotlin/tree/main`

**Check 5: Dependency count**

Number of `<dependency>` elements matches the baseline count for the module:

- core: 3
- android: 4
- adjust: 4
- appsflyer: 5
- braze: 4
- facebook: 4
- firebase: 4
- sprig: 5

**Check 6: Dependency identity and scopes**

- Each expected `groupId:artifactId` pair is present
- No unexpected dependencies are present
- Each dependency's `<scope>` matches the baseline

**Check 7: Pinned dependency versions**

For dependencies marked `pinned` in the baseline, the version must match exactly:

- `kotlin-stdlib-jdk8` = `1.9.0`
- `kotlinx-serialization-json` = `1.5.1` (core only)
- `kotlinx-coroutines-core` = `1.8.0` (core only)
- `core-ktx` = `1.16.0` (android/integrations)
- `lifecycle-process` = `2.8.7` (android only)
- `fragment-ktx` = `1.8.9` (sprig only)

**Check 8: Third-party version ranges**

For dependencies marked `range - fixed` in the baseline, the version must match exactly:

- adjust-android: `[5.1.0, 6.0.0)`
- af-android-sdk: `[6.17.0, 7.0.0)`
- installreferrer: `[2.2, 3.0)`
- android-sdk-ui (braze): `[35.0.0, 36.0.0)`
- facebook-android-sdk: `[18.0.1, 19.0.0)`
- firebase-bom: `[33.7.0, 34.0.0)`
- userleap-android-sdk (sprig): `[2.23.0, 3.0.0)`

**Check 9: Internal dependency versions** (derived from the branch build config — never from Maven)

The publishing logic (`resolveDepVersion` in `gradle/publishing/publishing.gradle.kts`) pins these from `RudderStackBuildConfig.kt`. Mirror it exactly:

- **android → core**: the android POM must reference `com.rudderstack.sdk.kotlin:core` with an **exact version equal to the `core` branch version** (`SDK.Core.VERSION_NAME`). This is *not* required to equal android's own version — core and android version independently (e.g. branch may have core `1.6.0` while android is `1.7.0`, so android references core `1.6.0`).
- **integration → android**: each integration POM must reference `com.rudderstack.sdk.kotlin:android` with the **range `[{android_branch_version}, {nextMajor})`**, where:
  - lower bound = the `android` branch version (`SDK.Android.VERSION_NAME`)
  - upper bound = next major = `({major}+1).0.0` (e.g. android `1.7.0` → `[1.7.0, 2.0.0)`)
- All integration modules must reference the **same** android range.

**Check 10: Artifact completeness**

For each module, verify these four artifacts exist on Maven Central via HTTP HEAD requests (expect 200). **Mind the separators** — the main artifact and POM join the version with a dot (`.`), while the classifier artifacts join with a hyphen (`-`):

| Artifact | Filename | Separator before suffix |
|---|---|---|
| POM | `{artifactId}-{version}.pom` | `.pom` (dot) |
| Main | `{artifactId}-{version}.jar` (core) / `.aar` (android, integrations) | `.jar` / `.aar` (dot) |
| Sources | `{artifactId}-{version}-sources.jar` | `-sources.jar` (**hyphen**) |
| Javadoc | `{artifactId}-{version}-javadoc.jar` | `-javadoc.jar` (**hyphen**) |

This separator distinction caused a false-negative in a previous run: building the sources URL as `{artifactId}-{version}.sources.jar` (dot) yields a path that does not exist and returns a spurious 404. The correct filename is `{artifactId}-{version}-sources.jar` (hyphen). Always use the table above; do not interpolate the suffix with a dot for `sources`/`javadoc`.

**Check 11: GPG signatures**

For each of the four artifacts in Check 10, verify the `.asc` signature exists (HTTP 200), appending `.asc` to the exact filename from the table above:

- `{artifactId}-{version}.pom.asc`
- `{artifactId}-{version}.jar.asc` (core) / `.aar.asc` (android, integrations)
- `{artifactId}-{version}-sources.jar.asc`
- `{artifactId}-{version}-javadoc.jar.asc`

### Step 5: Report results

Lead with a previous-vs-branch summary table so the version delta is explicit:

```
| Module    | Previous (baseline) | This branch | Result        |
|-----------|---------------------|-------------|---------------|
| core      | 1.6.0               | 1.6.0       | PASS          |
| android   | 1.6.0               | 1.7.0       | PASS          |
| adjust    | 1.4.0               | 1.4.1       | NOT PUBLISHED |
| ...       | ...                 | ...         | ...           |
```

Result values:

- `PASS` — POM fetched and all checks passed.
- `FAIL` — POM fetched but one or more checks failed.
- `NOT PUBLISHED` — branch version not on Maven Central yet (expected before the release is pushed).

For any `FAIL`, list the specific check(s) that failed with details:

```
FAIL: adjust 1.4.1
  - Check 5: Dependency count mismatch (expected 4, got 5)
  - Check 9: android range lower bound is [1.6.0, 2.0.0) — expected [1.7.0, 2.0.0)
```

Also flag, as a non-failing note, any module whose branch version is **not greater** than its known previous version (a missing or accidental version bump).

### Step 6: Output POM URLs

After the summary, output a POM URLs section for every validated module (or the requested subset), in the same order as the summary table:

```
## POM URLs

| Module    | Version | POM URL |
|-----------|---------|---------|
| core      | 1.6.0   | https://repo1.maven.org/maven2/com/rudderstack/sdk/kotlin/core/1.6.0/core-1.6.0.pom |
| android   | 1.7.0   | https://repo1.maven.org/maven2/com/rudderstack/sdk/kotlin/android/1.7.0/android-1.7.0.pom |
| ...       | ...     | ... |
```

Rules:

- **Version** column: the branch version being validated.
- **POM URL** column: the full URL from Step 3. For a `NOT PUBLISHED` module, still show the URL it would live at (so the user can re-check after publishing).

### Overall verdict

- **PASS** — every fetched module passes all checks. (Modules that are `NOT PUBLISHED` do not by themselves fail the run; call them out explicitly so the state is unambiguous.)
- **FAIL** — one or more fetched modules have structural differences, or artifacts/signatures are missing for a published module.

## Updating the baseline

After a release is published and validated, the branch versions become the new "known previous" baseline. To update:

1. Refresh the **Reference POM Versions** table in `references/release-validation-reference.md` to the just-released versions (GA only — never a qualifier version).
2. Refresh the corresponding per-module POM snapshot files in `references/` (e.g. `core-{version}.md`) so they capture the new baseline POMs.
3. Update any third-party range or pinned-version expectation that changed intentionally.

If a validation fails due to an **intentional** change (a third-party dependency bump, a new pinned version, etc.), update the baseline reference rather than the POM. Always ask the user for confirmation before updating the baseline.

## Fetching approach

Use `curl -s` for all HTTP requests. Parse XML by extracting values with grep/sed or by reading the structure directly. Do NOT rely on `xmllint` or `python3` — they may not be available.

Maximise parallelism: read the build config once, then fetch all branch-version POMs in parallel, then run all artifact/signature HEAD checks in parallel.

For artifact and GPG checks, use `curl -s -o /dev/null -w "%{http_code}"` to verify HTTP 200 without downloading the artifact. When constructing those URLs, build the filename from the Check 10 table — dot before `.pom`/`.jar`/`.aar`, hyphen before `-sources.jar`/`-javadoc.jar`.
