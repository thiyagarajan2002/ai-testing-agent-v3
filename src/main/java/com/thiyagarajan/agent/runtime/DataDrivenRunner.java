package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executes a test plan once for every validated data row, sequentially or in parallel. */
public final class DataDrivenRunner {
    private static final int MAX_PARALLELISM = 64;
    private final ObjectMapper mapper;
    private final AgentRunner runner;
    private final DataDrivenDatasetReader datasetReader;

    public DataDrivenRunner(ObjectMapper mapper, AgentRunner runner) {
        if (mapper == null || runner == null) throw new IllegalArgumentException("Mapper and runner are required");
        this.mapper = mapper;
        this.runner = runner;
        this.datasetReader = new DataDrivenDatasetReader(mapper);
    }

    public DataDrivenExecutionResult execute(TestPlan template, Path dataFile) throws Exception {
        return execute(template, dataFile, Map.of(), 1);
    }

    public DataDrivenExecutionResult execute(TestPlan template, Path dataFile, Map<String, String> filters) throws Exception {
        return execute(template, dataFile, filters, 1);
    }

    public DataDrivenExecutionResult execute(TestPlan template, Path dataFile, Map<String, String> filters, int parallelism) throws Exception {
        validateInputs(template, dataFile, parallelism);
        List<Map<String, String>> rows = datasetReader.read(dataFile).stream().filter(row -> matches(row, filters)).toList();
        if (rows.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "No dataset rows matched the supplied filters: " + dataFile);

        DataDrivenExecutionResult aggregate = new DataDrivenExecutionResult();
        aggregate.testName = template.name;
        aggregate.parallelism = Math.min(parallelism, rows.size());
        aggregate.executionMode = aggregate.parallelism > 1 ? "PARALLEL" : "SEQUENTIAL";
        long started = System.nanoTime();

        List<DataDrivenExecutionResult.IterationResult> results = aggregate.parallelism == 1
                ? executeSequential(template, rows)
                : executeParallel(template, rows, aggregate.parallelism);

        results.sort(java.util.Comparator.comparingInt(i -> i.index));
        aggregate.iterations.addAll(results);
        aggregate.totalIterations = results.size();
        aggregate.passedIterations = (int) results.stream().filter(i -> i.passed).count();
        aggregate.failedIterations = aggregate.totalIterations - aggregate.passedIterations;
        aggregate.estimatedSequentialDurationMs = results.stream().mapToLong(i -> i.durationMs).sum();
        aggregate.durationMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
        aggregate.estimatedSpeedup = aggregate.durationMs <= 0 ? 1.0 :
                Math.max(1.0, aggregate.estimatedSequentialDurationMs * 1.0 / aggregate.durationMs);
        return aggregate;
    }

    private void validateInputs(TestPlan template, Path dataFile, int parallelism) {
        if (template == null) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Test plan cannot be null");
        if (dataFile == null || !Files.isRegularFile(dataFile)) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Data file not found: " + dataFile);
        if (parallelism < 1 || parallelism > MAX_PARALLELISM)
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "Data-driven parallelism must be between 1 and " + MAX_PARALLELISM);
    }

    private List<DataDrivenExecutionResult.IterationResult> executeSequential(TestPlan template, List<Map<String, String>> rows) throws Exception {
        List<DataDrivenExecutionResult.IterationResult> results = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) results.add(executeIteration(template, rows.get(i), i + 1));
        return results;
    }

    private List<DataDrivenExecutionResult.IterationResult> executeParallel(TestPlan template, List<Map<String, String>> rows, int parallelism) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(parallelism, r -> {
            Thread t = new Thread(r, "data-driven-worker");
            t.setDaemon(true);
            return t;
        });
        try {
            List<Future<DataDrivenExecutionResult.IterationResult>> futures = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                final int index = i + 1;
                final Map<String, String> row = new LinkedHashMap<>(rows.get(i));
                futures.add(pool.submit((Callable<DataDrivenExecutionResult.IterationResult>) () -> executeIteration(template, row, index)));
            }
            List<DataDrivenExecutionResult.IterationResult> results = new ArrayList<>();
            for (Future<DataDrivenExecutionResult.IterationResult> future : futures) {
                try { results.add(future.get()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw e; }
                catch (ExecutionException e) {
                    throw new AgentExecutionException(AgentExecutionException.Category.API_EXECUTION,
                            "Parallel data-driven worker failed: " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()), e.getCause());
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private DataDrivenExecutionResult.IterationResult executeIteration(TestPlan template, Map<String, String> row, int index) {
        long started = System.nanoTime();
        TestPlan plan = null;
        ExecutionResult result;
        try {
            plan = mapper.readValue(mapper.writeValueAsString(template), TestPlan.class);
            substitute(plan, row);
            result = runner.execute(plan);
        } catch (Exception e) {
            result = new ExecutionResult();
            result.testName = plan == null ? template.name : plan.name;
            result.passed = false;
            result.failureAnalysis("Data-driven iteration " + index + " failed before result creation: " + e.getMessage());
        }
        DataDrivenExecutionResult.IterationResult iteration = new DataDrivenExecutionResult.IterationResult();
        iteration.index = index;
        iteration.data = new LinkedHashMap<>(row);
        iteration.passed = result.passed();
        iteration.durationMs = Math.max(0, (System.nanoTime() - started) / 1_000_000);
        iteration.execution = result;
        return iteration;
    }

    private boolean matches(Map<String, String> row, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) return true;
        for (var entry : filters.entrySet()) if (!Objects.equals(row.get(entry.getKey()), entry.getValue())) return false;
        return true;
    }

    private void substitute(TestPlan plan, Map<String, String> row) {
        plan.name = replace(plan.name, row); plan.baseUrl = replace(plan.baseUrl, row);
        if (plan.variables != null) plan.variables.replaceAll((k, v) -> replace(v, row));
        if (plan.steps != null) for (TestStep s : plan.steps) {
            if (s == null) continue;
            s.path=replace(s.path,row); s.locator=replace(s.locator,row); s.value=replace(s.value,row); s.body=replace(s.body,row);
            if (s.headers != null) s.headers.replaceAll((k,v)->replace(v,row));
            if (s.query != null) s.query.replaceAll((k,v)->replace(v,row));
            if (s.save != null) s.save.replaceAll((k,v)->replace(v,row));
            if (s.assertSpec != null) { s.assertSpec.contains=replace(s.assertSpec.contains,row); s.assertSpec.jsonPath=replace(s.assertSpec.jsonPath,row); s.assertSpec.equals=replace(s.assertSpec.equals,row); }
            if (s.assertions != null) for (var a:s.assertions) if (a!=null) { a.path=replace(a.path,row); a.expected=replace(a.expected,row); }
        }
    }

    private String replace(String value, Map<String,String> row) {
        if (value == null) return null; String out=value;
        for (var e:row.entrySet()) out=out.replace("${data."+e.getKey()+"}", e.getValue()==null?"":e.getValue());
        return out;
    }
}
