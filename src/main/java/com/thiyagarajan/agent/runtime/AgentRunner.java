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
        String raw = llm.generate(PromptManager.planningPrompt(requirement));
        raw = cleanJson(raw);
        TestPlan plan = mapper.readValue(raw, TestPlan.class);
        validate(plan);
        return plan;
    }

    public ExecutionResult execute(TestPlan plan) {
        return "UI".equalsIgnoreCase(plan.type) ? ui.execute(plan) : api.execute(plan);
    }

    public ExecutionResult analyzeFailure(TestPlan plan, ExecutionResult result) throws Exception {
        String planJson = mapper.writeValueAsString(plan);
        String resultJson = mapper.writeValueAsString(result);
        result.failureAnalysis(llm.generate(
                PromptManager.failurePrompt(planJson, resultJson)));
        return result;
    }

    private void validate(TestPlan plan) {
        if (plan == null || plan.steps == null || plan.steps.isEmpty())
            throw new IllegalArgumentException("LLM returned an empty test plan");

        for (var step : plan.steps) {
            if (step.action == null) throw new IllegalArgumentException("Missing action");
            if ("API".equalsIgnoreCase(plan.type)) {
                if (!step.action.matches("(?i)GET|POST|PUT|PATCH|DELETE"))
                    throw new IllegalArgumentException("Invalid API action: " + step.action);
            } else {
                if (!step.action.matches("navigate|click|fill|press|selectOption|assertVisible|assertText|screenshot"))
                    throw new IllegalArgumentException("Invalid UI action: " + step.action);
            }
        }
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "");
            s = s.replaceFirst("\\s*```$", "");
        }
        int first = s.indexOf('{');
        int last = s.lastIndexOf('}');
        if (first >= 0 && last > first) return s.substring(first, last + 1);
        return s;
    }
}
