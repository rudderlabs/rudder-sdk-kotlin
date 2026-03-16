# Changelog

All notable changes to this project will be documented in this file.

# [1.5.0](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.4.0...com.rudderstack.sdk.kotlin.core@1.5.0) (2026-03-16)

## Features

- Add per-module CHANGELOG.md files ([#291](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/291))
- Implement instance based logging ([#276](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/276))
- Use local project modules with unified publishing script and Gradle build cache ([#272](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/272))

## Refactors

- Remove open modifier from storageType param ([#289](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/289))

# [1.4.0](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.3.0...com.rudderstack.sdk.kotlin.core@1.4.0) (2026-03-03)

## Features

- Make storage dependencies injectable with expanded tests ([#265](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/265))

# [1.3.0](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.2.0...com.rudderstack.sdk.kotlin.core@1.3.0) (2026-02-17)

## Features

- Add retry headers with persisted state for improved reliability of event delivery ([#260](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/260))
- Add timeout error type and SSL exception handling ([#259](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/259))

# [1.2.0](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.1.2...com.rudderstack.sdk.kotlin.core@1.2.0) (2026-02-03)

## ⚠ Breaking Changes

- Default storage type changed from FILE to IN_MEMORY ([#253](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/253))

## Features

- Add configurable in-memory storage implementation ([#253](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/253))
- Refactor error handling nomenclature for retryable and non-retryable errors ([#249](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/249))

## Bug Fixes

- Correct PropertiesFile.clear(key) to remove only specified key ([#254](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/254))
- Fix event ordering issue in server-side environment ([#252](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/252))

# [1.1.2](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.1.1...com.rudderstack.sdk.kotlin.core@1.1.2) (2026-01-06)

## Bug Fixes

- Restrict source config to mobile platforms only ([#235](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/235))

# [1.1.1](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/com.rudderstack.sdk.kotlin.core@1.1.0...com.rudderstack.sdk.kotlin.core@1.1.1) (2025-12-23)

## Bug Fixes

- Fix last activity time greater than current monotonic time in session issue ([#237](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/237))

# [1.1.0](https://github.com/rudderlabs/rudder-sdk-kotlin/compare/v1.0.0...com.rudderstack.sdk.kotlin.core@1.1.0) (2025-09-29)

## Features

- Add new reset API overload ([#215](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/215))
- Add writeKey in query params for sourceConfig request ([#219](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/219))
- Update library package name in context.library.name in event payload ([#221](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/221))

## Bug Fixes

- Fix LoggerAnalytics error method ([#220](https://github.com/rudderlabs/rudder-sdk-kotlin/pull/220))

# [1.0.0](https://github.com/rudderlabs/rudder-sdk-kotlin/releases/tag/v1.0.0) (2025-05-14)

- Initial stable release
