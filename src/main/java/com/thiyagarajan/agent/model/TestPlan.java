package com.thiyagarajan.agent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestPlan {
    public String name = "Generated Test";
    public String type = "API";
    public String baseUrl = "";
    public Map<String, String> variables = new LinkedHashMap<>();
    public List<TestStep> steps = new ArrayList<>();
}
