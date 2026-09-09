package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.io.RunLogManager;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.runtime.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Main {
    private static final String VERSION = "3.31.0";
    private Main() {}

    public static void main(String[] a) {
        String[] args = a == null ? new String[0] : a; int exitCode = 0;
        try {
            Config config = Config.load(); String area = logArea(args); String command = args.length == 0 ? "help" : args[0];
            try (RunContext context = RunContext.start(); RunLogManager logs = RunLogManager.start(config.reportsDir(), area, command, context.runId())) {
                logs.log("Run ID: " + context.runId()); logs.log("Command: " + String.join(" ", args)); logs.log("Terminal output is captured to: " + logs.logFile());
                try { exitCode = run(args); }
                catch (AgentExecutionException e) { logs.log("Agent error [" + e.category() + "]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                catch (Exception e) { logs.log("Agent error [INFRASTRUCTURE]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                logs.log("Exit code: " + exitCode);
            }
        } catch (AgentExecutionException e) { System.err.println("Agent error [" + e.category() + "]: " + e.getMessage()); exitCode = 2; }
        catch (Exception e) { System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
        if (exitCode != 0) System.exit(exitCode);
    }

    static int run(String[] a) throws Exception {
        Config c = Config.load(); ObjectMapper m = new ObjectMapper(); String env = option(a, "--env");
        if (a.length > 0 && "history".equalsIgnoreCase(a[0])) return history(c, a);
        if (a.length >= 2 && "plan".equalsIgnoreCase(a[0])) { try (TestOrchestrator o = new TestOrchestrator(c, m, env)) { var r = o.executePlan(a[1]); return r.passed() ? 0 : 1; } }
        if (a.length >= 2 && "suite".equalsIgnoreCase(a[0])) { try (TestOrchestrator o = new TestOrchestrator(c, m, env)) { var r = o.executeSuite(a[1]); return r.passed() ? 0 : 1; } }
        if (a.length >= 3 && "data-driven".equalsIgnoreCase(a[0])) { try (TestOrchestrator o = new TestOrchestrator(c, m, env)) { int p = parallelism(a, c.parallelism()); var r = o.executeDataDriven(a[1], a[2], filters(a), p); System.out.println("Data-driven: " + r.testName + " | Iterations: " + r.totalIterations + " | Passed: " + r.passedIterations + " | Failed: " + r.failedIterations); System.out.println("Mode: " + r.executionMode + " | Workers: " + r.parallelism + " | Duration: " + r.durationMs + " ms | Estimated speedup: " + String.format(Locale.ROOT, "%.2fx", r.estimatedSpeedup)); System.out.println("Reports: ./reports/data-driven/" + safe(r.testName) + "/"); return r.passed() ? 0 : 1; } }
        if (a.length >= 2 && "validate".equalsIgnoreCase(a[0])) { try (TestOrchestrator o = new TestOrchestrator(c, m, env)) { if ("plan".equalsIgnoreCase(a[1]) && a.length >= 3) return o.validatePlan(a[2]).valid() ? 0 : 1; if ("suite".equalsIgnoreCase(a[1]) && a.length >= 3) return o.validateSuite(a[2]).valid() ? 0 : 1; return 2; } }
        if (a.length > 0 && "interactive".equalsIgnoreCase(a[0])) { interactive(c, m, env); return 0; }
        usage(); return a.length == 0 ? 0 : 2;
    }

    private static int history(Config c, String[] a) throws Exception {
        Path root = Path.of(c.reportsDir(), "history").toAbsolutePath().normalize(); int limit = optionInt(a, "--limit", 10); Path indexFile = root.resolve("index.json");
        if (!Files.isRegularFile(indexFile)) { System.out.println("No execution history found."); System.out.println("History directory: " + root); return 0; }
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        List<RunHistoryManager.RunSummary> all = mapper.readValue(indexFile.toFile(), new com.fasterxml.jackson.core.type.TypeReference<List<RunHistoryManager.RunSummary>>() {});
        System.out.println("=== Execution History ===");
        all.stream().skip(Math.max(0, all.size() - limit)).forEach(r -> System.out.println(r.runId + " | " + r.suiteName + " | " + r.status + " | " + r.passRate + "% | " + r.durationMs + " ms | regressions=" + r.regressions + " | fixed=" + r.fixed));
        int analysisLimit = Math.max(limit, 1); var analytics = new HistoryAnalyticsManager(root).analyze(analysisLimit, HistoryAnalyticsManager.DEFAULT_FLAKY_THRESHOLD);
        var trends = new TrendAnalyticsManager(root).writeReports(analysisLimit);
        System.out.println("Runs analyzed: " + analytics.runsAnalyzed + " | Tests observed: " + analytics.totalTestsObserved + " | Flaky: " + analytics.flakyTests.size());
        System.out.println("Trend: pass-rate delta=" + String.format(Locale.ROOT, "%.2f pp", trends.passRateDelta) + " | duration delta=" + trends.durationDeltaMs + " ms | regressions=" + trends.regressionTotal);
        System.out.println("History dashboard: " + root.resolve("index.html")); System.out.println("Analytics dashboard: " + root.resolve("analytics.html")); System.out.println("Trend dashboard: " + root.resolve("trends.html"));
        return 0;
    }

    private static int optionInt(String[] a, String name, int fallback) { String value = option(a, name); if (value == null) return fallback; try { int n = Integer.parseInt(value); if (n < 1 || n > 1000) throw new IllegalArgumentException(name + " must be between 1 and 1000"); return n; } catch (NumberFormatException e) { throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, name + " must be an integer: " + value, e); } }
    private static String logArea(String[] a) { if (a.length == 0) return "terminal"; if ("interactive".equalsIgnoreCase(a[0])) return "ui"; if ("history".equalsIgnoreCase(a[0])) return "history"; if ("plan".equalsIgnoreCase(a[0]) || "suite".equalsIgnoreCase(a[0]) || "data-driven".equalsIgnoreCase(a[0]) || ("validate".equalsIgnoreCase(a[0]) && a.length > 1 && "plan".equalsIgnoreCase(a[1]))) return "api"; return "terminal"; }
    private static Map<String, String> filters(String[] a) { Map<String, String> f = new LinkedHashMap<>(); for (int i = 0; i < a.length - 1; i++) if ("--filter".equalsIgnoreCase(a[i])) { String[] p = a[i + 1].split("=", 2); if (p.length != 2 || p[0].isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Filter must use key=value"); f.put(p[0], p[1]); } return f; }
    private static int parallelism(String[] a, int fallback) { for (int i = 0; i < a.length - 1; i++) if ("--parallelism".equalsIgnoreCase(a[i])) { try { int value = Integer.parseInt(a[i + 1]); if (value < 1 || value > Config.MAX_PARALLELISM) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "--parallelism must be between 1 and " + Config.MAX_PARALLELISM); return value; } catch (NumberFormatException e) { throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, "--parallelism must be an integer: " + a[i + 1], e); } } return fallback; }
    private static String option(String[] a, String n) { for (int i = 0; i < a.length - 1; i++) if (n.equalsIgnoreCase(a[i])) return a[i + 1].startsWith("--") ? null : a[i + 1]; return null; }
    private static String safe(String v) { String s = v == null || v.isBlank() ? "data-driven-test" : v.replaceAll("[^a-zA-Z0-9._-]+", "_"); return s.length() > 80 ? s.substring(0, 80) : s; }
    private static void interactive(Config c, ObjectMapper m, String e) throws Exception { try (TestOrchestrator o = new TestOrchestrator(c, m, e); Scanner s = new Scanner(System.in)) { while (true) { System.out.print("\nRequirement> "); if (!s.hasNextLine()) break; String q = s.nextLine(); if ("exit".equalsIgnoreCase(q.trim())) break; if (q.isBlank()) continue; try { TestPlan p = o.plan(q); ExecutionResult r = o.execute(p); o.analyzeIfFailed(p, r); o.writeReport(r); } catch (Exception x) { System.err.println("Agent error: " + x.getMessage()); } } } }
    private static void usage() { System.out.println("=== AI Testing Agent v" + VERSION + " ===\nCommands:\n  plan <file> [--env <name>]\n  suite <file> [--env <name>]\n  data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]\n  history [--limit <N>]\n  validate plan <file> [--env <name>]\n  validate suite <file> [--env <name>]\n  interactive [--env <name>]\n\nExecution history: ./reports/history/\nTerminal logs: ./reports/<area>/logs/"); }
}
