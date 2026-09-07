package com.thiyagarajan.agent.model;

import java.util.LinkedHashMap;
import java.util.Map;

/** Environment-specific configuration used to resolve test plans at runtime. */
public class EnvironmentProfile {
    public String name = "local";
    public String baseUrl = "";
    public Integer timeoutMs;
    public String dataFile = "";
    public Map<String, String> variables = new LinkedHashMap<>();
    public Map<String, String> headers = new LinkedHashMap<>();
    public Map<String, String> data = new LinkedHashMap<>();
}
