package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.AgentExecutionException;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.TestOrchestrator;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import com.thiyagarajan.agent.model.TestPlan;

import java.util.Scanner;

/** Thin CLI entry point. Execution lifecycle is delegated to TestOrchestrator. */
public final class Main {
    private static final String VERSION = "3.15.0";
    private Main() { }

    public static void main(String[] args) {
        try {
            int exitCode = run(args == null ? new String[0] : args);
            if (exitCode != 0) System.exit(exitCode);
        } catch (AgentExecutionException e) {
            System.err.println("Agent error [" + e.category() + "]: " + e.getMessage());
            if (e.getCause() != null && e.getCause().getMessage() != null) System.err.println("Cause: " + e.getCause().getMessage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage());
            System.exit(2);
        }
    }

    static int run(String[] args) throws Exception {
        Config config = Config.load();
        ObjectMapper mapper = new ObjectMapper();
        String environment = option(args, "--env");

        if (args.length >= 2 && "plan".equalsIgnoreCase(args[0])) {
            try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, environment)) {
                ExecutionResult result = orchestrator.executePlan(args[1]);
                printPlanResult(result, orchestrator);
                return result.passed() ? 0 : 1;
            }
        }
        if (args.length >= 2 && "suite".equalsIgnoreCase(args[0])) {
            try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, environment)) {
                SuiteExecutionResult result = orchestrator.executeSuite(args[1]);
                printSuiteResult(result, orchestrator);
                return result.passed() ? 0 : 1;
            }
        }
        if (args.length >= 2 && "validate".equalsIgnoreCase(args[0])) {
            try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, environment)) {
                if ("plan".equalsIgnoreCase(args[1]) && args.length >= 3) return validatePlan(orchestrator, args[2]);
                if ("suite".equalsIgnoreCase(args[1]) && args.length >= 3) return validateSuite(orchestrator, args[2]);
                System.err.println("Usage: validate <plan|suite> <file> [--env <name>]");
                return 2;
            }
        }
        if (args.length > 0 && "interactive".equalsIgnoreCase(args[0])) {
            interactive(config, mapper, environment);
            return 0;
        }
        printUsage();
        return args.length == 0 ? 0 : 2;
    }

    private static int validatePlan(TestOrchestrator orchestrator, String file) throws Exception {
        var result = orchestrator.validatePlan(file);
        System.out.println("Plan preflight: " + (result.valid() ? "VALID" : "INVALID"));
        result.warnings().forEach(w -> System.out.println("WARNING: " + w));
        result.errors().forEach(e -> System.out.println("ERROR: " + e));
        return result.valid() ? 0 : 1;
    }

    private static int validateSuite(TestOrchestrator orchestrator, String file) throws Exception {
        var result = orchestrator.validateSuite(file);
        System.out.println("Suite: " + result.suiteName);
        for (var plan : result.plans()) {
            System.out.println((plan.valid() ? "VALID" : "INVALID") + "  " + plan.file());
            plan.warnings().forEach(w -> System.out.println("  WARNING: " + w));
            plan.errors().forEach(e -> System.out.println("  ERROR: " + e));
        }
        System.out.println("Suite preflight: " + (result.valid() ? "VALID" : "INVALID"));
        return result.valid() ? 0 : 1;
    }

    private static void interactive(Config config, ObjectMapper mapper, String environment) throws Exception {
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, environment); Scanner scanner = new Scanner(System.in)) {
            System.out.println("=== AI Testing Agent v" + VERSION + " ===");
            System.out.println("Enter a testing requirement. Type 'exit' to quit.");
            while (true) {
                System.out.print("\nRequirement> ");
                if (!scanner.hasNextLine()) break;
                String requirement = scanner.nextLine();
                if ("exit".equalsIgnoreCase(requirement.trim())) break;
                if (requirement.isBlank()) continue;
                try {
                    TestPlan plan = orchestrator.plan(requirement);
                    System.out.println("\nGenerated plan:");
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan));
                    ExecutionResult result = orchestrator.execute(plan);
                    orchestrator.analyzeIfFailed(plan, result);
                    orchestrator.writeReport(result);
                    printPlanResult(result, orchestrator);
                } catch (AgentExecutionException e) {
                    System.err.println("Agent error [" + e.category() + "]: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage());
                }
            }
        }
    }

    private static void printPlanResult(ExecutionResult result, TestOrchestrator orchestrator) {
        System.out.println("Environment: " + (orchestrator.profile() == null ? "default" : orchestrator.profile().name));
        System.out.println("Result: " + (result.passed() ? "PASSED" : "FAILED"));
        System.out.println("Reports written to ./reports");
    }

    private static void printSuiteResult(SuiteExecutionResult result, TestOrchestrator orchestrator) {
        int threads = Math.min(orchestrator.config().parallelism(), Math.max(1, result.totalTests));
        System.out.println("Suite: " + result.suiteName);
        System.out.println("Environment: " + (orchestrator.profile() == null ? "default" : orchestrator.profile().name));
        System.out.println("Execution mode: parallel | threads=" + threads);
        System.out.println("Tests: " + result.totalTests + " | Passed: " + result.passedTests + " | Failed: " + result.failedTests);
        System.out.println("Duration: " + result.durationMs + " ms");
        System.out.println("Suite reports: " + orchestrator.config().reportsDir() + "/suite");
    }

    private static String option(String[] args, String name) {
        for (int i = 0; i < args.length - 1; i++) if (name.equalsIgnoreCase(args[i])) {
            String value = args[i + 1]; return value.startsWith("--") ? null : value;
        }
        return null;
    }

    private static void printUsage() {
        System.out.println("=== AI Testing Agent v" + VERSION + " ===");
        System.out.println("Commands:");
        System.out.println("  plan <file> [--env <name>]");
        System.out.println("  suite <file> [--env <name>]");
        System.out.println("  validate plan <file> [--env <name>]");
        System.out.println("  validate suite <file> [--env <name>]");
        System.out.println("  interactive [--env <name>]");
    }
}
