package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.AiIntelligenceResult;
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
import java.util.Map;

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
        this.config = config;
        this.mapper = mapper;
        this.environments = new EnvironmentManager(mapper);
        this.profile = loadEnvironment(environment);
        this.runner = new AgentRunner(new OllamaClient(config.ollamaUrl(), config.ollamaModel()), mapper);
        try {
            this.reports = new ReportManager();
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.REPORTING, "Report manager initialization failed: " + e.getMessage(), e);
        }
    }

    private EnvironmentProfile loadEnvironment(String environment) {
        if (environment == null || environment.isBlank()) return null;
        try {
            return environments.load(environment, Path.of("."));
        } catch (AgentExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,
                    "Unable to load environment '" + environment + "': " + e.getMessage(), e);
        }
    }

    public ExecutionResult executePlan(String file) throws Exception {
        Path p = requireFile(file, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        TestPlan plan = readPlan(p);
        if (profile != null) environments.apply(plan, profile);
        ExecutionResult r = runner.execute(plan);
        analyzeIfFailed(plan, r);
        reports.writeAll(r);
        return r;
    }

    public DataDrivenExecutionResult executeDataDriven(String planFile, String dataFile) throws Exception {
        return executeDataDriven(planFile, dataFile, Map.of(), config.parallelism());
    }

    public DataDrivenExecutionResult executeDataDriven(String planFile, String dataFile, Map<String, String> filters) throws Exception {
        return executeDataDriven(planFile, dataFile, filters, config.parallelism());
    }

    public DataDrivenExecutionResult executeDataDriven(String planFile, String dataFile, Map<String, String> filters, int parallelism) throws Exception {
        Path pp = requireFile(planFile, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        Path dp = requireFile(dataFile, AgentExecutionException.Category.PLAN_VALIDATION, "Data");
        TestPlan plan = readPlan(pp);
        if (profile != null) environments.apply(plan, profile);

        DataDrivenExecutionResult result = new DataDrivenRunner(mapper, runner).execute(plan, dp, filters, parallelism);
        for (var iteration : result.iterations) {
            if (iteration.execution != null && !iteration.execution.passed()) {
                analyzeIfFailed(planForIteration(plan, iteration.data), iteration.execution);
            }
        }

        Path dir = Path.of(config.reportsDir(), "data-driven", safeName(result.testName)).toAbsolutePath().normalize();
        new DataDrivenReportManager(mapper, dir).writeAll(result);
        return result;
    }

    private TestPlan planForIteration(TestPlan template, Map<String, String> data) {
        try {
            TestPlan copy = mapper.readValue(mapper.writeValueAsString(template), TestPlan.class);
            if (data != null) substitute(copy, data);
            return copy;
        } catch (Exception e) {
            System.err.println("Unable to create data-driven plan copy for failure analysis: " + e.getMessage());
            return template;
        }
    }

    private void substitute(TestPlan p, Map<String, String> data) {
        p.name = replace(p.name, data);
        p.baseUrl = replace(p.baseUrl, data);
        if (p.variables != null) p.variables.replaceAll((k, v) -> replace(v, data));
        if (p.steps != null) for (var s : p.steps) {
            if (s == null) continue;
            s.path = replace(s.path, data);
            s.locator = replace(s.locator, data);
            s.value = replace(s.value, data);
            s.body = replace(s.body, data);
            if (s.headers != null) s.headers.replaceAll((k, v) -> replace(v, data));
            if (s.query != null) s.query.replaceAll((k, v) -> replace(v, data));
            if (s.save != null) s.save.replaceAll((k, v) -> replace(v, data));
            if (s.assertSpec != null) {
                s.assertSpec.contains = replace(s.assertSpec.contains, data);
                s.assertSpec.jsonPath = replace(s.assertSpec.jsonPath, data);
                s.assertSpec.equals = replace(s.assertSpec.equals, data);
            }
            if (s.assertions != null) for (var a : s.assertions) {
                if (a == null) continue;
                a.path = replace(a.path, data);
                a.expected = replace(a.expected, data);
            }
        }
    }

    private String replace(String value, Map<String, String> data) {
        if (value == null) return null;
        String out = value;
        if (data != null) for (var e : data.entrySet()) {
            out = out.replace("${data." + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    private String safeName(String value) {
        String s = value == null || value.isBlank() ? "data-driven-test" : value.replaceAll("[^a-zA-Z0-9._-]+", "_");
        return s.length() > 80 ? s.substring(0, 80) : s;
    }

    public TestPlanValidator.ValidationResult validatePlan(String file) throws Exception {
        Path p = requireFile(file, AgentExecutionException.Category.PLAN_VALIDATION, "Plan");
        TestPlan t = readPlan(p);
        if (profile != null) environments.apply(t, profile);
        return TestPlanValidator.validate(t);
    }

    public PreflightSuiteResult validateSuite(String file) throws Exception {
        Path sp = requireFile(file, AgentExecutionException.Category.SUITE_VALIDATION, "Suite");
        TestSuite suite;
        try {
            suite = mapper.readValue(Files.readString(sp), TestSuite.class);
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Invalid suite JSON: " + sp, e);
        }
        if (suite.plans == null || suite.plans.isEmpty())
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Suite contains no plans");

        Path suiteDir = sp.getParent() == null ? Path.of(".").toAbsolutePath().normalize() : sp.getParent().toAbsolutePath().normalize();
        Path repositoryRoot = Path.of(".").toAbsolutePath().normalize();
        List<PlanPreflight> ps = new ArrayList<>();
        for (String f : suite.plans) {
            if (f == null || f.isBlank()) {
                ps.add(new PlanPreflight(String.valueOf(f), false, List.of("Plan path is blank"), List.of()));
                continue;
            }
            Path r = suiteDir.resolve(f).normalize();
            if (!r.startsWith(repositoryRoot)) {
                ps.add(new PlanPreflight(f, false, List.of("Plan path escapes repository root"), List.of()));
                continue;
            }
            if (!Files.isRegularFile(r)) {
                ps.add(new PlanPreflight(f, false, List.of("Plan file not found: " + r), List.of()));
                continue;
            }
            try {
                TestPlan t = readPlan(r);
                if (profile != null) environments.apply(t, profile);
                var v = TestPlanValidator.validate(t);
                ps.add(new PlanPreflight(f, v.valid(), v.errors(), v.warnings()));
            } catch (AgentExecutionException e) {
                ps.add(new PlanPreflight(f, false, List.of(e.getMessage()), List.of()));
            }
        }
        return new PreflightSuiteResult(suite.name, ps);
    }

    public SuiteExecutionResult executeSuite(String file) throws Exception {
        Path sp = requireFile(file, AgentExecutionException.Category.SUITE_VALIDATION, "Suite");
        TestSuite suite;
        try {
            suite = mapper.readValue(Files.readString(sp), TestSuite.class);
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.SUITE_VALIDATION, "Invalid suite JSON: " + sp, e);
        }
        Path dir = sp.getParent() == null ? Path.of(".").toAbsolutePath() : sp.getParent().toAbsolutePath();
        SuiteExecutionResult r = new SuiteExecutionEngine(mapper, runner, config.parallelism(), environments, profile).execute(suite, dir);
        try {
            new SuiteReportManager(Path.of(config.reportsDir(), "suite").toAbsolutePath().normalize()).writeAll(r);
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.REPORTING, "Suite report generation failed: " + e.getMessage(), e);
        }
        recordHistory(r);
        return r;
    }

    public TestPlan plan(String requirement) throws Exception { return runner.plan(requirement); }
    public AiIntelligenceResult intelligence(String requirement) throws Exception { return runner.intelligence(requirement); }
    public ExecutionResult execute(TestPlan p) { return runner.execute(p); }

    public void analyzeIfFailed(TestPlan p, ExecutionResult r) {
        if (r != null && !r.passed()) {
            try { runner.analyzeFailure(p, r); }
            catch (Exception e) { r.failureAnalysis("AI failure analysis unavailable: " + e.getMessage()); }
        }
    }

    public void writeReport(ExecutionResult r) throws Exception { reports.writeAll(r); }
    public Config config() { return config; }
    public EnvironmentProfile profile() { return profile; }
    public ObjectMapper mapper() { return mapper; }

    private TestPlan readPlan(Path p) throws Exception {
        try {
            return mapper.readValue(Files.readString(p), TestPlan.class);
        } catch (Exception e) {
            throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Invalid test plan JSON: " + p, e);
        }
    }

    private void recordHistory(SuiteExecutionResult r) {
        try {
            Path h = Path.of(config.reportsDir(), "history").toAbsolutePath().normalize();
            var x = new RunHistoryManager(h).recordSuite(r);
            var a = new HistoryAnalyticsManager(h).writeReports();
            System.out.println("History run: " + x.runId());
            System.out.println("Regressions: " + x.comparison().regressions + " | Fixed: " + x.comparison().fixed);
            System.out.println("Flaky tests: " + a.flakyTests.size());
        } catch (Exception e) {
            System.err.println("History/analytics unavailable: " + e.getMessage());
        }
    }

    private Path requireFile(String f, AgentExecutionException.Category c, String k) {
        if (f == null || f.isBlank()) throw new AgentExecutionException(c, k + " file is required");
        Path p = Path.of(f).toAbsolutePath().normalize();
        if (!Files.isRegularFile(p)) throw new AgentExecutionException(c, k + " file not found: " + p);
        return p;
    }

    public record PlanPreflight(String file, boolean valid, List<String> errors, List<String> warnings) {}
    public record PreflightSuiteResult(String suiteName, List<PlanPreflight> plans) {
        public boolean valid() { return plans.stream().allMatch(PlanPreflight::valid); }
    }

    @Override public void close() {}
}
