package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates the three supported execution report formats from the same
 * {@link ExecutionResult}: HTML, CSV and PDF.
 *
 * Runtime evidence such as terminal/API logs and UI screenshots is kept as
 * separate evidence and linked from the report through step artifacts.
 */
public class ReportManager {
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path dir;
    private final Path reportRoot;
    private final PdfReportWriter pdfWriter = new PdfReportWriter();

    public ReportManager() throws Exception {
        this(Path.of(Config.load().reportsDir()));
    }

    public ReportManager(Path directory) throws Exception {
        if (directory == null) throw new IllegalArgumentException("Report directory cannot be null");
        dir = directory.toAbsolutePath().normalize();
        reportRoot = Path.of(Config.load().reportsDir()).toAbsolutePath().normalize();
        Files.createDirectories(dir);
    }

    /** Generates exactly report.html, report.csv and report.pdf for this result. */
    public void writeAll(ExecutionResult result) throws Exception {
        if (result == null) throw new IllegalArgumentException("Execution result cannot be null");
        normalize(result);
        String generatedAt = OffsetDateTime.now().format(GENERATED_AT);
        writeHtml(result, generatedAt);
        writeCsv(result, generatedAt);
        pdfWriter.write(result, dir.resolve("report.pdf"), generatedAt);
    }

    private void writeHtml(ExecutionResult r, String generatedAt) throws Exception {
        Metrics m = metrics(r);
        String rows = r.steps.stream().map(s -> {
            String status = s.passed ? "PASS" : "FAIL";
            String artifacts = artifactHtml(s.artifacts);
            return "<tr data-status=\"" + status + "\" data-duration=\"" + s.durationMs + "\">"
                    + "<td>" + esc(s.action) + "</td>"
                    + "<td><span class=\"pill " + status.toLowerCase() + "\">" + status + "</span></td>"
                    + "<td>" + s.durationMs + "</td>"
                    + "<td><details><summary>View details</summary><pre>" + esc(s.details) + "</pre>" + artifacts + "</details></td>"
                    + "</tr>";
        }).collect(Collectors.joining("\n"));

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <title>AI Testing Agent Execution Report</title>
                  <style>
                    :root{--bg:#f4f6f8;--card:#fff;--text:#1f2937;--muted:#667085;--line:#e5e7eb;--pass:#16794b;--fail:#b42318;--accent:#344054}
                    [data-theme="dark"]{--bg:#111827;--card:#1f2937;--text:#f9fafb;--muted:#cbd5e1;--line:#374151;--accent:#e5e7eb}
                    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font-family:Inter,Segoe UI,Arial,sans-serif}.wrap{max-width:1400px;margin:auto;padding:28px}
                    header{display:flex;justify-content:space-between;align-items:flex-start;gap:16px;flex-wrap:wrap}.sub{color:var(--muted);margin-top:6px}.actions{display:flex;gap:8px;flex-wrap:wrap}
                    button,a.btn{border:1px solid var(--line);background:var(--card);color:var(--text);padding:9px 12px;border-radius:8px;text-decoration:none;cursor:pointer}
                    .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(150px,1fr));gap:12px;margin:22px 0}.card{background:var(--card);border:1px solid var(--line);border-radius:12px;padding:16px}.label{font-size:12px;color:var(--muted);text-transform:uppercase}.value{font-size:26px;font-weight:700;margin-top:5px}
                    .panel{background:var(--card);border:1px solid var(--line);border-radius:12px;margin-top:16px;padding:16px}.toolbar{display:flex;gap:8px;flex-wrap:wrap;margin-bottom:12px}.toolbar input,.toolbar select{padding:9px;border:1px solid var(--line);border-radius:8px;background:var(--card);color:var(--text)}
                    table{border-collapse:collapse;width:100%%}th,td{border-bottom:1px solid var(--line);padding:10px;text-align:left;vertical-align:top}th{font-size:12px;text-transform:uppercase;color:var(--muted);cursor:pointer}.pill{display:inline-block;padding:4px 8px;border-radius:999px;font-weight:700;font-size:12px}.pill.pass{background:#d1fadf;color:#05603a}.pill.fail{background:#fee4e2;color:#912018}
                    pre{white-space:pre-wrap;word-break:break-word;margin:8px 0 0;background:var(--bg);padding:10px;border-radius:8px}.artifact-list{margin:8px 0 0;padding-left:18px}.artifact-list a{color:inherit;word-break:break-all}.result-pass{color:var(--pass)}.result-fail{color:var(--fail)}
                    @media(max-width:760px){.wrap{padding:14px}.table-wrap{overflow:auto}}
                    @media print{.actions,.toolbar{display:none}.wrap{max-width:none;padding:0}.panel,.card{break-inside:avoid}}
                  </style>
                </head>
                <body>
                <div class="wrap">
                  <header>
                    <div><h1>AI Testing Agent Execution Report</h1><div class="sub">Generated %s</div></div>
                    <div class="actions"><a class="btn" href="report.csv">CSV</a><a class="btn" href="report.pdf">PDF</a><button onclick="window.print()">Print</button><button onclick="toggleTheme()">Theme</button></div>
                  </header>

                  <div class="panel">
                    <b>Test:</b> %s<br>
                    <b>Overall status:</b> <span class="result-%s">%s</span>
                  </div>

                  <section class="cards">
                    <div class="card"><div class="label">Steps</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">Passed</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">Failed</div><div class="value">%d</div></div>
                    <div class="card"><div class="label">Pass rate</div><div class="value">%.1f%%%%</div></div>
                    <div class="card"><div class="label">Duration</div><div class="value">%d ms</div></div>
                    <div class="card"><div class="label">Average</div><div class="value">%.1f ms</div></div>
                  </section>

                  <section class="panel">
                    <h2>Step Results</h2>
                    <div class="toolbar">
                      <input id="search" placeholder="Search action/details" oninput="applyFilters()">
                      <select id="status" onchange="applyFilters()"><option value="ALL">All statuses</option><option>PASS</option><option>FAIL</option></select>
                      <select id="speed" onchange="applyFilters()"><option value="ALL">All durations</option><option value="SLOW">Slow (>= %d ms)</option></select>
                    </div>
                    <div class="table-wrap"><table id="steps"><thead><tr><th onclick="sortTable(0)">Action</th><th onclick="sortTable(1)">Status</th><th onclick="sortTable(2,true)">Duration (ms)</th><th>Details / Input / Output / Artifacts</th></tr></thead><tbody>%s</tbody></table></div>
                  </section>

                  <section class="panel"><h2>Failure Analysis</h2><pre>%s</pre></section>
                  <section class="panel"><h2>Report Files</h2><p><a href="report.html">report.html</a> &nbsp;|&nbsp; <a href="report.csv">report.csv</a> &nbsp;|&nbsp; <a href="report.pdf">report.pdf</a></p><p class="sub">Runtime API/terminal logs and UI screenshots remain separate execution evidence and are linked as step artifacts when available.</p></section>
                </div>
                <script>
                  const slowThreshold=%d;
                  function applyFilters(){const q=document.getElementById('search').value.toLowerCase(),st=document.getElementById('status').value,sp=document.getElementById('speed').value;document.querySelectorAll('#steps tbody tr').forEach(r=>{const text=r.innerText.toLowerCase(),status=r.dataset.status,d=Number(r.dataset.duration);r.style.display=(text.includes(q)&&(st==='ALL'||status===st)&&(sp==='ALL'||d>=slowThreshold))?'':'none';});}
                  function sortTable(col,numeric){const body=document.querySelector('#steps tbody'),rows=[...body.rows],asc=body.dataset.sort!=='asc';rows.sort((a,b)=>{let x=a.cells[col].innerText.trim(),y=b.cells[col].innerText.trim();if(numeric){x=Number(x);y=Number(y);}return (x<y?-1:x>y?1:0)*(asc?1:-1);});rows.forEach(r=>body.appendChild(r));body.dataset.sort=asc?'asc':'desc';}
                  function toggleTheme(){const root=document.documentElement;root.dataset.theme=root.dataset.theme==='dark'?'light':'dark';}
                </script>
                </body></html>
                """.formatted(
                esc(generatedAt), esc(r.testName), r.passed ? "pass" : "fail", r.passed ? "PASS" : "FAIL",
                r.steps.size(), m.passed, m.failed, m.passRate, m.totalDuration, m.averageDuration,
                m.slowThreshold, rows, esc(r.failureAnalysis), m.slowThreshold);

        Files.writeString(dir.resolve("report.html"), html, StandardCharsets.UTF_8);
    }

    private void writeCsv(ExecutionResult r, String generatedAt) throws Exception {
        Metrics m = metrics(r);
        Path file = dir.resolve("report.csv");
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            // UTF-8 BOM improves compatibility with spreadsheet applications.
            w.write('\ufeff');
            w.write("test,action,status,duration_ms,details,artifacts\r\n");
            for (ExecutionResult.StepResult s : r.steps) {
                w.write(csv(r.testName) + "," + csv(s.action) + "," + csv(s.passed ? "PASS" : "FAIL") + "," + s.durationMs
                        + "," + csv(s.details) + "," + csv(s.artifacts == null ? "" : String.join(" | ", s.artifacts)) + "\r\n");
            }
            w.write("\r\n");
            w.write(csv("SUMMARY") + "," + csv("Generated At") + "," + csv(generatedAt) + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Overall Status") + "," + csv(r.passed ? "PASS" : "FAIL") + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Total Steps") + "," + r.steps.size() + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Passed") + "," + m.passed + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Failed") + "," + m.failed + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Pass Rate %") + "," + String.format(java.util.Locale.ROOT, "%.2f", m.passRate) + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Total Duration ms") + "," + m.totalDuration + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Average Duration ms") + "," + String.format(java.util.Locale.ROOT, "%.2f", m.averageDuration) + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Min Duration ms") + "," + m.minDuration + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Max Duration ms") + "," + m.maxDuration + ",,,\r\n");
            w.write(csv("SUMMARY") + "," + csv("Failure Analysis") + "," + csv(r.failureAnalysis) + ",,,\r\n");
        }
    }

    private Metrics metrics(ExecutionResult r) {
        long passed = r.steps.stream().filter(s -> s.passed).count();
        long failed = r.steps.size() - passed;
        long total = r.steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).sum();
        long min = r.steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).min().orElse(0);
        long max = r.steps.stream().mapToLong(s -> Math.max(0, s.durationMs)).max().orElse(0);
        double average = r.steps.isEmpty() ? 0 : (double) total / r.steps.size();
        double passRate = r.steps.isEmpty() ? (r.passed ? 100.0 : 0.0) : (passed * 100.0) / r.steps.size();
        long slowThreshold = Math.max(1000, Math.round(average * 2));
        return new Metrics(passed, failed, total, min, max, average, passRate, slowThreshold);
    }

    private String artifactHtml(List<String> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) return "";
        return "<div><b>Artifacts:</b><ul class=\"artifact-list\">" + artifacts.stream()
                .map(a -> "<li><a href=\"" + esc(artifactLink(a)) + "\">" + esc(a) + "</a></li>")
                .collect(Collectors.joining()) + "</ul></div>";
    }

    private void normalize(ExecutionResult result) {
        if (result.steps == null) result.steps = new java.util.ArrayList<>();
        if (result.failureAnalysis == null) result.failureAnalysis = "";
        for (ExecutionResult.StepResult step : result.steps) {
            if (step.action == null) step.action = "";
            if (step.details == null) step.details = "";
            if (step.artifacts == null) step.artifacts = new java.util.ArrayList<>();
        }
    }

    private String artifactLink(String artifact) {
        if (artifact == null || artifact.isBlank()) return "";
        try {
            Path target = Path.of(artifact).toAbsolutePath().normalize();
            if (target.startsWith(dir) || target.startsWith(reportRoot)) {
                return dir.relativize(target).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
            // Keep the original artifact reference when it is not a filesystem path.
        }
        return artifact.replace('\\', '/');
    }

    private String csv(String value) {
        String v = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private record Metrics(long passed, long failed, long totalDuration, long minDuration, long maxDuration,
                           double averageDuration, double passRate, long slowThreshold) { }
}
