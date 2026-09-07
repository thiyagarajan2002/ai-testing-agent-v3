package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import com.thiyagarajan.agent.report.ReportManager;
import com.thiyagarajan.agent.report.SuiteReportManager;
import com.thiyagarajan.agent.runtime.AgentRunner;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.SuiteExecutionEngine;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Config config = Config.load();
        ObjectMapper mapper = new ObjectMapper();
        OllamaClient llm = new OllamaClient(config.ollamaUrl(), config.ollamaModel());
        AgentRunner runner = new AgentRunner(llm, mapper);
        ReportManager reports = new ReportManager();

        if (args.length >= 2 && "plan".equalsIgnoreCase(args[0])) {
            executeFile(args[1], mapper, runner, reports);
            return;
        }
        if (args.length >= 2 && "suite".equalsIgnoreCase(args[0])) {
            executeSuite(args[1], mapper, runner, config);
            return;
        }
        System.out.println("=== AI Testing Agent v3.6.0 ===");
        System.out.println("Commands: plan <file> | suite <file> | interactive | exit");
        interactive(mapper, runner, reports);
    }

    private static void executeFile(String file, ObjectMapper mapper, AgentRunner runner, ReportManager reports) throws Exception {
        Path path = Path.of(file);
        if (!Files.exists(path)) throw new IllegalArgumentException("Plan file not found: " + file);
        TestPlan plan = mapper.readValue(Files.readString(path), TestPlan.class);
        ExecutionResult result = runner.execute(plan);
        if (!result.passed()) {
            try { runner.analyzeFailure(plan, result); }
            catch (Exception e) { System.err.println("AI failure analysis unavailable: " + e.getMessage()); }
        }
        reports.writeAll(result);
        System.out.println("Result: " + (result.passed() ? "PASSED" : "FAILED"));
        System.out.println("Reports written to ./reports");
        if (!result.passed()) System.exit(1);
    }

    private static void executeSuite(String file, ObjectMapper mapper, AgentRunner runner, Config config) throws Exception {
        Path suitePath = Path.of(file).toAbsolutePath().normalize();
        if (!Files.exists(suitePath)) throw new IllegalArgumentException("Suite file not found: " + file);
        TestSuite suite = mapper.readValue(Files.readString(suitePath), TestSuite.class);
        Path suiteDirectory = suitePath.getParent() == null ? Path.of(".").toAbsolutePath() : suitePath.getParent();

        SuiteExecutionEngine engine = new SuiteExecutionEngine(mapper, runner, config.parallelism());
        SuiteExecutionResult result = engine.execute(suite, suiteDirectory);
        Path suiteReports = Path.of(config.reportsDir(), "suite").toAbsolutePath().normalize();
        new SuiteReportManager(suiteReports).writeAll(result);

        System.out.println("Suite: " + suite.name);
        System.out.println("Execution mode: parallel | threads=" + Math.min(config.parallelism(), Math.max(1, result.totalTests)));
        System.out.println("Tests: " + result.totalTests + " | Passed: " + result.passedTests + " | Failed: " + result.failedTests);
        System.out.println("Duration: " + result.durationMs + " ms");
        System.out.println("Suite reports: " + suiteReports);
        if (!result.passed()) System.exit(1);
    }

    private static void interactive(ObjectMapper mapper, AgentRunner runner, ReportManager reports) throws Exception {
        System.out.println("Enter a testing requirement. Type 'exit' to quit.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nRequirement> ");
                String requirement = scanner.nextLine();
                if ("exit".equalsIgnoreCase(requirement.trim())) break;
                if (requirement.isBlank()) continue;
                try {
                    TestPlan plan = runner.plan(requirement);
                    System.out.println("\nGenerated plan:");
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan));
                    ExecutionResult result = runner.execute(plan);
                    if (!result.passed()) {
                        try { runner.analyzeFailure(plan, result); System.out.println("AI analysis: " + result.failureAnalysis()); }
                        catch (Exception e) { System.err.println("AI failure analysis unavailable: " + e.getMessage()); }
                    }
                    reports.writeAll(result);
                    System.out.println("\nResult: " + (result.passed() ? "PASSED" : "FAILED"));
                    System.out.println("Reports written to ./reports");
                } catch (Exception e) {
                    System.err.println("Agent error: " + e.getMessage());
                }
            }
        }
    }
}
