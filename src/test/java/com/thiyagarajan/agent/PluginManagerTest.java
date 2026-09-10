package com.thiyagarajan.agent;

import com.thiyagarajan.agent.plugin.AgentPlugin;
import com.thiyagarajan.agent.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PluginManagerTest {
    @Test void startsWithNoInvalidBuiltInPlugins() {
        PluginManager manager = new PluginManager();
        List<AgentPlugin> plugins = manager.discover();
        assertNotNull(plugins);
        assertEquals(plugins.size(), manager.plugins().size());
    }
}
