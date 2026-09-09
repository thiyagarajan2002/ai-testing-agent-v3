package com.thiyagarajan.agent.ai;

import com.thiyagarajan.agent.model.TestPlan;

import java.util.ArrayList;
import java.util.List;

/** Structured AI requirement analysis with executable scenarios and traceability. */
public class AiIntelligenceResult {
    public String summary = "";
    public List<Scenario> scenarios = new ArrayList<>();
    public List<Coverage> coverage = new ArrayList<>();
    public List<String> missingTests = new ArrayList<>();
    public List<List<String>> duplicateGroups = new ArrayList<>();

    public static class Scenario {
        public String id = "";
        public String title = "";
        public String category = "positive";
        public String priority = "medium";
        public String objective = "";
        public TestPlan plan = new TestPlan();
    }

    public static class Coverage {
        public String requirement = "";
        public List<String> testIds = new ArrayList<>();
    }
}
