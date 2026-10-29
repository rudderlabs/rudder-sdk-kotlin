# Gradle dependency submission

Dependabot needs resolved Gradle dependencies in GitHub's dependency graph before
it can report vulnerable Maven packages. `.github/dependabot.yml` controls update
PRs; it does not supply this inventory.

The `Gradle Dependency Submission` workflow resolves the root build with JDK 17
and the checked-in wrapper. The Gradle submission task covers all included
projects, including both sample apps and all integration modules. The workflow also
resolves `buildSrc` explicitly, including its test/runtime inputs. No project or
configuration is excluded. The strict resolution check rejects unresolved
dependencies before submission. One known exception is the local `:android`
project in `implementationDependenciesMetadata`: Kotlin metadata cannot choose
between Android debug and release variants. The Android runtime and test
classpaths are resolved separately. Other unresolved entries still fail the run.
Test and build dependencies remain in the graph with
development scope; non-test runtime classpaths have runtime scope. `buildSrc`
dependencies have development scope.

The workflow runs after dependency/build configuration changes on `main`, weekly,
and manually. Pull requests with matching changes generate an inventory artifact
with a read-only token. Only successful runs on `main` submit to GitHub, in a
separate job with `contents: write` and no repository code execution. The workflow
does not publish SDK artifacts or require sample-app credentials.

The action saves snapshots for seven days. Resolution, inventory validation, and
submission failures fail the workflow. A successful inventory check confirms
nonempty Maven data, direct/indirect relationships, and runtime/development scopes;
it is not a substitute for the coverage checks below.

## Activation prerequisite

The organization must approve this exact action before hosted validation:

```text
gradle/actions/dependency-submission@8da46b539cd8fe6ab27ca75128919a18d757604c
```

This is Gradle Actions v6.4.1. The October 29, 2026 repository policy and managed
allowlist did not include it. Request approval through the managed
[`rudder-devops` allowlist](https://github.com/rudderlabs/rudder-devops/blob/master/terraform/env/github-org/allowed-actions.yaml).
Do not enable a second automatic submission workflow for the same build context.

## Coverage acceptance

1. Run this workflow on the PR after action approval. Inspect the generated
   `dependency-graph_*` artifact and resolution log.
2. Verify resolution for `core`, `android`, `app`, `kotlin-jvm-app`, all seven
   integrations (`adjust`, `appsflyer`, `braze`, `clevertap`, `facebook`, `firebase`,
   `sprig`), and `buildSrc`. Check runtime, test, and build/plugin dependencies.
3. Compare representative resolved versions and direct/indirect relationships
   with Gradle `dependencyInsight` and `buildEnvironment` output. Preserve catalog
   strict/preferred bounds and Firebase BOM resolution. Record any omitted inputs.
4. After merge, verify the `main` submission run and the actual
   [GitHub dependency graph](https://github.com/rudderlabs/rudder-sdk-kotlin/network/dependencies).
   Record the commit, run URL, observation time, and expected Maven coordinates.
5. Review [Dependabot alerts](https://github.com/rudderlabs/rudder-sdk-kotlin/security/dependabot),
   including dismissed alerts and effective dismissal rules. An empty alert list
   is not proof of complete coverage. Detection does not guarantee an automatic
   security-fix PR for every transitive dependency.

Track implementation and acceptance in [SDK-4515](https://linear.app/rudderstack/issue/SDK-4515),
coordinated by [SDK-5554](https://linear.app/rudderstack/issue/SDK-5554) and the
[dependency configuration matrix](https://app.notion.com/p/3ecf2b415dd081b9bc8fef6efdf0f487).

## Failure and rollback

Keep failed-run evidence and treat the last submitted inventory as stale until a
new run succeeds. Revert the workflow change if it breaks dependency resolution.
Keep dependency graph, alerts, and security updates enabled. Reverting the workflow
does not remove its previously submitted snapshot or refresh that snapshot.
