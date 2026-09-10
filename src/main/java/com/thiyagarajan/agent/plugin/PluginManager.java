package com.thiyagarajan.agent.plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/** Discovers plugins through Java's standard ServiceLoader mechanism. */
public final class PluginManager {
    private final List<AgentPlugin> plugins = new ArrayList<>();

    public synchronized List<AgentPlugin> discover() {
        plugins.clear();
        ServiceLoader.load(AgentPlugin.class).forEach(plugin -> {
            if (plugin == null || plugin.id() == null || plugin.id().isBlank()) return;
            if (plugin.version() == null || plugin.version().isBlank()) return;
            if (plugins.stream().noneMatch(p -> p.id().equals(plugin.id()))) plugins.add(plugin);
        });
        plugins.sort(Comparator.comparing(AgentPlugin::id));
        return List.copyOf(plugins);
    }

    public synchronized void startAll() { plugins.forEach(AgentPlugin::start); }
    public synchronized void stopAll() { plugins.asReversed().forEach(AgentPlugin::stop); }
    public synchronized List<AgentPlugin> plugins() { return List.copyOf(plugins); }
}
