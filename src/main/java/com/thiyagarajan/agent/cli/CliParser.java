package com.thiyagarajan.agent.cli;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Small dependency-free parser for the agent command line. */
public final class CliParser {
    private static final Set<String> COMMANDS = Set.of("plan", "suite", "data-driven", "history", "validate", "interactive", "plugins", "help", "version");
    private static final Set<String> OPTIONS_WITH_VALUE = Set.of("--env", "--limit", "--filter", "--parallelism", "--output");
    private final String command;
    private final Map<String, String> options;
    private final String[] positional;

    private CliParser(String command, Map<String, String> options, String[] positional) {
        this.command = command; this.options = Map.copyOf(options); this.positional = positional.clone();
    }

    public static CliParser parse(String[] args) {
        String[] a = args == null ? new String[0] : args;
        String command = a.length == 0 ? "help" : a[0].toLowerCase();
        if (!COMMANDS.contains(command)) throw new IllegalArgumentException("Unknown command: " + a[0]);
        Map<String, String> opts = new LinkedHashMap<>(); java.util.List<String> pos = new java.util.ArrayList<>();
        for (int i = 1; i < a.length; i++) {
            String token = a[i];
            if (token.startsWith("--")) {
                if ("--help".equals(token) || "--version".equals(token)) { opts.put(token, "true"); continue; }
                if (!OPTIONS_WITH_VALUE.contains(token)) throw new IllegalArgumentException("Unknown option: " + token);
                if (i + 1 >= a.length || a[i + 1].startsWith("--")) throw new IllegalArgumentException("Missing value for " + token);
                opts.put(token, a[++i]);
            } else pos.add(token);
        }
        return new CliParser(command, opts, pos.toArray(String[]::new));
    }

    public String command() { return command; }
    public String option(String name) { return options.get(name); }
    public boolean hasOption(String name) { return options.containsKey(name); }
    public String[] positional() { return positional.clone(); }
    public boolean helpRequested() { return hasOption("--help") || "help".equals(command); }
    public boolean versionRequested() { return hasOption("--version") || "version".equals(command); }
}
