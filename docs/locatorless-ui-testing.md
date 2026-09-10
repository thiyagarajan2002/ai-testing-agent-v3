# Locatorless UI Testing

## Purpose

The AI Testing Agent can accept a UI requirement without Playwright locators. The requirement describes user intent, while the framework discovers concrete selectors from the live page at runtime.

Example:

```text
Step 1: open the youtube.com
Step 2: search "java tutorial"
Step 3: play first video
```

The source requirement does not need `#id`, CSS, XPath, or other selectors.

## Architecture

```text
Natural-language requirement
        |
        v
AI planning
        |
        v
Locatorless TestPlan
(target = "search input", locator = "")
        |
        v
Launch Playwright + navigate
        |
        v
Capture live DOM + visible text
        |
        v
AI semantic locator resolver
        |
        v
Candidate selector validation
        |
        v
Concrete locator
        |
        v
Execute action
        |
        v
Page state changes
        |
        +----> resolve next target from the new DOM
        |
        v
UiCodeGenerator
        |
        v
Java + Playwright test with concrete locators
```

## Planning rules

For UI steps, the planner should use `target` for natural-language intent and leave `locator` empty unless the user explicitly supplied a locator. The planner must not invent selectors during planning.

Examples of valid targets:

- `search input`
- `Login button`
- `first video`
- `Submit button next to the registration form`

For a search workflow, the plan should normally separate entering the search value from submitting it. For example:

1. `fill` target `search input`, value `java tutorial`
2. `press` target `search input`, value `Enter`
3. `click` target `first video`

## Runtime locator resolution

`SemanticLocatorResolver` sends live DOM evidence to the configured AI provider. The context contains the current page HTML and visible body text, with size limits applied. Previous semantic steps are also included so the model can understand the current workflow.

The resolver requests JSON containing:

```json
{
  "locator": "CSS selector",
  "confidence": 0.95,
  "reason": "why the selector matches the requested target",
  "alternatives": ["alternative CSS selector"]
}
```

The framework then validates every returned selector against the live DOM before accepting it. Selectors that do not match a visible element are rejected.

## Action-aware resolution

Resolution is based on the requested action. A target used for `fill` must resolve to an input-like element, while `click` and ordinal targets such as `first video` require a clickable matching element. The resolver is instructed not to invent attributes or elements that are absent from the evidence.

## Code generation

After successful runtime resolution, `UiCodeGenerator` receives the plan containing the resolved concrete locators. The generated Java source contains normal Playwright calls such as:

```java
page.locator("input[name='search_query']").fill("java tutorial");
page.locator("input[name='search_query']").press("Enter");
page.locator("ytd-video-renderer a#video-title").click();
```

The generated code is therefore executable Java/Playwright source, while the original requirement remains locatorless.

## CLI

Generate a Java UI test with:

```text
mvn exec:java -Dexec.args="generate \"Step 1: open https://example.com Step 2: click Login button\" --output generated/GeneratedUiTest.java"
```

Default output:

```text
generated/GeneratedUiTest.java
```

The `generate` command plans the requirement, launches a live Chromium session, resolves semantic targets, executes state-changing UI actions, and finally writes the generated Java source.

## Execution versus generation

Normal UI execution can also consume a locatorless plan. `UiExecutor` resolves a `target` immediately before executing the step. This allows later steps to be resolved against the updated DOM instead of relying on selectors discovered before navigation or interaction.

Explicit `locator` values remain supported for backward compatibility.

## Failure and self-healing

If a step fails, the existing failure classification and self-healing pipeline remains available. Self-healing is conservative and confidence-gated. Healing evidence is recorded in execution details and healing history.

## Security

DOM and visible-text context is passed through the project's redaction mechanism before being included in AI prompts. Context is capped to prevent unbounded prompt growth. AI-generated selectors are validated against the actual page rather than trusted blindly.

## Limitations

- The target still needs to describe the intended element clearly.
- Dynamic pages can change between locator resolution and action execution.
- AI confidence is not a proof of correctness; live DOM validation is required.
- CAPTCHA, authentication challenges, shadow DOM edge cases, and highly visual interactions may require additional browser-specific handling.
- Generated source contains concrete locators because executable Playwright code ultimately needs a target strategy.

## Verification checklist

1. Requirement contains no locator.
2. AI plan contains `target` and an empty `locator`.
3. Browser reaches the requested base URL.
4. Resolver receives current DOM evidence.
5. Returned selector matches the live DOM.
6. Action succeeds.
7. Next target is resolved after the page state changes.
8. Generated Java source contains the resolved selectors.
9. Original semantic intent is not emitted as a CSS selector.
