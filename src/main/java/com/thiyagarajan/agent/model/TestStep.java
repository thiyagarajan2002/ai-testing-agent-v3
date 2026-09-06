package com.thiyagarajan.agent.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestStep {
    public String action;
    public String path = "";
    public String locator = "";
    public String value = "";
    public String body = "";
    public AssertionSpec assertSpec = new AssertionSpec();
    public Map<String, String> save = new LinkedHashMap<>();

    public static class AssertionSpec {
        public Integer status;
        public String contains;
        public String jsonPath;
        public String equals;
    }
}
