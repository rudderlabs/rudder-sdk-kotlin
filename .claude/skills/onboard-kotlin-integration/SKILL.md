---
name: onboard-kotlin-integration
description: Generates a Kotlin integration for the rudder-sdk-kotlin repo in a step-by-step manner by referencing the corresponding Java integration. Use when the user wants to create a new Kotlin device-mode integration from an existing Java Android v1 integration. Trigger phrases - "onboard kotlin integration", "generate kotlin integration", "convert java integration to kotlin", "new kotlin integration for <name>".
argument-hint: [integration-name] [closest-example]
allowed-tools: Bash, Read, Write, Edit, Glob, Grep, AskUserQuestion
---

You are an expert Android/Kotlin developer that creates Rudder integrations by converting Java integrations to Kotlin equivalents.

Your goal is to generate a new Kotlin integration in the `rudder-sdk-kotlin` repo by analyzing the corresponding Java integration and creating the Kotlin equivalent in a step-by-step manner.

## Input

Parse the following user input for:
- **integration_name** (required): The name of the integration to generate (e.g., 'appsflyer', 'clevertap')
- **closest_example** (optional): Name of an existing Kotlin integration most similar to the one being generated (e.g., 'firebase', 'braze')

User input: $ARGUMENTS

If integration_name is not provided, use AskUserQuestion to ask the user for it before proceeding.

## Reference Material (read on demand)

- **`references/api-mapping.md`** — Java Android v1 ↔ Kotlin SDK method mapping table plus key differences. Read this before Step 1 (Java analysis) and consult it whenever you map an event method during Steps 5–7.
- **`references/integration-plugin.md`** — Signatures of the `IntegrationPlugin` abstract class and the `StandardIntegration` marker interface. Read this before Step 3 (generating the main integration class).

Use the `Read` tool to load each reference file only when the corresponding step needs it. Do not paste their contents back into your responses unless asked — refer to them by filename.

## Locating Source Material

This skill runs **from inside the `rudder-sdk-kotlin` repo**, so the Kotlin SDK is always the current working directory. You do **not** need to discover its path.

- **Kotlin SDK repo**: the current working directory.
- **Existing Kotlin integrations** (for `closest_example` reference): subdirectories under `integrations/` in the current working directory (e.g., `integrations/firebase`, `integrations/braze`, `integrations/adjust`).
- **Java integration repo** (`rudder-integration-<integration_name>-android`): ask the user where to find it.

### Locating the Java integration repo

Use `AskUserQuestion` to ask the user:

- Question: "Where is the Java integration repo `rudder-integration-<integration_name>-android` located?"
- Options:
  - **Provide local path** — "I have it cloned locally, I'll share the path"
  - **Clone from GitHub** — "Clone `https://github.com/rudderlabs/rudder-integration-<integration_name>-android` into a temp directory"
  - **Skip** — "I'll paste relevant Java snippets manually as we go"

If the user picks **Provide local path**, ask them for the absolute path (free-form input via the "Other" choice or a follow-up). Verify the path exists with `Bash`.

If the user picks **Clone from GitHub**, run:
```bash
git clone --depth=1 https://github.com/rudderlabs/rudder-integration-<integration_name>-android /tmp/rudder-integration-<integration_name>-android
```
Use the cloned directory as the Java integration source. Mention to the user that this is a throwaway clone they can delete after the skill completes.

If the user picks **Skip**, proceed without a Java repo reference and rely on snippets the user pastes inline during each step.

Store the resolved Java repo path and reuse it throughout subsequent steps.

## Process Overview

1. **Analyze Java Integration**: Examine the Java integration structure and business logic
2. **Create Module Structure**: Set up the Kotlin integration module structure
3. **Generate Core Classes**: Create the main integration class with method stubs
4. **Implement Business Logic**: Convert Java business logic to Kotlin step by step
5. **Configuration & Testing**: Add configuration files and validate the integration

## Detailed Instructions

### Step 1: Analyze Java Integration Structure

Locate the Java integration repo using the **Locating Source Material** instructions above. If `closest_example` was provided, also read the existing Kotlin integration at `integrations/<closest_example>/` in the current repo.

Analyze the Java integration:
- Examine the main integration factory class
- Identify core business logic methods and their purposes
- Note any utility classes or configuration classes
- Document the integration's key features and event mappings
- **If closest_example provided**: Also reference the existing Kotlin integration for implementation patterns, otherwise refer any other kotlin integration.

Present findings:
```markdown
## Java Integration Analysis for <integration_name>

### Key Files Found:
- Main integration class: [ClassName]
- Utility classes: [List utility classes]
- Configuration classes: [List config classes]

### Core Methods Identified:
Based on Java implementation, identify which of these methods need to be implemented:

**Event Handling Methods** (from EventPlugin interface):
- identify(): [Brief description of logic or "Not implemented"]
- track(): [Brief description of logic or "Not implemented"]
- screen(): [Brief description of logic or "Not implemented"]
- group(): [Brief description of logic or "Not implemented"]
- alias(): [Brief description of logic or "Not implemented"]

**Integration Lifecycle Methods** (from IntegrationPlugin):
- create(): [Always required - initialization logic]
- update(): [Always required - Kotlin-specific config update]
- getDestinationInstance(): [Always required - returns SDK instance]
- reset(): [Description of reset logic or "Not implemented"]
- flush(): [Description of flush logic or "Not implemented"]
- teardown(): [Override if custom cleanup needed]

### Special Features:
- [List any special event mappings, ecommerce handling, lifecycle management etc.]
- Activity lifecycle handling: [Yes/No - if yes, list methods]

### Dependencies:
- [List external SDK dependencies]

### Implementation Priority:
1. **Required**: create(), update(), getDestinationInstance(), key property
2. **Common**: identify(), track(), screen(), reset()
3. **Optional**: group(), alias(), flush(), teardown()
4. **Activity Lifecycle**: onActivityStarted(), onActivityStopped(), etc. (if needed)

### Closest Example Reference (if provided):
- Similar Kotlin integration: <closest_example>
- Key patterns to follow: [Note similar implementation approaches]
- Differences to consider: [How this integration might differ from the example]
```

**After presenting findings, get approval:**

Use `AskUserQuestion` with:
- Question: "Here's the Java integration analysis. How would you like to proceed?"
- Options:
  - **Approve** — "Analysis looks good, proceed to module structure"
  - **Request changes** — "I have corrections or additional context to share"
  - **Skip** — "Skip analysis, I'll provide context as we go"

### Step 2: Create Module Structure
Create the basic module structure in `integrations/<integration_name>/` of the current repo:

**Create directories and basic files**:
- `build.gradle.kts` - Module build configuration
- `src/main/AndroidManifest.xml` - Android manifest
- `.gitignore` - Git ignore file
- `consumer-rules.pro` - ProGuard consumer rules
- `proguard-rules.pro` - ProGuard rules

**Update the config and gradle files**:
- `RudderStackBuildConfig.kt` - add the integration object here. Refer to other existing Kotlin integrations for exact patterns.
- `settings.gradle.kts` - include the integration here. Copy the pattern from other integrations.
- `libs.versions.toml` - add the dependency version here for the destination SDK. Follow existing integration examples.
- `build.gradle.kts` - add that dependency from libs.versions.toml. Use the same structure as other integrations.
- `publishing.integration.gradle.kts` - add the integration here. Copy the pattern from other integrations.

**Important**: When creating or updating the above config files, **always refer to existing Kotlin integrations** (closest_example if provided, or firebase/braze/adjust) as they follow identical patterns.

**After creating module structure, get approval:**

Use `AskUserQuestion` with:
- Question: "Module structure and config files are ready. Review the changes?"
- Options:
  - **Approve** — "Looks good, proceed to class generation"
  - **Request changes** — "I want to adjust something before continuing"

### Step 3: Generate Main Integration Class
Create the main `<IntegrationName>Integration.kt` class and generate **only** the method stubs:

**Reference Java equivalent**: Show the main Java integration class method signatures
**Generate Kotlin class**: Create the Kotlin equivalent with proper class structure and generate required method stubs.

**After generating stubs, get approval using AskUserQuestion** — use the `preview` field to show the generated class skeleton so the user can review it inline.

### Step 4a: Implement Core Business Logic - Initialization
Convert the Java integration initialization logic to Kotlin:

**Reference Java initialization**: Show Java constructor/factory logic
**Implement Kotlin equivalent**: Create Kotlin initialization in the `create()` method

### Step 4b: Implement Update Logic
Create the `update()` method for configuration updates (this is Kotlin-specific, no Java equivalent):

**Note**: For configuration updates, don't re-initialize the integration, just update the config. This is different from Java and has no equivalent there. Refer to other Kotlin integrations for examples.

**After implementing create() and update(), get approval using AskUserQuestion.**

### Step 5: Implement Core Business Logic - Identify Method
Convert the Java identify method to Kotlin.

**After implementing, get approval using AskUserQuestion.**

### Step 6: Implement Core Business Logic - Track Method
Convert the Java track method to Kotlin with event mappings.

**After implementing, get approval using AskUserQuestion.**

### Step 7: Implement Core Business Logic - Screen Method
Convert the Java screen method to Kotlin.

**After implementing, get approval using AskUserQuestion.**

**Additional Event Methods** (implement if found in Java integration):
- **Step 7b**: group() method - for group events
- **Step 7c**: alias() method - for alias events
- **Step 7d**: flush() method - for manual flushing
- **Step 7e**: reset() method - for user logout/reset
- **Step 7f**: teardown() method - for custom cleanup (rarely needed)

**Activity Lifecycle Methods** (implement if found in Java integration):
- **Step 7a**: ActivityLifecycleObserver methods

**Each sub-step requires approval via AskUserQuestion before moving to the next.**

### Step 8: Implement Utility Classes and Configuration (If Applicable)
Convert any Java utility classes and configuration classes.

**After implementing, get approval using AskUserQuestion.**

### Step 9: Add Additional Dependencies (Optional)
If additional dependencies are discovered during implementation that weren't added in Step 2.

### Step 10: Build and Test Integration

#### Step 10a: Build and Code Quality Validation
```bash
./gradlew :integrations:<integration_name>:build
./gradlew :integrations:<integration_name>:detekt
```

If build/detekt reveals issues, use `AskUserQuestion` to present the errors and ask:
- **Auto-fix** — "Let me fix these issues automatically"
- **Manual fix** — "I'll fix these myself, show me the errors"
- **Skip** — "Ignore for now, continue to next step"

#### Step 10b: Write and Run Unit Tests
Create comprehensive unit tests and run them:
```bash
./gradlew :integrations:<integration_name>:test
```

### Step 11: Create Documentation
Generate README.md for the integration. Refer to other integrations for examples.

## Step Execution Protocol

### Approval Mechanism

**CRITICAL**: Use `AskUserQuestion` to gate every step. Do NOT proceed to the next step without explicit user approval.

Standard approval pattern for each step:
```
AskUserQuestion:
  Question: "Step N complete — <brief summary>. How would you like to proceed?"
  Options:
    - "Approve" — proceed to next step
    - "Request changes" — user provides feedback, revise this step
    - "Skip this step" — move to next step without this one
```

When presenting generated code for approval, use the `preview` field on AskUserQuestion options to show the code inline, so the user can review without reading files separately.

### User Modification Handling
- **Acknowledge user changes**: If user modifies generated code, understand and analyze the changes
- **Adapt remaining steps**: Update subsequent steps based on user modifications and learnings
- **Ask for clarification**: If user changes affect the approach, confirm the new direction
- **Maintain consistency**: Ensure later steps align with user's modifications and preferences

### Method Implementation Guidelines
- **Required methods**: Always implement create(), update(), getDestinationInstance(), and key property
- **Event methods**: Only implement event methods that exist in Java integration
- **Lifecycle methods**: Only implement flush(), reset(), teardown() if they have logic in Java version
- **Activity lifecycle**: Only implement ActivityLifecycleObserver if Java integration has activity handling
- **Skip empty methods**: Don't implement methods that have no logic in Java integration

### Java-to-Kotlin Conversion
- **Preserve business logic** exactly as in Java version
- **Use Kotlin idioms** (when expressions, extension functions, null safety)
- **Config access**: Use `JsonObject` APIs — `jsonObject["key"]?.jsonPrimitive?.content`, `jsonObject["key"]?.jsonPrimitive?.boolean`, etc.
- **Reference Java methods** when explaining implementation
- **Maintain compatibility** with RudderStack event structure

### Communication Style
- **Be concise**: Provide clear, focused explanations without unnecessary verbosity
- **Use bullet points**: Summarize key points and changes efficiently
- **Highlight essentials**: Focus on critical business logic and important decisions

### Error Handling
- If the Java integration repo cannot be located (path invalid or clone fails), ask the user to verify the integration name or provide an alternate path/URL via AskUserQuestion.
- If business logic is unclear, ask for clarification via AskUserQuestion
- If dependencies are missing, suggest adding them

### Final Summary
After all steps completed:
```markdown
## Kotlin Integration Generation Complete

### Original Java Integration:
`rudder-integration-<integration_name>-android`

### Generated Kotlin Integration:
`integrations/<integration_name>/`

### Key Business Logic Converted:
- [List main methods and their Java counterparts]

### Files Created:
- [List all generated files]

### Test Results:
- Unit tests: [PASSED/FAILED status]
- Detekt analysis: [PASSED/FAILED status]
- Build compilation: [PASSED/FAILED status]
```
