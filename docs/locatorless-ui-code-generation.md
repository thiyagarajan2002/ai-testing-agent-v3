# Locatorless UI Testing and Code Generation

## Goal

UI requirements can be written as business intent without CSS, XPath, IDs, or other Playwright locators.

Example:

```text
Step 1: open the youtube.com
Step 2: search "java tutorial"
Step 3: play first video
```

The AI planner produces a UI `TestPlan` with semantic `target` values and no invented locators.

## Flow

```text
Natural-language requirement
        |
        v
AI planning
        |
        v
Locatorless TestPlan
  target = "search input"
  target = "first video"
        |
        v
Open target website with Playwright
        |
        v
Capture live DOM
        |
        v
AI SemanticLocatorResolver
        |
        v
Playwright CSS locator + confidence + reason
        |
        +--------------------+
        |                    |
        v                    v
Runtime execution      Java code generation
                             |
                             v
                    GeneratedUiTest.java
```

## Runtime

`UiExecutor` accepts both legacy explicit `locator` values and new semantic `target` values. If `target` is supplied, the configured AI provider resolves the target against the current page DOM before the action is executed.

## Code generation

The `generate` command performs:

1. AI requirement-to-plan conversion.
2. UI base URL discovery.
3. Browser startup.
4. Navigation to the target page.
5. Live DOM inspection for each semantic target.
6. AI locator resolution.
7. Deterministic Java + Playwright source generation.

The generated source contains the resolved Playwright locators because it is the final executable artifact. The original test definition does not need to contain those locators.

## CLI

```bash
mvn exec:java -Dexec.args='generate "Step 1: open the youtube.com Step 2: search java tutorial Step 3: play first video"'
```

Default output:

```text
generated/GeneratedUiTest.java
```

Custom output:

```bash
mvn exec:java -Dexec.args='generate "open youtube.com and search java tutorial" --output generated/YouTubeTest.java'
```

## Safety and determinism

The AI model is not asked to emit arbitrary Java or shell code. It produces structured test intent and locator decisions. `UiCodeGenerator` converts that structured plan into a fixed Java/Playwright template.

Locator resolution is based on live DOM evidence. If no reliable target is found, generation fails instead of inventing a selector.

## Components

- `TestStep.target`: natural-language UI intent.
- `SemanticLocatorResolver`: live DOM to Playwright locator resolution.
- `UiExecutor`: runtime semantic-target execution.
- `UiCodeGenerator`: deterministic Java/Playwright source generation.
- `AgentRunner.generateUiCode`: orchestration entry point.
- `Main generate`: command-line entry point.

## Backward compatibility

Existing plans using `locator` continue to work. New locatorless plans should leave `locator` empty and use `target` instead.
