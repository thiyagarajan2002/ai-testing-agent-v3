package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.cli.CliParser;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.io.RunLogManager;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.plugin.PluginManager;
import com.thiyagarajan.agent.runtime.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public final class Main {
    private static final String VERSION = "3.46.0";
    private Main() { }

    public static void main(String[] a) {
        String[] args = a == null ? new String[0] : a; int exitCode = 0;
        try {
            Config config = Config.load(); String area = logArea(args); String command = args.length == 0 ? "help" : args[0];
            try (RunContext context = RunContext.start(); RunLogManager logs = RunLogManager.start(config.reportsDir(), area, command, context.runId())) {
                logs.log("Run ID: " + context.runId()); logs.log("Command: " + String.join(" ", args));
                try { exitCode = run(args); }
                catch (AgentExecutionException e) { logs.log("Agent error [" + e.category() + "]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                catch (IllegalArgumentException e) { logs.log("Agent error [CONFIGURATION]: " + e.getMessage()); System.err.println("Agent error [CONFIGURATION]: " + e.getMessage()); exitCode = 2; }
                catch (Exception e) { logs.log("Agent error [INFRASTRUCTURE]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                logs.log("Exit code: " + exitCode);
            }
        } catch (AgentExecutionException e) { System.err.println("Agent error [" + e.category() + "]: " + e.getMessage()); exitCode = 2; }
        catch (Exception e) { System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage()); exitCode = 2; }
        if (exitCode != 0) System.exit(exitCode);
    }

    static int run(String[] args) throws Exception {
        CliParser cli = CliParser.parse(args);
        if (cli.versionRequested()) { System.out.println("AI Testing Agent v" + VERSION); return 0; }
        if (cli.helpRequested()) { usage(); return 0; }
        Config config = Config.load(); ObjectMapper mapper = new ObjectMapper(); String env = cli.option("--env");
        return switch (cli.command()) {
            case "history" -> history(config, cli.option("--limit"));
            case "plugins" -> plugins();
            case "plan" -> executePlan(config, mapper, env, cli);
            case "generate" -> generateUiCode(config, mapper, env, cli);
            case "suite" -> executeSuite(config, mapper, env, cli);
            case "data-driven" -> dataDriven(config, mapper, env, cli);
            case "validate" -> validate(config, mapper, env, cli);
            case "interactive" -> { requirePositionalCount(cli, 0, "interactive"); interactive(config, mapper, env); yield 0; }
            default -> throw configuration("Unknown command: " + cli.command());
        };
    }

    private static int executePlan(Config config, ObjectMapper mapper, String env, CliParser cli) throws Exception {
        requirePositionalCount(cli, 1, "plan <file>");
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env)) { return orchestrator.executePlan(cli.positional()[0]).passed() ? 0 : 1; }
    }

    private static int generateUiCode(Config config, ObjectMapper mapper, String env, CliParser cli) throws Exception {
        requirePositionalCount(cli, 1, "generate <requirement> [--output <file>]");
        String requirement = cli.positional()[0]; String source;
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env)) { source = orchestrator.generateUiCode(requirement, "GeneratedUiTest"); }
        Path output = cli.option("--output") == null ? Path.of("generated", "GeneratedUiTest.java") : Path.of(cli.option("--output")).toAbsolutePath().normalize();
        Files.createDirectories(output.getParent() == null ? Path.of(".") : output.getParent()); Files.writeString(output, source);
        System.out.println("Generated UI test: " + output); return 0;
    }

    private static int executeSuite(Config config, ObjectMapper mapper, String env, CliParser cli) throws Exception {
        requirePositionalCount(cli, 1, "suite <file>");
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env)) { return orchestrator.executeSuite(cli.positional()[0]).passed() ? 0 : 1; }
    }

    private static int dataDriven(Config config, ObjectMapper mapper, String env, CliParser cli) throws Exception {
        requirePositionalCount(cli, 2, "data-driven <plan> <data-file>"); String[] positional = cli.positional();
        int workers = cli.option("--parallelism") == null ? config.parallelism() : parseBoundedInt(cli.option("--parallelism"), "--parallelism", 1, Config.MAX_PARALLELISM);
        Map<String, String> filters = new LinkedHashMap<>(); String filter = cli.option("--filter");
        if (filter != null) { String[] parts = filter.split("=", 2); if (parts.length != 2 || parts[0].isBlank()) throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION, "Filter must use key=value"); filters.put(parts[0], parts[1]); }
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env)) { var result = orchestrator.executeDataDriven(positional[0], positional[1], filters, workers); System.out.println("Data-driven: " + result.testName + " | Iterations: " + result.totalIterations + " | Passed: " + result.passedIterations + " | Failed: " + result.failedIterations); return result.passed() ? 0 : 1; }
    }

    private static int validate(Config config, ObjectMapper mapper, String env, CliParser cli) throws Exception {
        requirePositionalCount(cli, 2, "validate <plan|suite> <file>"); String[] positional = cli.positional();
        try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env)) {
            if ("plan".equalsIgnoreCase(positional[0])) return orchestrator.validatePlan(positional[1]).valid() ? 0 : 1;
            if ("suite".equalsIgnoreCase(positional[0])) return orchestrator.validateSuite(positional[1]).valid() ? 0 : 1;
            throw configuration("Validation target must be plan or suite");
        }
    }

    private static int plugins() { PluginManager manager = new PluginManager(); var plugins = manager.discover(); System.out.println("=== Plugins ==="); if (plugins.isEmpty()) System.out.println("No plugins discovered."); else plugins.forEach(plugin -> System.out.println(plugin.id() + " | " + plugin.version())); return 0; }
    private static int history(Config config, String limitText) throws Exception { int limit = limitText == null ? 10 : parseBoundedInt(limitText, "--limit", 1, 1000); Path root = Path.of(config.reportsDir(), "history").toAbsolutePath().normalize(); Path index = root.resolve("index.json"); if (!Files.isRegularFile(index)) { System.out.println("No execution history found."); System.out.println("History directory: " + root); return 0; } ObjectMapper mapper = new ObjectMapper().findAndRegisterModules(); List<RunHistoryManager.RunSummary> all = mapper.readValue(index.toFile(), new com.fasterxml.jackson.core.type.TypeReference<List<RunHistoryManager.RunSummary>>() { }); all.stream().skip(Math.max(0, all.size() - limit)).forEach(r -> System.out.println(r.runId + " | " + r.suiteName + " | " + r.status + " | " + r.passRate + "% | " + r.durationMs + " ms")); return 0; }
    private static int parseBoundedInt(String value, String option, int min, int max) { try { int parsed = Integer.parseInt(value); if (parsed < min || parsed > max) throw configuration(option + " must be between " + min + " and " + max); return parsed; } catch (NumberFormatException e) { throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, option + " must be an integer: " + value, e); } }
    private static void requirePositionalCount(CliParser cli, int expected, String usage) { if (cli.positional().length != expected) throw configuration("Usage: " + usage); }
    private static AgentExecutionException configuration(String message) { return new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION, message); }
    private static String logArea(String[] args) { if (args.length == 0 || args[0].startsWith("--")) return "terminal"; return switch (args[0].toLowerCase()) { case "plan", "suite", "data-driven", "validate" -> "api"; case "generate", "interactive" -> "ui"; case "history" -> "history"; default -> "terminal"; }; }
    private static void interactive(Config config, ObjectMapper mapper, String env) throws Exception { try (TestOrchestrator orchestrator = new TestOrchestrator(config, mapper, env); Scanner scanner = new Scanner(System.in)) { while (true) { System.out.print("\nRequirement> "); if (!scanner.hasNextLine()) break; String requirement = scanner.nextLine(); if ("exit".equalsIgnoreCase(requirement.trim())) break; if (requirement.isBlank()) continue; try { TestPlan plan = orchestrator.plan(requirement); ExecutionResult result = orchestrator.execute(plan); orchestrator.analyzeIfFailed(plan, result); orchestrator.writeReport(result); } catch (Exception e) { System.err.println("Agent error: " + e.getMessage()); } } } }
    private static void usage() { System.out.println("=== AI Testing Agent v" + VERSION + " ===\nCommands:\n  plan <file> [--env <name>]\n  generate <requirement> [--output <file>] [--env <name>]\n  suite <file> [--env <name>]\n  data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]\n  history [--limit <N>]\n  validate plan <file> [--env <name>]\n  validate suite <file> [--env <name>]\n  plugins\n  interactive [--env <name>]\n  version\n  help\n\nUI generation flow: requirement -> locatorless plan -> live DOM -> AI locator resolution -> Java Playwright code\nGlobal options: --help, --version"); }
}
