package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes one test plan once for every data row. Rows are isolated by deep-copying the plan. */
public final class DataDrivenRunner {
    private final ObjectMapper mapper;
    private final AgentRunner runner;

    public DataDrivenRunner(ObjectMapper mapper, AgentRunner runner) {
        if (mapper == null || runner == null) throw new IllegalArgumentException("Mapper and runner are required");
        this.mapper = mapper;
        this.runner = runner;
    }

    public DataDrivenExecutionResult execute(TestPlan template, Path dataFile) throws Exception {
        if (template == null) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Test plan cannot be null");
        if (dataFile == null || !Files.isRegularFile(dataFile)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file not found: " + dataFile);
        List<Map<String, String>> rows = readRows(dataFile);
        if (rows.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file contains no rows: " + dataFile);

        DataDrivenExecutionResult aggregate = new DataDrivenExecutionResult();
        aggregate.testName = template.name;
        long started = System.nanoTime();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            TestPlan plan = mapper.readValue(mapper.writeValueAsString(template), TestPlan.class);
            substitute(plan, row);
            long iterationStart = System.nanoTime();
            ExecutionResult result;
            try {
                result = runner.execute(plan);
            } catch (Exception e) {
                result = new ExecutionResult();
                result.testName = plan.name;
                result.passed = false;
                result.failureAnalysis("Data-driven iteration " + (i + 1) + " failed before result creation: " + e.getMessage());
            }
            long duration = (System.nanoTime() - iterationStart) / 1_000_000;
            DataDrivenExecutionResult.IterationResult iteration = new DataDrivenExecutionResult.IterationResult();
            iteration.index = i + 1;
            iteration.data = new LinkedHashMap<>(row);
            iteration.passed = result.passed();
            iteration.durationMs = duration;
            iteration.execution = result;
            aggregate.iterations.add(iteration);
            aggregate.totalIterations++;
            if (iteration.passed) aggregate.passedIterations++; else aggregate.failedIterations++;
        }
        aggregate.durationMs = (System.nanoTime() - started) / 1_000_000;
        return aggregate;
    }

    private List<Map<String, String>> readRows(Path file) throws Exception {
        JsonNode root = mapper.readTree(Files.readString(file));
        JsonNode rows = root.isArray() ? root : root.get("rows");
        if (rows == null || !rows.isArray()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file must contain a JSON array or {\"rows\": [...]}");
        List<Map<String, String>> out = new ArrayList<>();
        for (JsonNode row : rows) {
            if (!row.isObject()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Every data row must be a JSON object");
            Map<String, String> values = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> it = row.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                values.put(e.getKey(), e.getValue().isNull() ? "" : e.getValue().asText());
            }
            out.add(values);
        }
        return out;
    }

    private void substitute(TestPlan plan, Map<String, String> row) {
        plan.name = replace(plan.name, row);
        plan.baseUrl = replace(plan.baseUrl, row);
        if (plan.variables != null) plan.variables.replaceAll((k, v) -> replace(v, row));
        if (plan.steps != null) for (TestStep s : plan.steps) {
            if (s == null) continue;
            s.path = replace(s.path, row); s.locator = replace(s.locator, row); s.value = replace(s.value, row); s.body = replace(s.body, row);
            if (s.headers != null) s.headers.replaceAll((k, v) -> replace(v, row));
            if (s.query != null) s.query.replaceAll((k, v) -> replace(v, row));
            if (s.save != null) s.save.replaceAll((k, v) -> replace(v, row));
            if (s.assertSpec != null) {
                s.assertSpec.contains = replace(s.assertSpec.contains, row);
                s.assertSpec.jsonPath = replace(s.assertSpec.jsonPath, row);
                s.assertSpec.equals = replace(s.assertSpec.equals, row);
            }
            if (s.assertions != null) for (var a : s.assertions) {
                if (a == null) continue;
                a.path = replace(a.path, row);
                a.expected = replace(a.expected, row);
            }
        }
    }

    private String replace(String value, Map<String, String> row) {
        if (value == null) return null;
        String out = value;
        for (Map.Entry<String, String> e : row.entrySet()) out = out.replace("${data." + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        return out;
    }
}
