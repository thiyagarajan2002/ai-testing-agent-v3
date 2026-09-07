package com.thiyagarajan.agent.runtime;

import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Reusable, non-executing validation for test plans. */
public final class TestPlanValidator {
    private TestPlanValidator() { }

    public static ValidationResult validate(TestPlan plan) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (plan == null) {
            errors.add("Test plan cannot be null");
            return new ValidationResult(errors, warnings);
        }
        if (plan.name == null || plan.name.isBlank()) warnings.add("Plan name is blank; reports may be harder to identify");
        if (plan.steps == null || plan.steps.isEmpty()) errors.add("Test plan contains no steps");
        if (!"API".equalsIgnoreCase(plan.type) && !"UI".equalsIgnoreCase(plan.type))
            errors.add("Unsupported plan type: " + plan.type);
        if (plan.baseUrl == null || plan.baseUrl.isBlank()) errors.add("baseUrl is required");

        if (plan.steps != null) {
            for (int i = 0; i < plan.steps.size(); i++) {
                TestStep step = plan.steps.get(i);
                String prefix = "Step " + (i + 1);
                if (step == null) {
                    errors.add(prefix + ": step cannot be null");
                    continue;
                }
                if (step.action == null || step.action.isBlank()) {
                    errors.add(prefix + ": action is required");
                    continue;
                }
                if (step.timeoutMs <= 0) errors.add(prefix + ": timeoutMs must be greater than zero");
                if (step.retryCount != null && step.retryCount < 0) errors.add(prefix + ": retryCount cannot be negative");

                String action = step.action.trim();
                if ("API".equalsIgnoreCase(plan.type)) {
                    if (!action.matches("(?i)GET|POST|PUT|PATCH|DELETE"))
                        errors.add(prefix + ": invalid API action: " + action);
                    if (step.path == null || step.path.isBlank()) warnings.add(prefix + ": API path is blank; base URL will be called directly");
                } else {
                    if (!action.matches("(?i)navigate|click|fill|press|selectOption|assertVisible|assertText|assertValue|waitFor|screenshot"))
                        errors.add(prefix + ": invalid UI action: " + action);
                    if (!"navigate".equalsIgnoreCase(action) && !"screenshot".equalsIgnoreCase(action)
                            && (step.locator == null || step.locator.isBlank()))
                        warnings.add(prefix + ": locator is blank for action " + action);
                }
            }
        }
        return new ValidationResult(errors, warnings);
    }

    public static void requireValid(TestPlan plan) {
        ValidationResult result = validate(plan);
        if (!result.valid()) throw new AgentExecutionException(
                AgentExecutionException.Category.PLAN_VALIDATION, result.errorsAsMessage());
    }

    public record ValidationResult(List<String> errors, List<String> warnings) {
        public ValidationResult {
            errors = List.copyOf(errors == null ? Collections.emptyList() : errors);
            warnings = List.copyOf(warnings == null ? Collections.emptyList() : warnings);
        }
        public boolean valid() { return errors.isEmpty(); }
        public String errorsAsMessage() { return String.join("; ", errors); }
    }
}
