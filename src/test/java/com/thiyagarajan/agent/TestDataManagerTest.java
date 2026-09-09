package com.thiyagarajan.agent;

import com.thiyagarajan.agent.data.TestDataManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class TestDataManagerTest {
    @Test void loadsRowsAndResolvesVariables() throws Exception {
        var file = Files.createTempFile("data-", ".json");
        Files.writeString(file, "[{\"user\":\"alice\",\"age\":25},{\"user\":\"bob\",\"age\":30}]");
        var manager = new TestDataManager(); var rows = manager.load(file);
        assertEquals(2, rows.size()); assertEquals("alice", rows.get(0).get("user"));
        assertEquals("Hello alice / 25", manager.resolve("Hello {{user}} / {{age}}", rows.get(0)));
    }
    @Test void rejectsMissingVariable() {
        assertThrows(IllegalArgumentException.class, () -> new TestDataManager().resolve("{{missing}}", java.util.Map.of()));
    }
    @Test void rejectsNonArrayData() throws Exception {
        var file = Files.createTempFile("data-object-", ".json"); Files.writeString(file, "{\"user\":\"alice\"}");
        assertThrows(IllegalArgumentException.class, () -> new TestDataManager().load(file));
    }
}
