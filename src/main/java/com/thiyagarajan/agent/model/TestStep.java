package com.thiyagarajan.agent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestStep {
    public String action;
    public String path = "";
    public String locator = "";
    public String value = "";
    public String body = "";
    public int timeoutMs = 30000;
    /** Number of retries after the first attempt. Null means no step-specific retry override. */
    public Integer retryCount;
    public Map<String, String> headers = new LinkedHashMap<>();
    public Map<String, String> query = new LinkedHashMap<>();
    /** Legacy assertion object retained for backward compatibility. */
    public AssertionSpec assertSpec = new AssertionSpec();
    /** v3.9 supports multiple typed assertions on one API response. */
    public List<Assertion> assertions = new ArrayList<>();
    public Map<String, String> save = new LinkedHashMap<>();

    public static class AssertionSpec {
        public Integer status;
        public String contains;
        public String jsonPath;
        public String equals;
        public Long responseTimeMs;
    }

    public static class Assertion {
        /** status, bodyContains, bodyNotContains, bodyRegex, headerEquals, jsonPathExists,
         * jsonPathEquals, jsonPathContains, jsonPathRegex, responseTimeMs. */
        public String type;
        /** Header name or JSONPath, depending on assertion type. */
        public String path = "";
        /** Expected value/pattern. jsonPathExists accepts true/false; empty means true. */
        public String expected = "";
    }
}
