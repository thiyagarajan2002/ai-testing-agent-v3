package com.thiyagarajan.agent.report;

import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.ExecutionResult;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Generates the three supported execution report formats from the same
 * {@link ExecutionResult}: HTML, CSV and PDF.
 *
 * The HTML report is a self-contained, responsive execution dashboard. Runtime
 * evidence such as terminal/API logs and UI screenshots remains separate and
 * is linked through step artifacts.
 */
public class ReportManager {
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int PAGE_SIZE = 25;

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
        String rows = r.steps.stream().map(this::stepRow).collect(Collectors.joining("\n"));
        String failure = r.failureAnalysis.isBlank() ? "No failure analysis recorded." : r.failureAnalysis;
        String statusClass = r.passed ? "pass" : "fail";
        String statusLabel = r.passed ? "PASSED" : "FAILED";
        String chartStyle = "conic-gradient(var(--pass) 0 " + formatPercent(m.passRate) + "%, var(--fail) "
                + formatPercent(m.passRate) + "% 100%)";

        String html = """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta name="color-scheme" content="light dark">
                  <meta name="description" content="AI Testing Agent execution report">
                  <title>AI Testing Agent — Execution Report</title>
                  <style>
                    :root{
                      --bg:#f5f7fa;--surface:#ffffff;--surface-2:#f8fafc;--text:#172033;--muted:#667085;
                      --line:#e4e7ec;--primary:#344054;--pass:#12b76a;--pass-bg:#ecfdf3;--fail:#f04438;--fail-bg:#fef3f2;
                      --warn:#f79009;--warn-bg:#fffaeb;--shadow:0 1px 2px rgba(16,24,40,.05),0 4px 14px rgba(16,24,40,.04)
                    }
                    [data-theme="dark"]{
                      --bg:#0b1220;--surface:#111a2b;--surface-2:#172235;--text:#f2f4f7;--muted:#aab4c3;
                      --line:#263247;--primary:#e4e7ec;--pass:#32d583;--pass-bg:#063b26;--fail:#f97066;--fail-bg:#4a1512;
                      --warn:#fdb022;--warn-bg:#422b08;--shadow:0 2px 12px rgba(0,0,0,.25)
                    }
                    *{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--bg);color:var(--text);font-family:Inter,ui-sans-serif,system-ui,-apple-system,"Segoe UI",Arial,sans-serif;line-height:1.45}
                    a{color:inherit}.container{max-width:1480px;margin:0 auto;padding:28px 24px 56px}.topbar{display:flex;justify-content:space-between;align-items:flex-start;gap:20px;flex-wrap:wrap}
                    h1{font-size:30px;line-height:1.2;margin:0;font-weight:750;letter-spacing:-.02em}.eyebrow{font-size:12px;text-transform:uppercase;letter-spacing:.08em;color:var(--muted);font-weight:700;margin-bottom:7px}.sub{color:var(--muted);font-size:13px;margin-top:7px}.actions{display:flex;gap:8px;flex-wrap:wrap}
                    button,.btn{border:1px solid var(--line);background:var(--surface);color:var(--text);border-radius:8px;padding:9px 12px;font:inherit;font-size:13px;text-decoration:none;cursor:pointer;box-shadow:var(--shadow)}button:hover,.btn:hover{border-color:#98a2b3}
                    .hero{display:grid;grid-template-columns:1fr auto;gap:20px;align-items:center;background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:20px;margin-top:22px;box-shadow:var(--shadow)}
                    .status{display:inline-flex;align-items:center;gap:8px;border-radius:999px;padding:7px 12px;font-weight:800;font-size:13px}.status.pass{background:var(--pass-bg);color:var(--pass)}.status.fail{background:var(--fail-bg);color:var(--fail)}.dot{width:8px;height:8px;border-radius:50%;background:currentColor}
                    .kpis{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:12px;margin:16px 0}.kpi{background:var(--surface);border:1px solid var(--line);border-radius:12px;padding:15px;box-shadow:var(--shadow);min-width:0}.kpi-label{font-size:11px;color:var(--muted);text-transform:uppercase;letter-spacing:.06em;font-weight:700}.kpi-value{font-size:25px;font-weight:750;margin-top:5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.kpi-note{font-size:12px;color:var(--muted);margin-top:3px}
                    .grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.panel{background:var(--surface);border:1px solid var(--line);border-radius:14px;padding:18px;box-shadow:var(--shadow);margin-top:16px}.panel h2{font-size:17px;margin:0 0 14px}.panel h3{font-size:13px;margin:0 0 8px}.summary-grid{display:grid;grid-template-columns:180px 1fr;gap:20px;align-items:center}.ring{width:150px;height:150px;border-radius:50%;display:grid;place-items:center;background:var(--ring)}.ring::after{content:"";width:102px;height:102px;border-radius:50%;background:var(--surface)}.ring-label{position:absolute;text-align:center;z-index:1;font-weight:800}.ring-wrap{position:relative;display:grid;place-items:center}.legend{display:grid;grid-template-columns:1fr 1fr;gap:8px}.legend-item{display:flex;align-items:center;gap:8px;font-size:13px;color:var(--muted)}.legend-swatch{width:10px;height:10px;border-radius:3px;background:var(--swatch)}
                    .metric-list{display:grid;grid-template-columns:1fr 1fr;gap:10px}.metric{background:var(--surface-2);border:1px solid var(--line);border-radius:9px;padding:11px}.metric b{display:block;font-size:16px}.metric span{font-size:11px;color:var(--muted)}
                    .toolbar{display:flex;gap:8px;align-items:center;flex-wrap:wrap;padding-bottom:14px;border-bottom:1px solid var(--line)}.toolbar input,.toolbar select{border:1px solid var(--line);background:var(--surface);color:var(--text);border-radius:8px;padding:9px 10px;font:inherit;font-size:13px}.toolbar input{min-width:260px;flex:1}.toolbar .count{font-size:12px;color:var(--muted);margin-left:auto}
                    .table-wrap{overflow:auto}.table{width:100%;border-collapse:separate;border-spacing:0;min-width:900px}.table th{position:sticky;top:0;background:var(--surface-2);color:var(--muted);font-size:11px;text-transform:uppercase;letter-spacing:.05em;text-align:left;padding:11px;border-bottom:1px solid var(--line);white-space:nowrap}.table td{padding:12px 11px;border-bottom:1px solid var(--line);vertical-align:top;font-size:13px}.table tr:last-child td{border-bottom:0}.table tr:hover td{background:var(--surface-2)}.index{color:var(--muted);font-variant-numeric:tabular-nums}.duration{font-variant-numeric:tabular-nums;white-space:nowrap;font-weight:650}.slow{color:var(--warn)}
                    .pill{display:inline-flex;align-items:center;border-radius:999px;padding:4px 8px;font-size:11px;font-weight:800}.pill.pass{background:var(--pass-bg);color:var(--pass)}.pill.fail{background:var(--fail-bg);color:var(--fail)}.details{max-width:760px}.details summary{cursor:pointer;font-weight:650;color:var(--primary)}.details pre,.failure{white-space:pre-wrap;word-break:break-word;background:var(--surface-2);border:1px solid var(--line);border-radius:8px;padding:12px;margin:9px 0 0;font:12px/1.55 ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;max-height:360px;overflow:auto}.artifact-list{display:flex;flex-wrap:wrap;gap:7px;margin:10px 0 0;padding:0;list-style:none}.artifact-list a{display:inline-block;border:1px solid var(--line);border-radius:7px;padding:5px 8px;font-size:11px;text-decoration:none;background:var(--surface)}.artifact-list a:hover{text-decoration:underline}
                    .bar-list{display:grid;gap:10px}.bar-row{display:grid;grid-template-columns:115px 1fr 55px;gap:10px;align-items:center;font-size:12px}.bar-track{height:9px;border-radius:99px;background:var(--surface-2);overflow:hidden}.bar-fill{height:100%;border-radius:99px;background:var(--primary);min-width:2px}.bar-value{text-align:right;font-variant-numeric:tabular-nums;color:var(--muted)}
                    .failure-box{border-left:4px solid var(--fail);background:var(--fail-bg);padding:14px;border-radius:8px}.failure-box.ok{border-left-color:var(--pass);background:var(--pass-bg)}.footer{margin-top:22px;color:var(--muted);font-size:12px;display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap}.pagination{display:flex;justify-content:flex-end;align-items:center;gap:8px;margin-top:13px}.pagination button:disabled{opacity:.45;cursor:not-allowed}
                    .empty{text-align:center;color:var(--muted);padding:28px}.sr-only{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}
                    @media(max-width:1100px){.kpis{grid-template-columns:repeat(3,minmax(0,1fr))}.grid{grid-template-columns:1fr}}@media(max-width:700px){.container{padding:18px 12px 40px}.hero{grid-template-columns:1fr}.kpis{grid-template-columns:repeat(2,minmax(0,1fr))}.summary-grid{grid-template-columns:1fr}.toolbar input{min-width:100%}.metric-list,.legend{grid-template-columns:1fr}}
                    @media print{body{background:#fff}.container{max-width:none;padding:0}.actions,.toolbar,.pagination{display:none}.panel,.kpi,.hero{box-shadow:none;break-inside:avoid}.table{min-width:0}.table th{position:static}.details pre{max-height:none}.footer{margin-top:12px}}
                  </style>
                </head>
                <body>
                  <main class="container">
                    <header class="topbar">
                      <div><div class="eyebrow">AI Testing Agent · Execution Report</div><h1>Test execution dashboard</h1><div class="sub">Generated <span id="generatedAt">__GENERATED_AT__</span></div></div>
                      <div class="actions"><a class="btn" href="report.csv" download>Export CSV</a><a class="btn" href="report.pdf" target="_blank" rel="noopener">Open PDF</a><button type="button" onclick="window.print()">Print</button><button type="button" id="themeBtn" onclick="toggleTheme()">Dark mode</button></div>
                    </header>

                    <section class="hero" aria-label="Execution identity">
                      <div><div class="eyebrow">Test / Plan</div><div style="font-size:19px;font-weight:700;word-break:break-word">__TEST_NAME__</div><div class="sub">Execution evidence is retained separately and linked from individual steps.</div></div>
                      <div><span class="status __STATUS_CLASS__"><span class="dot"></span>__STATUS_LABEL__</span></div>
                    </section>

                    <section class="kpis" aria-label="Execution metrics">
                      <div class="kpi"><div class="kpi-label">Total steps</div><div class="kpi-value">__TOTAL_STEPS__</div><div class="kpi-note">Executed actions</div></div>
                      <div class="kpi"><div class="kpi-label">Passed</div><div class="kpi-value">__PASSED__</div><div class="kpi-note">Successful steps</div></div>
                      <div class="kpi"><div class="kpi-label">Failed</div><div class="kpi-value">__FAILED__</div><div class="kpi-note">Unsuccessful steps</div></div>
                      <div class="kpi"><div class="kpi-label">Pass rate</div><div class="kpi-value">__PASS_RATE__%</div><div class="kpi-note">Step-level success</div></div>
                      <div class="kpi"><div class="kpi-label">Total duration</div><div class="kpi-value">__TOTAL_DURATION__</div><div class="kpi-note">Milliseconds</div></div>
                      <div class="kpi"><div class="kpi-label">Average</div><div class="kpi-value">__AVERAGE__</div><div class="kpi-note">Per step</div></div>
                    </section>

                    <section class="grid">
                      <article class="panel"><h2>Outcome overview</h2><div class="summary-grid"><div class="ring-wrap"><div class="ring" style="--ring:__RING_STYLE__"></div><div class="ring-label">__PASS_RATE__%<br><span style="font-size:10px;color:var(--muted)">PASS RATE</span></div></div><div><div class="legend"><div class="legend-item"><span class="legend-swatch" style="--swatch:var(--pass)"></span>Passed <b>__PASSED__</b></div><div class="legend-item"><span class="legend-swatch" style="--swatch:var(--fail)"></span>Failed <b>__FAILED__</b></div></div><div class="metric-list" style="margin-top:14px"><div class="metric"><b>__MIN__ ms</b><span>Fastest step</span></div><div class="metric"><b>__MAX__ ms</b><span>Slowest step</span></div></div></div></div></article>
                      <article class="panel"><h2>Latency profile</h2><div class="bar-list"><div class="bar-row"><span>Fast &lt; 1s</span><div class="bar-track"><div class="bar-fill" style="width:__FAST_PCT__%"></div></div><span class="bar-value">__FAST_COUNT__</span></div><div class="bar-row"><span>Normal 1–2s</span><div class="bar-track"><div class="bar-fill" style="width:__NORMAL_PCT__%"></div></div><span class="bar-value">__NORMAL_COUNT__</span></div><div class="bar-row"><span>Slow &gt; 2s</span><div class="bar-track"><div class="bar-fill" style="width:__SLOW_PCT__%"></div></div><span class="bar-value">__SLOW_COUNT__</span></div></div><div class="metric-list" style="margin-top:14px"><div class="metric"><b>__MEDIAN__ ms</b><span>Median duration</span></div><div class="metric"><b>__P95__ ms</b><span>P95 duration</span></div></div></article>
                    </section>

                    <section class="panel" id="stepResults"><h2>Step results</h2><div class="toolbar"><label class="sr-only" for="search">Search steps</label><input id="search" type="search" placeholder="Search actions, details or artifacts…" oninput="resetPageAndFilter()"><label class="sr-only" for="status">Status</label><select id="status" onchange="resetPageAndFilter()"><option value="ALL">All statuses</option><option value="PASS">Passed</option><option value="FAIL">Failed</option></select><label class="sr-only" for="duration">Duration</label><select id="duration" onchange="resetPageAndFilter()"><option value="ALL">All durations</option><option value="SLOW">Slow steps</option><option value="FAST">Fast steps</option></select><select id="sort" onchange="renderRows()"><option value="index">Execution order</option><option value="duration-desc">Duration: high → low</option><option value="duration-asc">Duration: low → high</option><option value="status">Status</option></select><span class="count" id="resultCount" aria-live="polite"></span></div><div class="table-wrap"><table class="table" id="stepTable"><thead><tr><th>#</th><th>Action</th><th>Status</th><th>Duration</th><th>Details / Input / Output / Artifacts</th></tr></thead><tbody>__ROWS__</tbody></table></div><div class="pagination"><button type="button" id="prev" onclick="changePage(-1)">Previous</button><span id="pageInfo" class="sub"></span><button type="button" id="next" onclick="changePage(1)">Next</button></div></section>

                    <section class="panel"><h2>Failure analysis</h2><div class="failure-box __FAILURE_CLASS__"><div class="failure">__FAILURE__</div></div></section>

                    <footer class="footer"><span>AI Testing Agent · HTML report</span><span><a href="report.csv">CSV</a> · <a href="report.pdf">PDF</a> · <a href="report.html">HTML</a></span></footer>
                  </main>
                  <script>
                    const PAGE_SIZE=__PAGE_SIZE__;
                    let page=1;
                    function rows(){return [...document.querySelectorAll('#stepTable tbody tr')];}
                    function matches(r){const q=document.getElementById('search').value.trim().toLowerCase();const st=document.getElementById('status').value;const du=document.getElementById('duration').value;const text=r.innerText.toLowerCase();const status=r.dataset.status;const d=Number(r.dataset.duration);return (!q||text.includes(q))&&(st==='ALL'||status===st)&&(du==='ALL'||(du==='SLOW'&&d>=2000)||(du==='FAST'&&d<1000));}
                    function renderRows(){const body=document.querySelector('#stepTable tbody');let list=rows();list.forEach(r=>r.style.display='none');list=list.filter(matches);const sort=document.getElementById('sort').value;if(sort!=='index'){list.sort((a,b)=>{if(sort==='duration-desc')return Number(b.dataset.duration)-Number(a.dataset.duration);if(sort==='duration-asc')return Number(a.dataset.duration)-Number(b.dataset.duration);return a.dataset.status.localeCompare(b.dataset.status);});}const totalPages=Math.max(1,Math.ceil(list.length/PAGE_SIZE));if(page>totalPages)page=totalPages;const start=(page-1)*PAGE_SIZE;list.slice(start,start+PAGE_SIZE).forEach(r=>r.style.display='');document.getElementById('resultCount').textContent=list.length+' matching step'+(list.length===1?'':'s');document.getElementById('pageInfo').textContent='Page '+page+' of '+totalPages;document.getElementById('prev').disabled=page<=1;document.getElementById('next').disabled=page>=totalPages;}
                    function resetPageAndFilter(){page=1;renderRows();}
                    function changePage(delta){page+=delta;renderRows();document.getElementById('stepResults').scrollIntoView({behavior:'smooth',block:'start'});}
                    function toggleTheme(){const root=document.documentElement;const dark=root.dataset.theme==='dark';root.dataset.theme=dark?'light':'dark';localStorage.setItem('ai-testing-report-theme',root.dataset.theme);document.getElementById('themeBtn').textContent=dark?'Dark mode':'Light mode';}
                    (function(){const saved=localStorage.getItem('ai-testing-report-theme');if(saved)document.documentElement.dataset.theme=saved;document.getElementById('themeBtn').textContent=document.documentElement.dataset.theme==='dark'?'Light mode':'Dark mode';renderRows();})();
                  </script>
                </body>
                </html>
                """;

        html = html.replace("__GENERATED_AT__", esc(generatedAt))
                .replace("__TEST_NAME__", esc(r.testName))
                .replace("__STATUS_CLASS__", statusClass)
                .replace("__STATUS_LABEL__", statusLabel)
                .replace("__TOTAL_STEPS__", String.valueOf(r.steps.size()))
                .replace("__PASSED__", String.valueOf(m.passed))
                .replace("__FAILED__", String.valueOf(m.failed))
                .replace("__PASS_RATE__", formatPercent(m.passRate))
                .replace("__TOTAL_DURATION__", String.valueOf(m.totalDuration))
                .replace("__AVERAGE__", formatMs(m.averageDuration))
                .replace("__MIN__", String.valueOf(m.minDuration))
                .replace("__MAX__", String.valueOf(m.maxDuration))
                .replace("__MEDIAN__", String.valueOf(m.medianDuration))
                .replace("__P95__", String.valueOf(m.p95Duration))
                .replace("__FAST_COUNT__", String.valueOf(m.fastCount))
                .replace("__NORMAL_COUNT__", String.valueOf(m.normalCount))
                .replace("__SLOW_COUNT__", String.valueOf(m.slowCount))
                .replace("__FAST_PCT__", String.valueOf(m.fastPct))
                .replace("__NORMAL_PCT__", String.valueOf(m.normalPct))
                .replace("__SLOW_PCT__", String.valueOf(m.slowPct))
                .replace("__RING_STYLE__", chartStyle)
                .replace("__ROWS__", rows.isBlank() ? "<tr><td colspan=\"5\" class=\"empty\">No execution steps recorded.</td></tr>" : rows)
                .replace("__FAILURE_CLASS__", r.failureAnalysis.isBlank() ? "ok" : "")
                .replace("__FAILURE__", esc(failure))
                .replace("__PAGE_SIZE__", String.valueOf(PAGE_SIZE));

        Files.writeString(dir.resolve("report.html"), html, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private String stepRow(ExecutionResult.StepResult s) {
        String status = s.passed ? "PASS" : "FAIL";
        boolean slow = s.durationMs >= 2000;
        String artifacts = artifactHtml(s.artifacts);
        String detail = s.details == null || s.details.isBlank() ? "No details recorded." : s.details;
        return "<tr data-status=\"" + status + "\" data-duration=\"" + Math.max(0, s.durationMs) + "\">"
                + "<td class=\"index\">" + "</td>"
                + "<td><b>" + esc(s.action) + "</b></td>"
                + "<td><span class=\"pill " + status.toLowerCase(Locale.ROOT) + "\"><span class=\"dot\"></span>" + status + "</span></td>"
                + "<td class=\"duration " + (slow ? "slow" : "") + "\">" + Math.max(0, s.durationMs) + " ms" + (slow ? " ⚠" : "") + "</td>"
                + "<td class=\"details\"><details><summary>View execution details</summary><pre>" + esc(detail) + "</pre>" + artifacts + "</details></td>"
                + "</tr>";
    }

    private void writeCsv(ExecutionResult r, String generatedAt) throws Exception {
        Metrics m = metrics(r);
        Path file = dir.resolve("report.csv");
        try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            w.write('\ufeff');
            w.write("test,step_no,action,status,duration_ms,slow,details,artifacts\r\n");
            for (int i = 0; i < r.steps.size(); i++) {
                ExecutionResult.StepResult s = r.steps.get(i);
                w.write(csv(r.testName) + "," + (i + 1) + "," + csv(s.action) + "," + csv(s.passed ? "PASS" : "FAIL") + ","
                        + Math.max(0, s.durationMs) + "," + (s.durationMs >= 2000 ? "YES" : "NO") + ","
                        + csv(s.details) + "," + csv(s.artifacts == null ? "" : String.join(" | ", s.artifacts)) + "\r\n");
            }
            w.write("\r\n");
            summaryRow(w, "Generated At", generatedAt);
            summaryRow(w, "Overall Status", r.passed ? "PASS" : "FAIL");
            summaryRow(w, "Total Steps", String.valueOf(r.steps.size()));
            summaryRow(w, "Passed", String.valueOf(m.passed));
            summaryRow(w, "Failed", String.valueOf(m.failed));
            summaryRow(w, "Pass Rate %", formatPercent(m.passRate));
            summaryRow(w, "Total Duration ms", String.valueOf(m.totalDuration));
            summaryRow(w, "Average Duration ms", formatMs(m.averageDuration));
            summaryRow(w, "Median Duration ms", String.valueOf(m.medianDuration));
            summaryRow(w, "P95 Duration ms", String.valueOf(m.p95Duration));
            summaryRow(w, "Min Duration ms", String.valueOf(m.minDuration));
            summaryRow(w, "Max Duration ms", String.valueOf(m.maxDuration));
            summaryRow(w, "Slow Steps", String.valueOf(m.slowCount));
            summaryRow(w, "Failure Analysis", r.failureAnalysis);
        }
    }

    private void summaryRow(BufferedWriter w, String key, String value) throws Exception {
        w.write(csv("SUMMARY") + "," + csv(key) + "," + csv(value) + ",,,,\r\n");
    }

    private Metrics metrics(ExecutionResult r) {
        List<Long> durations = r.steps.stream().map(s -> Math.max(0, s.durationMs)).sorted().toList();
        long passed = r.steps.stream().filter(s -> s.passed).count();
        long failed = r.steps.size() - passed;
        long total = durations.stream().mapToLong(Long::longValue).sum();
        long min = durations.stream().mapToLong(Long::longValue).min().orElse(0);
        long max = durations.stream().mapToLong(Long::longValue).max().orElse(0);
        double average = r.steps.isEmpty() ? 0 : (double) total / r.steps.size();
        double passRate = r.steps.isEmpty() ? (r.passed ? 100.0 : 0.0) : (passed * 100.0) / r.steps.size();
        long median = percentile(durations, 50);
        long p95 = percentile(durations, 95);
        long fast = r.steps.stream().filter(s -> s.durationMs < 1000).count();
        long normal = r.steps.stream().filter(s -> s.durationMs >= 1000 && s.durationMs < 2000).count();
        long slow = r.steps.stream().filter(s -> s.durationMs >= 2000).count();
        int size = Math.max(1, r.steps.size());
        int fastPct = (int) Math.round(fast * 100.0 / size);
        int normalPct = (int) Math.round(normal * 100.0 / size);
        int slowPct = (int) Math.round(slow * 100.0 / size);
        return new Metrics(passed, failed, total, min, max, average, passRate, median, p95, fast, normal, slow,
                fastPct, normalPct, slowPct);
    }

    private long percentile(List<Long> sorted, int percentile) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private String artifactHtml(List<String> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) return "";
        return "<ul class=\"artifact-list\">" + artifacts.stream().map(a -> {
            String href = safeArtifactHref(a);
            if (href == null) return "<li><span class=\"sub\">" + esc(a) + "</span></li>";
            return "<li><a href=\"" + esc(href) + "\" target=\"_blank\" rel=\"noopener noreferrer\">" + esc(a) + "</a></li>";
        }).collect(Collectors.joining()) + "</ul>";
    }

    private String safeArtifactHref(String artifact) {
        if (artifact == null || artifact.isBlank()) return null;
        String value = artifact.trim();
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() != null) {
                String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
                return (scheme.equals("http") || scheme.equals("https")) ? value : null;
            }
        } catch (Exception ignored) {
            return null;
        }
        try {
            Path target = Path.of(value).toAbsolutePath().normalize();
            if (target.startsWith(dir) || target.startsWith(reportRoot)) {
                return dir.relativize(target).toString().replace('\\', '/');
            }
        } catch (Exception ignored) {
            return null;
        }
        return value.replace('\\', '/');
    }

    private void normalize(ExecutionResult result) {
        if (result.steps == null) result.steps = new ArrayList<>();
        if (result.failureAnalysis == null) result.failureAnalysis = "";
        for (ExecutionResult.StepResult step : result.steps) {
            if (step == null) continue;
            if (step.action == null) step.action = "";
            if (step.details == null) step.details = "";
            if (step.artifacts == null) step.artifacts = new ArrayList<>();
            if (step.durationMs < 0) step.durationMs = 0;
        }
        result.steps.removeIf(java.util.Objects::isNull);
    }

    private String csv(String value) {
        String v = value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    private String esc(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String formatMs(double value) {
        return String.format(Locale.ROOT, "%.1f ms", value);
    }

    private record Metrics(long passed, long failed, long totalDuration, long minDuration, long maxDuration,
                           double averageDuration, double passRate, long medianDuration, long p95Duration,
                           long fastCount, long normalCount, long slowCount, int fastPct, int normalPct, int slowPct) { }
}
