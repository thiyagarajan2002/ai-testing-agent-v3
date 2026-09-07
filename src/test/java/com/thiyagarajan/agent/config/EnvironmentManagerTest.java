package com.thiyagarajan.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentManagerTest {
    @Test
    void appliesProfileAndDataPlaceholders() throws Exception {
        Path root=Files.createTempDirectory("agent-env-");
        Files.createDirectories(root.resolve("config/environments")); Files.createDirectories(root.resolve("config/test-data"));
        Files.writeString(root.resolve("config/environments/qa.json"),"{\"name\":\"qa\",\"baseUrl\":\"https://qa.example.test\",\"dataFile\":\"config/test-data/data.json\",\"variables\":{\"tenant\":\"demo\"},\"headers\":{\"X-Tenant\":\"${profile.tenant}\"}}");
        Files.writeString(root.resolve("config/test-data/data.json"),"{\"id\":\"42\",\"name\":\"Alice\"}");
        EnvironmentManager manager=new EnvironmentManager(new ObjectMapper()); EnvironmentProfile p=manager.load("qa",root);
        TestPlan plan=new TestPlan(); plan.baseUrl="${data.name}"; TestStep s=new TestStep(); s.action="GET"; s.path="/users/${data.id}"; plan.steps.add(s);
        manager.apply(plan,p);
        assertEquals("https://qa.example.test",plan.baseUrl); assertEquals("/users/42",s.path); assertEquals("demo",s.headers.get("X-Tenant"));
    }

    @Test
    void rejectsMissingProfile() throws Exception {
        Path root=Files.createTempDirectory("agent-env-");
        EnvironmentManager manager=new EnvironmentManager(new ObjectMapper());
        assertThrows(IllegalArgumentException.class,()->manager.load("missing",root));
    }
}
