package com.thiyagarajan.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestStep;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Loads environment profiles and resolves runtime placeholders. */
public final class EnvironmentManager {
    private static final Pattern P = Pattern.compile("\\$\\{(env|data|profile)\\.([A-Za-z0-9_.-]+)}");
    private final ObjectMapper mapper;
    public EnvironmentManager(ObjectMapper mapper) { this.mapper = mapper == null ? new ObjectMapper() : mapper; }

    public EnvironmentProfile load(String environment, Path root) throws Exception {
        String name = environment == null || environment.isBlank() ? "local" : environment.trim();
        Path base = root.toAbsolutePath().normalize();
        Path file = base.resolve("config/environments").resolve(name + ".json").normalize();
        if (!file.startsWith(base) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Environment profile not found: " + file);
        EnvironmentProfile profile = mapper.readValue(Files.readString(file), EnvironmentProfile.class);
        if (profile.name == null || profile.name.isBlank()) profile.name = name;
        if (profile.dataFile != null && !profile.dataFile.isBlank()) loadData(profile, base);
        return profile;
    }

    public TestPlan apply(TestPlan plan, EnvironmentProfile profile) {
        if (plan == null) throw new IllegalArgumentException("Test plan cannot be null");
        if (profile == null) return plan;
        if (profile.baseUrl != null && !profile.baseUrl.isBlank()) plan.baseUrl = resolve(profile.baseUrl, profile);
        else plan.baseUrl = resolve(plan.baseUrl, profile);
        plan.variables = resolveMap(plan.variables, profile);
        if (plan.steps != null) for (TestStep s : plan.steps) {
            if (s == null) continue;
            s.path = resolve(s.path, profile); s.locator = resolve(s.locator, profile);
            s.value = resolve(s.value, profile); s.body = resolve(s.body, profile);
            s.query = resolveMap(s.query, profile); s.save = resolveMap(s.save, profile);
            Map<String,String> headers = new LinkedHashMap<>();
            headers.putAll(resolveMap(profile.headers, profile)); headers.putAll(resolveMap(s.headers, profile)); s.headers = headers;
            if (s.assertSpec != null) { s.assertSpec.contains = resolve(s.assertSpec.contains, profile); s.assertSpec.jsonPath = resolve(s.assertSpec.jsonPath, profile); s.assertSpec.equals = resolve(s.assertSpec.equals, profile); }
            if (profile.timeoutMs != null && profile.timeoutMs > 0 && s.timeoutMs == 30000) s.timeoutMs = profile.timeoutMs;
        }
        return plan;
    }

    private void loadData(EnvironmentProfile profile, Path root) throws Exception {
        Path file = root.resolve(profile.dataFile).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Test data file not found: " + file);
        JsonNode node = mapper.readTree(Files.readString(file));
        if (!node.isObject()) throw new IllegalArgumentException("Test data must be a JSON object: " + file);
        node.fields().forEachRemaining(e -> profile.data.put(e.getKey(), e.getValue().isValueNode() ? e.getValue().asText() : e.getValue().toString()));
    }
    private Map<String,String> resolveMap(Map<String,String> in, EnvironmentProfile p) { Map<String,String> out=new LinkedHashMap<>(); if(in!=null) in.forEach((k,v)->out.put(resolve(k,p),resolve(v,p))); return out; }
    private String resolve(String value, EnvironmentProfile p) {
        if(value==null||value.isEmpty()) return value; Matcher m=P.matcher(value); StringBuffer b=new StringBuffer();
        while(m.find()){ String src=m.group(1), key=m.group(2); String r=switch(src){case "env"->System.getenv(key);case "data"->p.data.get(key);case "profile"->p.variables.get(key);default->null;}; if(r==null) throw new IllegalArgumentException("Unresolved environment placeholder: " + m.group()); m.appendReplacement(b,Matcher.quoteReplacement(r)); } m.appendTail(b); return b.toString();
    }
}
