Release POM Validation Reference

This document defines the expected structure and invariants for each module's release POM on Maven Central.
Use this as the baseline when validating newly published releases. Only version numbers
should change between releases - everything else must remain identical.

# Validation Rules

## What MUST stay the same across releases

- `groupId` and `artifactId`
- `packaging` type
- `name`, `description`, `url`
- `licenses` block (name, url, distribution)
- `developers` block (id, name)
- `scm` block (connection, developerConnection, url)
- Dependency list (same dependencies present, same groupIds, same artifactIds)
- Dependency scopes (compile, runtime)
- Third-party dependency version ranges (these are ranges, not pinned - they should not change unless intentionally bumped)
- Integration modules' android dependency upper bound (`2.0.0)`) — the lower bound is the `android` module's branch version from `RudderStackBuildConfig.kt` (see Check 9)

## What SHOULD change across releases

- `<version>` of the module itself
- Internal SDK dependency versions, both derived from `RudderStackBuildConfig.kt` by the publishing logic:
  - android → core: exact version equal to the `core` module's version (e.g. `1.6.0`) — this need not equal android's own version
  - integrations → android: version range `[{android_version}, 2.0.0)` where `{android_version}` is the `android` module's version (e.g. `[1.7.0, 2.0.0)`)

## What to flag as unexpected

- Any new dependency added
- Any dependency removed
- Any scope change (e.g. compile to runtime or vice versa)
- Any change to licence, developer, or SCM metadata
- Any change to third-party dependency version ranges (e.g. Braze, AppsFlyer, etc.)
- Missing `packaging` element (android and integration modules must be `aar`, core must be absent/`jar`)
- `name` or `description` changing
- Missing GPG signatures (`.asc` files)
- Missing source or javadoc JARs

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
| name | `MIT License` |
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

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `core-{version}.pom` |
| JAR | `core-{version}.jar` |
| Sources | `core-{version}-sources.jar` |
| Javadoc | `core-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `core` | _(exact = `core` module version, e.g. `1.6.0`)_ | compile | internal - exact |
| `org.jetbrains.kotlin` | `kotlin-stdlib-jdk8` | `1.9.0` | compile | pinned |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `androidx.lifecycle` | `lifecycle-process` | `2.8.7` | runtime | pinned |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `android-{version}.pom` |
| AAR | `android-{version}.aar` |
| Sources | `android-{version}-sources.jar` |
| Javadoc | `android-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `com.adjust.sdk` | `adjust-android` | `[5.1.0, 6.0.0)` | runtime | range - fixed |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `adjust-{version}.pom` |
| AAR | `adjust-{version}.aar` |
| Sources | `adjust-{version}-sources.jar` |
| Javadoc | `adjust-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.appsflyer` | `af-android-sdk` | `[6.17.0, 7.0.0)` | runtime | range - fixed |
| `com.android.installreferrer` | `installreferrer` | `[2.2, 3.0)` | runtime | range - fixed |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `appsflyer-{version}.pom` |
| AAR | `appsflyer-{version}.aar` |
| Sources | `appsflyer-{version}-sources.jar` |
| Javadoc | `appsflyer-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.braze` | `android-sdk-ui` | `[35.0.0, 36.0.0)` | runtime | range - fixed |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `braze-{version}.pom` |
| AAR | `braze-{version}.aar` |
| Sources | `braze-{version}-sources.jar` |
| Javadoc | `braze-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.facebook.android` | `facebook-android-sdk` | `[18.0.1, 19.0.0)` | runtime | range - fixed |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `facebook-{version}.pom` |
| AAR | `facebook-{version}.aar` |
| Sources | `facebook-{version}-sources.jar` |
| Javadoc | `facebook-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |
| `com.google.firebase` | `firebase-bom` | `[33.7.0, 34.0.0)` | runtime | range - fixed |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `firebase-{version}.pom` |
| AAR | `firebase-{version}.aar` |
| Sources | `firebase-{version}-sources.jar` |
| Javadoc | `firebase-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

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
| `com.rudderstack.sdk.kotlin` | `android` | `[{android_version}, 2.0.0)` | runtime | internal - range (lower = android version) |
| `com.userleap` | `userleap-android-sdk` | `[2.23.0, 3.0.0)` | runtime | range - fixed |
| `androidx.fragment` | `fragment-ktx` | `1.8.9` | runtime | pinned |
| `androidx.core` | `core-ktx` | `1.16.0` | runtime | pinned |

Artifacts:
| Type | Filename pattern |
|---|---|
| POM | `sprig-{version}.pom` |
| AAR | `sprig-{version}.aar` |
| Sources | `sprig-{version}-sources.jar` |
| Javadoc | `sprig-{version}-javadoc.jar` |
| GPG (all) | `.asc` suffix on each of the above |

---

# Reference POM Versions (baseline)

These are the **known previous** GA releases on Maven Central — the baseline this branch's release is compared against.

| Module | Known previous version (GA) | Last Updated |
|---|---|---|
| core | 1.6.0 | 07 Apr 2026 |
| android | 1.6.0 | 07 Apr 2026 |
| adjust | 1.4.0 | 07 Apr 2026 |
| appsflyer | 1.3.0 | 07 Apr 2026 |
| braze | 1.4.0 | 07 Apr 2026 |
| facebook | 1.2.1 | 07 Apr 2026 |
| firebase | 1.3.1 | 07 Apr 2026 |
| sprig | 1.0.0 | 29 Apr 2026 |

Note: The skill reads the version to validate from `RudderStackBuildConfig.kt`, never from Maven's `<release>`/`<latest>` pointer, so a pre-release (beta, alpha, rc, snapshot) can never enter the comparison. Update this table to the just-released GA versions after each release, and refresh the matching per-module POM snapshot files in this directory.

---

# Skill Validation Checklist

When validating a newly published release POM, check:

1. **Metadata invariants** - licence, developer, SCM blocks match this reference exactly
2. **Dependency count** - number of dependencies matches the baseline for that module
3. **Dependency identity** - same groupId:artifactId pairs present, no additions or removals
4. **Dependency scopes** - each dependency's scope matches the baseline
5. **Third-party version ranges** - ranges for external dependencies (Braze, AppsFlyer, etc.) are unchanged unless intentionally bumped
6. **Pinned dependency versions** - kotlin-stdlib, coroutines, core-ktx, lifecycle-process versions match baseline
7. **Internal version consistency** - android references `core` with an exact version equal to the `core` module's version from `RudderStackBuildConfig.kt` (not necessarily android's own version); integrations reference `android` with the range `[{android_version}, 2.0.0)` where `{android_version}` is the `android` module's version from the same build config; all integrations use the same range
8. **Packaging** - core has no packaging element (jar default); all others have `aar`
9. **Artifact completeness** - POM, sources jar, javadoc jar, and main artifact (jar/aar) all published
10. **GPG signatures** - `.asc` files exist for all 4 artifacts per module
