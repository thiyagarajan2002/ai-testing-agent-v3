package com.thiyagarajan.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.runtime.AgentExecutionException;
import com.thiyagarajan.agent.runtime.AgentRunner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiIntelligenceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesAndValidatesExecutableIntelligence() throws Exception {
        String json = """
                {
                  "summary":"User lookup API",
                  "scenarios":[{
                    "id":"TC-001",
                    "title":"Get existing user",
                    "category":"positive",
                    "priority":"high",
                    "objective":"Verify successful lookup",
                    "plan":{
                      "name":"Get existing user",
                      "type":"API",
                      "baseUrl":"https://example.test",
                      "steps":[{"action":"GET","path":"/users/1","assertions":[{"type":"status","expected":"200"}]}]
                    }
                  }],
                  "coverage":[{"requirement":"Existing users can be retrieved","testIds":["TC-001"]}],
                  "missingTests":["Unknown-user behavior is unspecified"],
                  "duplicateGroups":[]
                }
                """;
        AgentRunner runner = new AgentRunner(prompt -> json, mapper);
        AiIntelligenceResult result = runner.intelligence("GET /users/{id} should retrieve an existing user");
        assertEquals(1, result.scenarios.size());
        assertEquals("TC-001", result.scenarios.getFirst().id);
        assertEquals("GET", result.scenarios.getFirst().plan.steps.getFirst().action);
        assertEquals(1, result.coverage.size());
    }

    @Test
    void rejectsCoverageThatReferencesUnknownScenario() {
        String json = """
                {
                  "summary":"bad coverage",
                  "scenarios":[{
                    "id":"TC-001",
                    "plan":{"name":"Valid","type":"API","baseUrl":"https://example.test","steps":[{"action":"HEAD","path":"/health"}]}
                  }],
                  "coverage":[{"requirement":"health endpoint","testIds":["TC-999"]}],
                  "missingTests":[],
                  "duplicateGroups":[]
                }
                """;
        AgentRunner runner = new AgentRunner(prompt -> json, mapper);
        assertThrows(AgentExecutionException.class, () -> runner.intelligence("health endpoint"));
    }

    @Test
    void rejectsDuplicateScenarioIds() {
        String json = """
                {
                  "summary":"duplicates",
                  "scenarios":[
                    {"id":"TC-001","plan":{"name":"A","type":"API","baseUrl":"https://example.test","steps":[{"action":"GET","path":"/a"}]}},
                    {"id":"TC-001","plan":{"name":"B","type":"API","baseUrl":"https://example.test","steps":[{"action":"GET","path":"/b"}]}}
                  ],
                  "coverage":[],"missingTests":[],"duplicateGroups":[]
                }
                """;
        AgentRunner runner = new AgentRunner(prompt -> json, mapper);
        assertThrows(AgentExecutionException.class, () -> runner.intelligence("two tests"));
    }
}
