package com.thiyagarajan.agent.plugin;

/** Contract implemented by optional AI Testing Agent extensions. */
public interface AgentPlugin {
    String id();
    String version();
    default void start() { }
    default void stop() { }
}
