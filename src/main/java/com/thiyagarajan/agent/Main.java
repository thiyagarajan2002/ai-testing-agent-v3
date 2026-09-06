package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.report.ReportManager;
import com.thiyagarajan.agent.runtime.AgentRunner;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("=== AI Testing Agent ===");
        System.out.println("Enter a testing requirement. Type 'exit' to quit.");

        ObjectMapper mapper = new ObjectMapper();
        OllamaClient llm = new OllamaClient(
                System.getenv().getOrDefault("OLLAMA_URL", "http://localhost:11434"),
                System.getenv().getOrDefault("OLLAMA_MODEL", "llama3.2")
        );

        AgentRunner runner = new AgentRunner(llm, mapper);
        ReportManager reports = new ReportManager();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\nRequirement> ");
                String requirement = scanner.nextLine();
                if ("exit".equalsIgnoreCase(requirement.trim())) break;
                if (requirement.isBlank()) continue;

                try {
                    TestPlan plan = runner.plan(requirement);
                    System.out.println("\nGenerated plan:");
                    System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan));

                    ExecutionResult result = runner.execute(plan);
                    reports.writeAll(result);

                    System.out.println("\nResult: " + (result.passed() ? "PASSED" : "FAILED"));
                    if (!result.passed()) {
                        result = runner.analyzeFailure(plan, result);
                        System.out.println("AI analysis: " + result.failureAnalysis());
                    }
                    System.out.println("Reports written to ./reports");
                } catch (Exception e) {
                    System.err.println("Agent error: " + e.getMessage());
                }
            }
        }
    }
}
