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
              "action": "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|navigate|click|fill|press|selectOption|hover|check|uncheck|assertVisible|assertText|assertValue|assertTitle|assertUrl|waitFor|waitForVisible|waitForHidden|screenshot",
              "path": "string",
              "target": "natural-language UI target, or empty for API steps",
              "locator": "string",
              "value": "string",
              "body": "string",
              "contentType": "string",
              "assertSpec": {
                "status": 200,
                "contains": "string",
                "jsonPath": "$.id",
                "equals": "string",
                "responseTimeMs": 1000
              },
              "assertions": [
                {"type":"status|bodyContains|bodyNotContains|bodyRegex|headerEquals|jsonPathExists|jsonPathEquals|jsonPathContains|jsonPathRegex|xmlPathExists|xmlPathEquals|responseTimeMs", "path":"header-name-or-json-path", "expected":"value-or-pattern"}
              ],
              "save": {"variableName": "$.json.path"}
            }
          ]
        }

        Rules:
        1. Output JSON only. No markdown.
        2. Never output shell commands, Java, JavaScript, SQL, or arbitrary code.
        3. Use only the listed actions.
        4. Do not invent credentials.
        5. If an API endpoint is supplied, preserve it.
        6. For UI requirements, extract the supplied website into baseUrl. For example, "open youtube.com" means baseUrl "https://www.youtube.com".
        7. For UI steps, prefer a natural-language target and leave locator empty unless the requirement explicitly supplies a locator. Examples: "search input", "Login button", "first video".
        8. Do not invent CSS/XPath locators for UI targets. Locator resolution is performed later against the live DOM by the AI locator resolver.
        9. Prefer assertions that are directly requested.
        10. Use legacy assertSpec for simple checks; use assertions for multiple or advanced checks.
        11. For headerEquals, put the header name in path and the expected value in expected.
        12. For existence assertions, expected may be true or false.

        Requirement:
        """ + requirement;
    }

    public static String intelligencePrompt(String requirement) {
        return """
        You are a senior QA architect. Convert the requirement into a structured, executable test intelligence package.
        Output ONLY valid JSON using this exact top-level shape:
        {
          "summary":"short requirement summary",
          "scenarios":[
            {
              "id":"TC-001",
              "title":"string",
              "category":"positive|negative|boundary|authentication|validation|resilience",
              "priority":"critical|high|medium|low",
              "objective":"what this test proves",
              "plan": {
                "name":"string",
                "type":"API or UI",
                "baseUrl":"string or empty",
                "steps":[
                  {
                    "action":"GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS|navigate|click|fill|press|selectOption|hover|check|uncheck|assertVisible|assertText|assertValue|assertTitle|assertUrl|waitFor|waitForVisible|waitForHidden|screenshot",
                    "path":"string",
                    "target":"natural-language UI target, or empty for API steps",
                    "locator":"string",
                    "value":"string",
                    "body":"string",
                    "contentType":"string",
                    "assertions":[{"type":"status|bodyContains|bodyNotContains|bodyRegex|headerEquals|jsonPathExists|jsonPathEquals|jsonPathContains|jsonPathRegex|xmlPathExists|xmlPathEquals|responseTimeMs","path":"header-name-or-json-path","expected":"string"}],
                    "save":{"variableName":"$.json.path"}
                  }
                ]
              }
            }
          ],
          "coverage":[{"requirement":"atomic requirement clause","testIds":["TC-001"]}],
          "missingTests":["important uncovered scenario, or empty array"],
          "duplicateGroups":[["TC-001","TC-002"]]
        }

        Rules:
        1. JSON only, no markdown or commentary.
        2. Generate distinct positive, negative, boundary and authentication scenarios when applicable.
        3. Every scenario must contain an executable TestPlan using only the allowed actions.
        4. Never invent passwords, API keys, bearer tokens, personal data, or production secrets. Use variable placeholders such as ${token} when authentication is required.
        5. Preserve endpoints, methods, status codes, field names, URLs and explicitly supplied locators.
        6. For UI targets that do not explicitly contain a locator, use target as natural-language intent and leave locator empty. A separate resolver will inspect the live DOM.
        7. Do not claim coverage for a requirement clause unless at least one generated test genuinely exercises it.
        8. Put genuinely uncovered high-value cases in missingTests instead of pretending they are covered.
        9. duplicateGroups must contain only scenario IDs that are substantively redundant; otherwise return an empty array.
        10. Prefer deterministic assertions based on the requirement. Do not invent business responses that are not stated or safely implied.
        11. Generate a practical set of scenarios, normally 4-12 depending on requirement complexity.

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
