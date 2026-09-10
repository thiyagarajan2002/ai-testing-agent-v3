package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.thiyagarajan.agent.ai.AiIntelligenceResult;
import com.thiyagarajan.agent.ai.AiProvider;
import com.thiyagarajan.agent.ai.FailureIntelligence;
import com.thiyagarajan.agent.ai.PromptManager;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AgentRunner {
    private final AiProvider llm;
    private final ObjectMapper mapper;
    private final Config config;

    public AgentRunner(AiProvider llm, ObjectMapper mapper) { this(llm, mapper, Config.load()); }

    public AgentRunner(AiProvider llm, ObjectMapper mapper, Config config) {
        if (mapper == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "ObjectMapper cannot be null");
        this.llm = llm;
        this.mapper = mapper;
        this.config = config == null ? Config.load() : config;
    }

    public TestPlan plan(String requirement) throws Exception {
        requireRequirement(requirement); requireAi();
        try {
            TestPlan plan = mapper.readValue(cleanJson(llm.generate(PromptManager.planningPrompt(requirement))), TestPlan.class);
            validate(plan); return plan;
        } catch (AgentExecutionException e) { throw e; }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Unable to generate a valid test plan: " + e.getMessage(), e); }
    }

    /** Generates a locatorless UI plan, resolves every semantic target against live DOM, then emits Java Playwright code. */
    public String generateUiCode(String requirement, String className) throws Exception {
        TestPlan plan = plan(requirement);
        if (!"UI".equalsIgnoreCase(plan.type)) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Code generation requires a UI requirement");
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "UI code generation requires a baseUrl");

        SemanticLocatorResolver resolver = new SemanticLocatorResolver(llm, mapper);
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            page.setDefaultTimeout(config.defaultTimeoutMs());
            page.navigate(plan.baseUrl);
            for (TestStep step : plan.steps) {
                if (step == null || step.action == null) continue;
                if ("navigate".equalsIgnoreCase(step.action)) {
                    String value = step.value == null ? "" : step.value;
                    page.navigate(resolveUrl(plan.baseUrl, value));
                    continue;
                }
                if (step.target != null && !step.target.isBlank()) {
                    SemanticLocatorResolver.Resolution resolution = resolver.resolve(page, step);
                    step.locator = resolution.locator();
                }
            }
            browser.close();
        }
        return new UiCodeGenerator().generate(plan, className);
    }

    public AiIntelligenceResult intelligence(String requirement) throws Exception {
        requireRequirement(requirement); requireAi();
        try {
            AiIntelligenceResult result = mapper.readValue(cleanJson(llm.generate(PromptManager.intelligencePrompt(requirement))), AiIntelligenceResult.class);
            validateIntelligence(result); return result;
        } catch (AgentExecutionException e) { throw e; }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Unable to generate valid AI test intelligence: " + e.getMessage(), e); }
    }

    public ExecutionResult execute(TestPlan plan) {
        validate(plan);
        try { return "UI".equalsIgnoreCase(plan.type) ? new UiExecutor(config, llm, mapper).execute(plan) : new ApiExecutor().execute(plan); }
        catch (AgentExecutionException e) { throw e; }
        catch (Exception e) {
            AgentExecutionException.Category category = "UI".equalsIgnoreCase(plan.type) ? AgentExecutionException.Category.UI_EXECUTION : AgentExecutionException.Category.API_EXECUTION;
            throw new AgentExecutionException(category, "Test execution failed: " + e.getMessage(), e);
        }
    }

    public FailureIntelligence.Analysis analyzeFailureIntelligence(ExecutionResult result) { return FailureIntelligence.analyze(result); }

    public List<ExecutionResult> executeSuite(com.thiyagarajan.agent.model.TestSuite suite, java.nio.file.Path suiteDirectory) throws Exception {
        if (suite == null || suite.plans == null || suite.plans.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains no plans");
        if (suiteDirectory == null) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite directory is required");
        java.util.ArrayList<ExecutionResult> results = new java.util.ArrayList<>();
        for (String planFile : suite.plans) {
            if (planFile == null || planFile.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains a blank plan path");
            java.nio.file.Path resolved = suiteDirectory.resolve(planFile).normalize();
            if (!resolved.startsWith(suiteDirectory.toAbsolutePath().normalize())) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Plan path escapes suite directory: " + planFile);
            if (!java.nio.file.Files.isRegularFile(resolved)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Plan file not found: " + resolved);
            TestPlan plan;
            try { plan = mapper.readValue(java.nio.file.Files.readString(resolved), TestPlan.class); }
            catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid test plan JSON: " + resolved, e); }
            ExecutionResult result = execute(plan);
            if (!result.passed()) { try { analyzeFailure(plan, result); } catch (Exception e) { result.failureAnalysis("AI failure analysis unavailable: " + e.getMessage()); } }
            results.add(result);
        }
        return results;
    }

    public ExecutionResult analyzeFailure(TestPlan plan, ExecutionResult result) throws Exception {
        if (result == null) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Execution result cannot be null");
        requireAi();
        String safePlan = SecurityRedactor.redactText(mapper.writeValueAsString(plan));
        String safeResult = SecurityRedactor.redactText(mapper.writeValueAsString(result));
        try { result.failureAnalysis(llm.generate(PromptManager.failurePrompt(safePlan, safeResult))); }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Failure analysis generation failed: " + e.getMessage(), e); }
        return result;
    }

    private void validateIntelligence(AiIntelligenceResult result) {
        if (result == null || result.scenarios == null || result.scenarios.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "AI intelligence contains no scenarios");
        Set<String> ids = new HashSet<>();
        for (AiIntelligenceResult.Scenario scenario : result.scenarios) {
            if (scenario == null || scenario.id == null || scenario.id.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "AI intelligence scenario id is required");
            if (!ids.add(scenario.id)) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Duplicate AI scenario id: " + scenario.id);
            if (scenario.plan == null) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "AI scenario " + scenario.id + " has no executable plan");
            validate(scenario.plan);
        }
        if (result.coverage != null) for (AiIntelligenceResult.Coverage coverage : result.coverage) {
            if (coverage == null || coverage.requirement == null || coverage.requirement.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Coverage requirement text is required");
            if (coverage.testIds != null) for (String id : coverage.testIds) if (!ids.contains(id)) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Coverage references unknown scenario: " + id);
        }
        if (result.duplicateGroups != null) for (List<String> group : result.duplicateGroups) if (group != null) for (String id : group) if (!ids.contains(id)) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Duplicate group references unknown scenario: " + id);
    }

    private void requireRequirement(String requirement) { if (requirement == null || requirement.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Requirement cannot be blank"); }
    private void requireAi() { if (llm == null) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "AI provider is not configured"); }
    private void validate(TestPlan plan) { TestPlanValidator.requireValid(plan); }
    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "LLM returned an empty response");
        String s = raw.trim();
        if (s.startsWith("```")) { s = s.replaceFirst("^```(?:json)?\\s*", ""); s = s.replaceFirst("\\s*```$", ""); }
        int first = s.indexOf('{'), last = s.lastIndexOf('}');
        return first >= 0 && last > first ? s.substring(first, last + 1) : s;
    }
    private String resolveUrl(String base, String value) {
        if (value == null || value.isBlank()) return base;
        if (value.startsWith("http://") || value.startsWith("https://")) return value;
        return base.replaceAll("/$", "") + "/" + value.replaceFirst("^/", "");
    }
}
