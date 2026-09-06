# AI Testing Agent

A Java 21 testing agent that combines:
- Plain-English test planning
- Ollama/local LLM integration
- API execution with RestAssured
- UI execution with Playwright
- Request chaining and variable extraction
- Failure analysis
- HTML, CSV and PDF reports

## Architecture

Requirement -> LLM Planner -> Structured Test Plan -> Safe Tool Executor -> Results
                                                    -> Failure Analyzer
                                                    -> Reports

The LLM never executes arbitrary Java code. It produces a constrained JSON plan,
and the Java executor performs only supported actions.

## Requirements

- JDK 21+
- Maven 3.9+
- Ollama running locally for AI features
- Playwright browsers for UI tests

## Setup

```bash
mvn clean compile
mvn exec:java
```

Install Playwright browsers:

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

Install Ollama and a model, for example:

```bash
ollama pull llama3.2
```

The default Ollama endpoint is `http://localhost:11434`.

## Configuration

Environment variables:

```text
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
BASE_URL=https://example.com
```

## Example request

Run the application and enter:

```text
Create a GET API test for /users. Verify status 200 and verify that the response
contains a field named id.
```

The planner generates a safe JSON plan. The executor runs only known actions.

## Supported API actions

GET, POST, PUT, PATCH, DELETE

Assertions:
- HTTP status
- response body contains text
- JSON path equals value
- JSON path exists
- response time

Variable extraction:
- JSONPath -> runtime variable

Example:

```json
{
  "name": "Create user",
  "type": "API",
  "steps": [
    {
      "action": "POST",
      "path": "/users",
      "body": "{\"name\":\"agent-user\"}",
      "save": {
        "userId": "$.id"
      }
    },
    {
      "action": "GET",
      "path": "/users/${userId}",
      "assert": {
        "status": 200
      }
    }
  ]
}
```

## UI actions

navigate, click, fill, press, selectOption, assertVisible, assertText, screenshot.

## Reports

Generated under:

```text
reports/
  report.html
  report.csv
  report.pdf
```

## Safety model

Never allow the LLM to return shell commands or arbitrary Java. Validate its JSON
against the application's supported actions before execution. Keep secrets in
environment variables and never send credentials to the model.
