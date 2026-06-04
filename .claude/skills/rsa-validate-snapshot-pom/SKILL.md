---
name: rsa-validate-snapshot-pom
description: Validate latest snapshot POMs on Maven Central against the baseline reference. Fetches the latest snapshot for all 8 modules (core, android, adjust, appsflyer, braze, facebook, firebase, sprig), compares each against the known-good baseline, and reports pass/fail with details. Use when the user asks to validate snapshots, check snapshot POMs, verify snapshot publish, or mentions "validate pom", "check snapshot", "verify snapshot".
---

# Snapshot POM Validator

Fetches the latest snapshot POMs from Sonatype Maven Central Snapshots and validates them against the baseline reference in `pom-snapshots/snapshot-validation-reference.md`.

## Constants

```
BASE_URL = https://central.sonatype.com/repository/maven-snapshots
SDK_GROUP = com.rudderstack.sdk.kotlin
INTEGRATION_GROUP = com.rudderstack.integration.kotlin
```

Modules:
| Shortname | groupId | artifactId |
|---|---|---|
| core | `com.rudderstack.sdk.kotlin` | `core` |
| android | `com.rudderstack.sdk.kotlin` | `android` |
| adjust | `com.rudderstack.integration.kotlin` | `adjust` |
| appsflyer | `com.rudderstack.integration.kotlin` | `appsflyer` |
| braze | `com.rudderstack.integration.kotlin` | `braze` |
| facebook | `com.rudderstack.integration.kotlin` | `facebook` |
| firebase | `com.rudderstack.integration.kotlin` | `firebase` |
| sprig | `com.rudderstack.integration.kotlin` | `sprig` |

## Arguments

- No arguments: validate all 8 modules (default)
- Module name(s): validate only specified modules (e.g. `core android braze`)

## Workflow

### Step 1: Read the baseline

Read the reference file from the skill's references directory:
```
.claude/skills/rsa-validate-snapshot-pom/references/snapshot-validation-reference.md
```

This contains the expected metadata, dependencies, scopes, and version types for each module. Use this as the source of truth for all comparisons.

### Step 2: Fetch version index for each module

For each module, fetch the version list from:
```
{BASE_URL}/{groupPath}/{artifactId}/maven-metadata.xml
```

Where `groupPath` is the groupId with dots replaced by slashes.

Extract all `<version>` elements that contain `SNAPSHOT`. The **latest** is the last in the list (or use `<latest>`).

### Step 3: Fetch build metadata for each latest snapshot

For the latest snapshot version of each module, fetch:
```
{BASE_URL}/{groupPath}/{artifactId}/{version}/maven-metadata.xml
```

Extract:
- `<timestamp>` - the build timestamp (format: `yyyyMMdd.HHmmss`)
- `<buildNumber>` - the build number
- Check that all expected artifact types are present in `<snapshotVersions>`:
  - `pom`
  - `jar` (core) or `aar` (android/integrations)
  - `jar` with classifier `sources`
  - `jar` with classifier `javadoc`

### Step 4: Fetch and parse the POM

Build the POM URL:
```
{BASE_URL}/{groupPath}/{artifactId}/{version}/{artifactId}-{baseVersion}-{timestamp}-{buildNumber}.pom
```

Where `baseVersion` is the version without `-SNAPSHOT` suffix.

Fetch the POM XML via `curl -s`.

### Step 5: Validate against baseline

For each module, run these checks against the baseline reference:

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
- Number of `<dependency>` elements matches the baseline count for the module:
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
- For dependencies marked `pinned` in the baseline, the version must match exactly:
  - `kotlin-stdlib-jdk8` = `1.9.0`
  - `kotlinx-serialization-json` = `1.5.1` (core only)
  - `kotlinx-coroutines-core` = `1.8.0` (core only)
  - `core-ktx` = `1.16.0` (android/integrations)
  - `lifecycle-process` = `2.8.7` (android only)
  - `fragment-ktx` = `1.8.9` (sprig only)

**Check 8: Third-party version ranges**
- For dependencies marked `range - fixed` in the baseline, the version must match exactly:
  - adjust-android: `[5.1.0, 6.0.0)`
  - af-android-sdk: `[6.17.0, 7.0.0)`
  - installreferrer: `[2.2, 3.0)`
  - android-sdk-ui (braze): `[35.0.0, 36.0.0)`
  - facebook-android-sdk: `[18.0.1, 19.0.0)`
  - firebase-bom: `[33.7.0, 34.0.0)`
  - userleap-android-sdk (sprig): `[2.23.0, 3.0.0)`

**Check 9: Internal version consistency**
- All integration modules must reference the **same** `com.rudderstack.sdk.kotlin:android` version
- The android module must reference the **same** `com.rudderstack.sdk.kotlin:core` version
- The android dependency version in integrations must match the android module's own version
- The core dependency version in android must match the core module's own version

**Check 10: Publish date**
- The build timestamp should be within the last 24 hours (or within the expected CI window)
- All modules should have been published in the same run (timestamps within a few minutes of each other)

### Step 6: Report results

Present results as a summary table:

```
| Module     | Version           | Published           | Result |
|------------|-------------------|---------------------|--------|
| core       | 1.6.0-SNAPSHOT    | 02 Apr 2026, 10:57  | PASS   |
| android    | 1.6.0-SNAPSHOT    | 02 Apr 2026, 10:57  | PASS   |
| adjust     | 1.4.0-SNAPSHOT    | 02 Apr 2026, 10:57  | PASS   |
| ...        | ...               | ...                 | ...    |
```

Then for any FAIL, list the specific check(s) that failed with details:

```
FAIL: adjust 1.4.0-SNAPSHOT
  - Check 5: Dependency count mismatch (expected 4, got 5)
  - Check 6: Unexpected dependency com.example:new-lib:1.0.0 (scope: runtime)
```

### Step 7: Output POM URLs

After the summary table, output a **POM URLs** section listing every module with its version and the resolved POM URL. If a module's POM could not be resolved (e.g. no snapshot versions found, build metadata missing, or fetch failed), show `N/A` instead of the URL.

Format:

```
## POM URLs

| Module    | Version        | POM URL |
|-----------|----------------|---------|
| core      | 1.6.0-SNAPSHOT | https://central.sonatype.com/repository/maven-snapshots/com/rudderstack/sdk/kotlin/core/1.6.0-SNAPSHOT/core-1.6.0-20260402.122658-3.pom |
| android   | 1.6.0-SNAPSHOT | https://central.sonatype.com/repository/maven-snapshots/com/rudderstack/sdk/kotlin/android/1.6.0-SNAPSHOT/android-1.6.0-20260402.122654-3.pom |
| adjust    | 1.4.0-SNAPSHOT | https://central.sonatype.com/repository/maven-snapshots/com/rudderstack/integration/kotlin/adjust/1.4.0-SNAPSHOT/adjust-1.4.0-20260402.122657-3.pom |
| appsflyer | N/A            | N/A |
| ...       | ...            | ... |
```

Rules:
- Always output all 8 modules (or the subset requested via arguments), in the same order as the summary table.
- **Version** column: the latest snapshot version string (e.g. `1.6.0-SNAPSHOT`). Show `N/A` if no snapshot versions were found.
- **POM URL** column: the full URL constructed in Step 4. Show `N/A` if the URL could not be built (no versions, no build metadata, or fetch failure).

### Overall verdict

- **PASS** - all modules pass all checks
- **WARN** - all structural checks pass but publish date is stale
- **FAIL** - one or more modules have structural differences

## Updating the baseline

If a validation fails due to an **intentional** change (e.g. a third-party dependency version bump), update the baseline reference file at `.claude/skills/rsa-validate-snapshot-pom/references/snapshot-validation-reference.md` and the corresponding previous snapshot POM file in `.claude/skills/rsa-validate-snapshot-pom/references/`.

Always ask the user for confirmation before updating the baseline.

## Fetching approach

Use `curl -s` for all HTTP requests. Parse XML by extracting values with grep/sed patterns or by reading the XML structure directly. Do NOT rely on `xmllint` or `python3` as they may not be available.

Maximise parallelism: fetch all 8 version indexes in parallel, then all 8 build metadata in parallel, then all 8 POMs in parallel.
