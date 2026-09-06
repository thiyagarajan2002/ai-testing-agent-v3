package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.ai.PromptManager;
import com.thiyagarajan.agent.model.TestPlan;

public class AgentRunner {
    private final OllamaClient llm;
    private final ObjectMapper mapper;
    private final ApiExecutor api = new ApiExecutor();
    private final UiExecutor ui = new UiExecutor();

    public AgentRunner(OllamaClient llm, ObjectMapper mapper) {
        this.llm = llm;
        this.mapper = mapper;
    }

    public TestPlan plan(String requirement) throws Exception {
        if (requirement == null || requirement.isBlank()) throw new IllegalArgumentException("Requirement cannot be blank");
        TestPlan plan = mapper.readValue(cleanJson(llm.generate(PromptManager.planningPrompt(requirement))), TestPlan.class);
        validate(plan);
        return plan;
    }

    public ExecutionResult execute(TestPlan plan) {
        validate(plan);
        return "UI".equalsIgnoreCase(plan.type) ? ui.execute(plan) : api.execute(plan);
    }

    public ExecutionResult analyzeFailure(TestPlan plan, ExecutionResult result) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        result.failureAnalysis(llm.generate(PromptManager.failurePrompt(mapper.writeValueAsString(plan), mapper.writeValueAsString(result))));
        return result;
    }

    private void validate(TestPlan plan) {
        if (plan == null) throw new IllegalArgumentException("Test plan cannot be null");
        if (plan.steps == null || plan.steps.isEmpty()) throw new IllegalArgumentException("Test plan contains no steps");
        if (!"API".equalsIgnoreCase(plan.type) && !"UI".equalsIgnoreCase(plan.type)) throw new IllegalArgumentException("Unsupported plan type: " + plan.type);
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl is required");
        for (var step : plan.steps) {
            if (step == null || step.action == null || step.action.isBlank()) throw new IllegalArgumentException("Every step requires an action");
            String action = step.action.trim();
            if ("API".equalsIgnoreCase(plan.type) && !action.matches("(?i)GET|POST|PUT|PATCH|DELETE")) throw new IllegalArgumentException("Invalid API action: " + action);
            if ("UI".equalsIgnoreCase(plan.type) && !action.matches("(?i)navigate|click|fill|press|selectOption|assertVisible|assertText|assertValue|waitFor|screenshot")) throw new IllegalArgumentException("Invalid UI action: " + action);
        }
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("LLM returned an empty response");
        String s = raw.trim();
        if (s.startsWith("```")) { s = s.replaceFirst("^```(?:json)?\\s*", ""); s = s.replaceFirst("\\s*```$", ""); }
        int first = s.indexOf('{'), last = s.lastIndexOf('}');
        return first >= 0 && last > first ? s.substring(first, last + 1) : s;
    }
}
