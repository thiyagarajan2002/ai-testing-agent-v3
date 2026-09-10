package com.thiyagarajan.agent.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestStep {
    public String action;
    public String path = "";
    /** Natural-language UI intent. When present, the framework resolves the actual locator from live DOM evidence. */
    public String target = "";
    /** Optional explicit Playwright locator retained for backward compatibility. */
    public String locator = "";
    public String value = "";
    public String body = "";
    /** Request Content-Type. Defaults to application/json when a body is sent and no type is supplied. */
    public String contentType = "";
    public int timeoutMs = 30000;
    /** Number of retries after the first attempt. Null means no step-specific retry override. */
    public Integer retryCount;
    public Map<String, String> headers = new LinkedHashMap<>();
    public Map<String, String> query = new LinkedHashMap<>();
    /** Optional step authentication. Supported types: bearer, basic, apiKeyHeader, apiKeyQuery. */
    public AuthSpec auth = new AuthSpec();
    /** Optional form fields for form-urlencoded requests. */
    public Map<String, String> form = new LinkedHashMap<>();
    /** Legacy assertion object retained for backward compatibility. */
    public AssertionSpec assertSpec = new AssertionSpec();
    /** v3.9 supports multiple typed assertions on one API response. */
    public List<Assertion> assertions = new ArrayList<>();
    public Map<String, String> save = new LinkedHashMap<>();

    public static class AuthSpec {
        public String type = "none";
        public String token = "";
        public String username = "";
        public String password = "";
        public String key = "";
        public String value = "";
    }

    public static class AssertionSpec {
        public Integer status;
        public String contains;
        public String jsonPath;
        public String equals;
        public Long responseTimeMs;
    }

    public static class Assertion {
        /** status, bodyContains, bodyNotContains, bodyRegex, headerEquals, jsonPathExists,
         * jsonPathEquals, jsonPathContains, jsonPathRegex, responseTimeMs, xmlPathEquals,
         * xmlPathExists. */
        public String type;
        /** Header name or JSONPath/XMLPath, depending on assertion type. */
        public String path = "";
        /** Expected value/pattern. jsonPathExists/xmlPathExists accepts true/false; empty means true. */
        public String expected = "";
    }
}
