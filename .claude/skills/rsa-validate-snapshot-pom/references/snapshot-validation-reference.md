Snapshot POM Validation Reference

This document defines the expected structure and invariants for each module's snapshot POM.
Use this as the baseline when validating newly published snapshots. Only version numbers
should change between releases - everything else must remain identical.

# Validation Rules

## What MUST stay the same across snapshots

- `groupId` and `artifactId`
- `packaging` type
- `name`, `description`, `url`
- `licenses` block (name, url, distribution)
- `developers` block (id, name)
- `scm` block (connection, developerConnection, url)
- Dependency list (same dependencies present, same groupIds, same artifactIds)
- Dependency scopes (compile, runtime)
- Third-party dependency version ranges (these are ranges, not pinned - they should not change unless intentionally bumped)

## What SHOULD change across snapshots

- `<version>` of the module itself
- Internal SDK dependency versions (`com.rudderstack.sdk.kotlin:core`, `com.rudderstack.sdk.kotlin:android`) - these track the release version

## What to flag as unexpected

- Any new dependency added
- Any dependency removed
- Any scope change (e.g. compile to runtime or vice versa)
- Any change to licence, developer, or SCM metadata
- Any change to third-party dependency version ranges (e.g. Braze, AppsFlyer, etc.)
- Missing `packaging` element (android and integration modules must be `aar`, core must be absent/`jar`)
- `name` or `description` changing

---

# Module Baselines

## core

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.sdk.kotlin` |
| artifactId | `core` |
| packaging | _(absent, defaults to jar)_ |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence:
| Field | Value |
|---|---|
| name | `Elastic License 2.0 (ELv2)` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin/blob/main/LICENSE.md` |
| distribution | `repo` |

Developer:
| Field | Value |
|---|---|
| id | `Rudderstack` |
| name | `Rudderstack, Inc.` |

SCM:
| Field | Value |
|---|---|
| connection | `scm:git:git://github.com/rudderlabs/rudder-sdk-kotlin.git` |
| developerConnection | `scm:git:git://github.com:rudderlabs/rudder-sdk-kotlin.git` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin/tree/main` |

Dependencies (3 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlinx` | `kotlinx-serialization-json` | `1.5.1` | compile | pinned |
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `org.jetbrains.kotlinx` | `kotlinx-coroutines-core` | `1.8.0` | runtime | pinned |

---

## android

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.sdk.kotlin` |
| artifactId | `android` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (4 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `com.rudderstack.sdk.kotlin` | `core` | _(tracks release)_ | compile | internal - variable |
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `androidx.lifecycle` | `lifecycle-process` | `2.8.7` | runtime | pinned |

---

## adjust

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `adjust` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (4 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `com.adjust.sdk` | `adjust-android` | `[5.1.0, 6.0.0)` | runtime | range - fixed |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |

---

## appsflyer

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `appsflyer` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (5 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.appsflyer` | `af-android-sdk` | `[6.17.0, 7.0.0)` | runtime | range - fixed |
| `com.android.installreferrer` | `installreferrer` | `[2.2, 3.0)` | runtime | range - fixed |

---

## braze

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `braze` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (4 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.braze` | `android-sdk-ui` | `[35.0.0, 36.0.0)` | runtime | range - fixed |

---

## facebook

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `facebook` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (4 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.facebook.android` | `facebook-android-sdk` | `[18.0.1, 19.0.0)` | runtime | range - fixed |

---

## firebase

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `firebase` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (4 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.google.firebase` | `firebase-bom` | `[33.7.0, 34.0.0)` | runtime | range - fixed |

---

## sprig

| Field | Expected Value |
|---|---|
| groupId | `com.rudderstack.integration.kotlin` |
| artifactId | `sprig` |
| packaging | `aar` |
| name | `Analytics Kotlin SDK` |
| description | `RudderStack's SDK for android` |
| url | `https://github.com/rudderlabs/rudder-sdk-kotlin` |

Licence, Developer, SCM: _Same as core (see above)_

Dependencies (5 total):
| groupId | artifactId | Version | Scope | Version type |
|---|---|---|---|---|
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `com.rudderstack.sdk.kotlin` | `android` | _(tracks release)_ | runtime | internal - variable |
| `com.userleap` | `userleap-android-sdk` | `[2.23.0, 3.0.0)` | runtime | range - fixed |
| `androidx.fragment` | `fragment-ktx` | `1.8.9` | runtime | pinned |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |

---

# Comparison: Previous vs Latest Snapshot

Data sourced from previous (16 Mar 2026) and latest (02 Apr 2026) snapshot POMs.

## Version Changes

| Module | Previous Snapshot | Latest Snapshot | Internal SDK Dep Version Change |
|---|---|---|---|
| core | 1.5.0-SNAPSHOT | 1.6.0-SNAPSHOT | _(no internal deps)_ |
| android | 1.5.0-SNAPSHOT | 1.6.0-SNAPSHOT | core: 1.5.0 → 1.6.0 |
| adjust | 1.3.0-SNAPSHOT | 1.4.0-SNAPSHOT | android: 1.5.0 → 1.6.0 |
| appsflyer | 1.2.0-SNAPSHOT | 1.3.0-SNAPSHOT | android: 1.5.0 → 1.6.0 |
| braze | 1.3.0-SNAPSHOT | 1.4.0-SNAPSHOT | android: 1.5.0 → 1.6.0 |
| facebook | 1.2.0-SNAPSHOT | 1.2.1-SNAPSHOT | android: 1.5.0 → 1.6.0 |
| firebase | 1.3.0-SNAPSHOT | 1.3.1-SNAPSHOT | android: 1.5.0 → 1.6.0 |

## Non-Version Changes

**None found.** Across all 7 modules, the only differences between previous and latest snapshots were:
- The module's own version number
- Internal SDK dependency versions (core, android) tracking the new release

All metadata (licence, developer, SCM), dependency lists, dependency scopes, third-party version ranges, packaging types, and POM structure remained identical.

## Result: PASS

The snapshot publishing pipeline is producing correct, consistent POMs. Only version numbers changed as expected.

---

# Skill Validation Checklist

When a future skill validates a newly published snapshot POM, it should check:

1. **Metadata invariants** - licence, developer, SCM blocks match this reference exactly
2. **Dependency count** - number of dependencies matches the baseline for that module
3. **Dependency identity** - same groupId:artifactId pairs present, no additions or removals
4. **Dependency scopes** - each dependency's scope matches the baseline
5. **Third-party version ranges** - ranges for external dependencies (Braze, AppsFlyer, etc.) are unchanged unless intentionally bumped
6. **Pinned dependency versions** - kotlin-stdlib, coroutines, core-ktx, lifecycle-process versions match baseline
7. **Internal version consistency** - integration modules reference the same android SNAPSHOT version; android references the same core SNAPSHOT version
8. **Packaging** - core has no packaging element (jar default); all others have `aar`
9. **Publish date** - timestamp should be recent (within expected CI window)
10. **Artifacts present** - POM, sources jar, javadoc jar, and main artifact (jar/aar) all published
