package com.thiyagarajan.agent.ai;

public final class PromptManager {
    private PromptManager() {}

    public static String planningPrompt(String requirement) {
        return """
        You are a software testing planner.
        Convert the requirement into ONLY valid JSON matching this schema:

        {
          "name": "string",
          "type": "API or UI",
          "baseUrl": "string or empty",
          "steps": [
            {
              "action": "GET|POST|PUT|PATCH|DELETE|navigate|click|fill|press|selectOption|assertVisible|assertText|screenshot",
              "path": "string",
              "locator": "string",
              "value": "string",
              "body": "string",
              "assert": {
                "status": 200,
                "contains": "string",
                "jsonPath": "string",
                "equals": "string"
              },
              "save": {
                "variableName": "$.json.path"
              }
            }
          ]
        }

        Rules:
        1. Output JSON only. No markdown.
        2. Never output shell commands, Java, JavaScript, SQL, or arbitrary code.
        3. Use only the listed actions.
        4. Do not invent credentials.
        5. If an API endpoint is supplied, preserve it.
        6. Use an empty baseUrl when none is supplied.
        7. Prefer assertions that are directly requested.

        Requirement:
        """ + requirement;
    }

    public static String failurePrompt(String plan, String result) {
        return """
        Analyze this automated test failure.
        Give: likely cause, evidence, whether it looks like a product defect,
        and one recommended next debugging step.
        Do not invent evidence.

        TEST PLAN:
        """ + plan + """

        EXECUTION RESULT:
        """ + result;
    }
}
