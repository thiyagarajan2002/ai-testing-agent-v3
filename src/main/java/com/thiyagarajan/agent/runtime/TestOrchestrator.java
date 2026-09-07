package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.config.EnvironmentManager;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import com.thiyagarajan.agent.report.DataDrivenReportManager;
import com.thiyagarajan.agent.report.ReportManager;
import com.thiyagarajan.agent.report.SuiteReportManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Coordinates planning/execution/reporting without exposing runtime wiring to the CLI. */
public final class TestOrchestrator implements AutoCloseable {
    private final ObjectMapper mapper;
    private final Config config;
    private final EnvironmentManager environments;
    private final EnvironmentProfile profile;
    private final AgentRunner runner;
    private final ReportManager reports;

    public TestOrchestrator(Config config, ObjectMapper mapper, String environment) {
        if (config == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "Config cannot be null");
        if (mapper == null) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "ObjectMapper cannot be null");
        this.config = config; this.mapper = mapper;
        this.environments = new EnvironmentManager(mapper);
        this.profile = environment == null || environment.isBlank() ? null : environments.load(environment, Path.of("."));
        this.runner = new AgentRunner(new OllamaClient(config.ollamaUrl(), config.ollamaModel()), mapper);
        try { this.reports = new ReportManager(); } catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.REPORTING, "Report manager initialization failed: " + e.getMessage(), e); }
    }

    public ExecutionResult executePlan(String file) throws Exception {
        Path path = requireFile(file, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        TestPlan plan = readPlan(path);
        if (profile != null) environments.apply(plan, profile);
        ExecutionResult result = runner.execute(plan); analyzeIfFailed(plan, result); reports.writeAll(result); return result;
    }

    /** Executes a plan once per JSON data row and writes aggregate data-driven reports. */
    public DataDrivenExecutionResult executeDataDriven(String planFile, String dataFile) throws Exception {
        Path planPath = requireFile(planFile, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        Path dataPath = requireFile(dataFile, AgentExecutionException.Category.PLAN_VALIDATION, "Data");
        TestPlan plan = readPlan(planPath);
        if (profile != null) environments.apply(plan, profile);
        DataDrivenExecutionResult result = new DataDrivenRunner(mapper, runner).execute(plan, dataPath);
        for (DataDrivenExecutionResult.IterationResult iteration : result.iterations) {
            if (iteration.execution != null && !iteration.execution.passed()) analyzeIfFailed(planForIteration(plan, iteration.data), iteration.execution);
        }
        Path reportDir = Path.of(config.reportsDir(), "data-driven", safeName(result.testName)).toAbsolutePath().normalize();
        new DataDrivenReportManager(mapper, reportDir).writeAll(result);
        return result;
    }

    private TestPlan planForIteration(TestPlan template, java.util.Map<String, String> data) throws Exception {
        TestPlan copy = mapper.readValue(mapper.writeValueAsString(template), TestPlan.class);
        if (data == null) return copy;
        copy.name = replace(copy.name, data);
        copy.baseUrl = replace(copy.baseUrl, data);
        if (copy.variables != null) copy.variables.replaceAll((k,v) -> replace(v, data));
        if (copy.steps != null) for (var s : copy.steps) {
            if (s == null) continue;
            s.path=replace(s.path,data); s.locator=replace(s.locator,data); s.value=replace(s.value,data); s.body=replace(s.body,data);
            if (s.headers != null) s.headers.replaceAll((k,v)->replace(v,data));
            if (s.query != null) s.query.replaceAll((k,v)->replace(v,data));
            if (s.save != null) s.save.replaceAll((k,v)->replace(v,data));
            if (s.assertSpec != null) { s.assertSpec.contains=replace(s.assertSpec.contains,data); s.assertSpec.jsonPath=replace(s.assertSpec.jsonPath,data); s.assertSpec.equals=replace(s.assertSpec.equals,data); }
            if (s.assertions != null) for (var a : s.assertions) if (a != null) { a.path=replace(a.path,data); a.expected=replace(a.expected,data); }
        }
        return copy;
    }
    private String replace(String value, java.util.Map<String,String> data) { if (value == null) return null; String out=value; for (var e:data.entrySet()) out=out.replace("${data."+e.getKey()+"}", e.getValue()==null?"":e.getValue()); return out; }
    private String safeName(String value) { String s = value == null || value.isBlank() ? "data-driven-test" : value.replaceAll("[^a-zA-Z0-9._-]+", "_"); return s.length() > 80 ? s.substring(0,80) : s; }

    public TestPlanValidator.ValidationResult validatePlan(String file) throws Exception {
        Path path = requireFile(file, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        TestPlan plan = readPlan(path); if (profile != null) environments.apply(plan, profile); return TestPlanValidator.validate(plan);
    }

    public PreflightSuiteResult validateSuite(String file) throws Exception {
        Path suitePath = requireFile(file, AgentExecutionException.Category.SUITE_VALIDATION, "Suite");
        final TestSuite suite;
        try { suite = mapper.readValue(Files.readString(suitePath), TestSuite.class); }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Invalid suite JSON: " + suitePath, e); }
        if (suite.plans == null || suite.plans.isEmpty()) throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains no plans");
        Path dir = suitePath.getParent() == null ? Path.of(".").toAbsolutePath().normalize() : suitePath.getParent().toAbsolutePath().normalize();
        List<PlanPreflight> plans = new ArrayList<>();
        for (String planFile : suite.plans) {
            if (planFile == null || planFile.isBlank()) { plans.add(new PlanPreflight(String.valueOf(planFile), false, List.of("Plan path is blank"), List.of())); continue; }
            Path resolved = dir.resolve(planFile).normalize();
            if (!resolved.startsWith(dir)) { plans.add(new PlanPreflight(planFile, false, List.of("Plan path escapes suite directory"), List.of())); continue; }
            if (!Files.isRegularFile(resolved)) { plans.add(new PlanPreflight(planFile, false, List.of("Plan file not found: " + resolved), List.of())); continue; }
            try { TestPlan plan = readPlan(resolved); if (profile != null) environments.apply(plan, profile); var v = TestPlanValidator.validate(plan); plans.add(new PlanPreflight(planFile, v.valid(), v.errors(), v.warnings())); }
            catch (AgentExecutionException e) { plans.add(new PlanPreflight(planFile, false, List.of(e.getMessage()), List.of())); }
        }
        return new PreflightSuiteResult(suite.name, plans);
    }

    public SuiteExecutionResult executeSuite(String file) throws Exception {
        Path suitePath = requireFile(file, AgentExecutionException.Category.SUITE_VALIDATION, "Suite");
        final TestSuite suite;
        try { suite = mapper.readValue(Files.readString(suitePath), TestSuite.class); }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Invalid suite JSON: " + suitePath, e); }
        Path dir = suitePath.getParent() == null ? Path.of(".").toAbsolutePath() : suitePath.getParent();
        SuiteExecutionResult result = new SuiteExecutionEngine(mapper, runner, config.parallelism(), environments, profile).execute(suite, dir);
        try { new SuiteReportManager(Path.of(config.reportsDir(), "suite").toAbsolutePath().normalize()).writeAll(result); }
        catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.REPORTING, "Suite report generation failed: " + e.getMessage(), e); }
        recordHistory(result); return result;
    }

    public TestPlan plan(String requirement) throws Exception { return runner.plan(requirement); }
    public ExecutionResult execute(TestPlan plan) { return runner.execute(plan); }
    public void analyzeIfFailed(TestPlan plan, ExecutionResult result) { if (result != null && !result.passed()) try { runner.analyzeFailure(plan, result); } catch (Exception e) { result.failureAnalysis("AI failure analysis unavailable: " + e.getMessage()); } }
    public void writeReport(ExecutionResult result) throws Exception { reports.writeAll(result); }
    public Config config() { return config; }
    public EnvironmentProfile profile() { return profile; }
    public ObjectMapper mapper() { return mapper; }
    private TestPlan readPlan(Path path) throws Exception { try { return mapper.readValue(Files.readString(path), TestPlan.class); } catch (Exception e) { throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid test plan JSON: " + path, e); } }
    private void recordHistory(SuiteExecutionResult result) { try { Path history = Path.of(config.reportsDir(), "history").toAbsolutePath().normalize(); var recorded = new RunHistoryManager(history).recordSuite(result); var analytics = new HistoryAnalyticsManager(history).writeReports(); System.out.println("History run: " + recorded.runId()); System.out.println("Regressions: " + recorded.comparison().regressions + " | Fixed: " + recorded.comparison().fixed); System.out.println("Flaky tests: " + analytics.flakyTests.size()); } catch (Exception e) { System.err.println("History/analytics unavailable: " + e.getMessage()); } }
    private Path requireFile(String file, AgentExecutionException.Category category, String kind) { if (file == null || file.isBlank()) throw new AgentExecutionException(category, kind + " file is required"); Path path = Path.of(file).toAbsolutePath().normalize(); if (!Files.isRegularFile(path)) throw new AgentExecutionException(category, kind + " file not found: " + path); return path; }
    public record PlanPreflight(String file, boolean valid, List<String> errors, List<String> warnings) { }
    public record PreflightSuiteResult(String suiteName, List<PlanPreflight> plans) { public boolean valid() { return plans.stream().allMatch(PlanPreflight::valid); } }
    @Override public void close() { }
}
