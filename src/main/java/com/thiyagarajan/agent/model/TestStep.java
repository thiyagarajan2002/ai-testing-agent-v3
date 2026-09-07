package com.thiyagarajan.agent.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestStep {
    public String action;
    public String path = "";
    public String locator = "";
    public String value = "";
    public String body = "";
    public int timeoutMs = 30000;
    /** Number of retries after the first attempt. Null uses the global RETRIES configuration. */
    public Integer retryCount;
    public Map<String, String> headers = new LinkedHashMap<>();
    public Map<String, String> query = new LinkedHashMap<>();
    public AssertionSpec assertSpec = new AssertionSpec();
    public Map<String, String> save = new LinkedHashMap<>();

    public static class AssertionSpec {
        public Integer status;
        public String contains;
        public String jsonPath;
        public String equals;
        public Long responseTimeMs;
    }
}
