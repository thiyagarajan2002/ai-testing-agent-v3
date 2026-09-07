package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.ai.PromptManager;
import com.thiyagarajan.agent.model.TestPlan;

import java.util.List;

public class AgentRunner {
    private final OllamaClient llm;
    private final ObjectMapper mapper;

    public AgentRunner(OllamaClient llm, ObjectMapper mapper) {
        if (mapper == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "ObjectMapper cannot be null");
        this.llm = llm;
        this.mapper = mapper;
    }

    public TestPlan plan(String requirement) throws Exception {
        if (requirement == null || requirement.isBlank()) {
            throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Requirement cannot be blank");
        }
        if (llm == null) {
            throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Ollama client is not configured");
        }
        try {
            TestPlan plan = mapper.readValue(cleanJson(llm.generate(PromptManager.planningPrompt(requirement))), TestPlan.class);
            validate(plan);
            return plan;
        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION,
                    "Unable to generate a valid test plan: " + e.getMessage(), e);
        }
    }

    /** Creates a fresh executor per test so suite parallelism has isolated runtime state. */
    public ExecutionResult execute(TestPlan plan) {
        validate(plan);
        try {
            return "UI".equalsIgnoreCase(plan.type)
                    ? new UiExecutor().execute(plan)
                    : new ApiExecutor().execute(plan);
        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            AgentExecutionException.Category category = "UI".equalsIgnoreCase(plan.type)
                    ? AgentExecutionException.Category.UI_EXECUTION
                    : AgentExecutionException.Category.API_EXECUTION;
            throw new AgentExecutionException(category, "Test execution failed: " + e.getMessage(), e);
        }
    }

    /** Backward-compatible sequential suite execution. */
    public List<ExecutionResult> executeSuite(com.thiyagarajan.agent.model.TestSuite suite, java.nio.file.Path suiteDirectory) throws Exception {
        if (suite == null || suite.plans == null || suite.plans.isEmpty())
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains no plans");
        if (suiteDirectory == null)
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite directory is required");
        java.util.ArrayList<ExecutionResult> results = new java.util.ArrayList<>();
        for (String planFile : suite.plans) {
            if (planFile == null || planFile.isBlank())
                throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains a blank plan path");
            java.nio.file.Path resolved = suiteDirectory.resolve(planFile).normalize();
            if (!resolved.startsWith(suiteDirectory.toAbsolutePath().normalize()))
                throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Plan path escapes suite directory: " + planFile);
            if (!java.nio.file.Files.isRegularFile(resolved))
                throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Plan file not found: " + resolved);
            TestPlan plan;
            try {
                plan = mapper.readValue(java.nio.file.Files.readString(resolved), TestPlan.class);
            } catch (Exception e) {
                throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid test plan JSON: " + resolved, e);
            }
            ExecutionResult result = execute(plan);
            if (!result.passed()) {
                try { analyzeFailure(plan, result); }
                catch (Exception e) { result.failureAnalysis("AI failure analysis unavailable: " + e.getMessage()); }
            }
            results.add(result);
        }
        return results;
    }

    public ExecutionResult analyzeFailure(TestPlan plan, ExecutionResult result) throws Exception {
        if (result == null) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Execution result cannot be null");
        if (llm == null) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "Ollama client is not configured");
        String safePlan = SecurityRedactor.redactText(mapper.writeValueAsString(plan));
        String safeResult = SecurityRedactor.redactText(mapper.writeValueAsString(result));
        try {
            result.failureAnalysis(llm.generate(PromptManager.failurePrompt(safePlan, safeResult)));
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION,
                    "Failure analysis generation failed: " + e.getMessage(), e);
        }
        return result;
    }

    private void validate(TestPlan plan) {
        if (plan == null) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Test plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Test plan contains no steps");
        if (!"API".equalsIgnoreCase(plan.type) && !"UI".equalsIgnoreCase(plan.type)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Unsupported plan type: " + plan.type);
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "baseUrl is required");
        for (var step : plan.steps) {
            if (step == null || step.action == null || step.action.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Every step requires an action");
            if (step.retryCount != null && step.retryCount < 0) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "retryCount cannot be negative");
            String action = step.action.trim();
            if ("API".equalsIgnoreCase(plan.type) && !action.matches("(?i)GET|POST|PUT|PATCH|DELETE")) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid API action: " + action);
            if ("UI".equalsIgnoreCase(plan.type) && !action.matches("(?i)navigate|click|fill|press|selectOption|assertVisible|assertText|assertValue|waitFor|screenshot")) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid UI action: " + action);
        }
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.AI_GENERATION, "LLM returned an empty response");
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            s = s.replaceFirst("\\s*```$", "");
        }
        int first = s.indexOf('{'), last = s.lastIndexOf('}');
        return first >= 0 && last > first ? s.substring(first, last + 1) : s;
    }
}
