package com.thiyagarajan.agent.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thiyagarajan.agent.runtime.DataDrivenExecutionResult;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.SecurityRedactor;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

/** Writes aggregate and per-iteration reports for data-driven executions. */
public final class DataDrivenReportManager {
    private final ObjectMapper mapper;
    private final Path dir;

    public DataDrivenReportManager(ObjectMapper mapper, Path directory) throws Exception {
        if (mapper == null || directory == null) throw new IllegalArgumentException("Mapper and report directory are required");
        this.mapper = mapper; this.dir = directory.toAbsolutePath().normalize(); Files.createDirectories(this.dir);
    }
    public Path writeAll(DataDrivenExecutionResult result) throws Exception { if(result==null)throw new IllegalArgumentException("Data-driven result cannot be null");writeJson(result);writeCsv(result);writeHtml(result);return dir.resolve("data-driven-report.html"); }
    private void writeJson(DataDrivenExecutionResult result)throws Exception{JsonNode root=mapper.valueToTree(result);JsonNode its=root.get("iterations");if(its!=null&&its.isArray())for(JsonNode it:its){JsonNode data=it.get("data");if(data!=null&&data.isObject()){ObjectNode o=(ObjectNode)data;o.fieldNames().forEachRemaining(k->{if(SecurityRedactor.isSensitiveKey(k))o.put(k,SecurityRedactor.MASK);else if(o.get(k)!=null&&o.get(k).isTextual())o.put(k,SecurityRedactor.redactText(o.get(k).asText()));});}}Files.writeString(dir.resolve("data-driven-report.json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));}
    private void writeCsv(DataDrivenExecutionResult result)throws Exception{try(BufferedWriter w=Files.newBufferedWriter(dir.resolve("data-driven-report.csv"))){w.write("iteration,status,duration_ms,data,failure_analysis,step_count\n");for(var i:result.iterations){ExecutionResult e=i.execution;String failure=e==null?"":e.failureAnalysis();int steps=e==null||e.steps==null?0:e.steps.size();w.write(i.index+","+(i.passed?"PASS":"FAIL")+","+i.durationMs+","+csv(SecurityRedactor.redactMap(i.data))+","+csv(failure)+","+steps+"\n");}}}
    private void writeHtml(DataDrivenExecutionResult result)throws Exception{String rows=result.iterations.stream().map(i->{ExecutionResult e=i.execution;String failure=e==null?"Iteration produced no execution result":e.failureAnalysis();String steps=e==null||e.steps==null?"":e.steps.stream().map(s->"<li><b>"+esc(s.action)+"</b> — "+(s.passed?"PASS":"FAIL")+" — "+s.durationMs+" ms — "+esc(s.details)+"</li>").collect(Collectors.joining());return "<tr><td>"+i.index+"</td><td><b>"+(i.passed?"PASS":"FAIL")+"</b></td><td>"+i.durationMs+" ms</td><td><pre>"+esc(SecurityRedactor.redactMap(i.data))+"</pre></td><td>"+(i.passed?"":"<pre>"+esc(failure)+"</pre>")+"<details><summary>Step diagnostics</summary><ul>"+steps+"</ul></details></td></tr>";}).collect(Collectors.joining());double rate=result.totalIterations==0?0.0:result.passedIterations*100.0/result.totalIterations;String html="""
<!doctype html><html><head><meta charset="UTF-8"><title>Data-Driven Test Dashboard</title><style>body{font-family:Arial;margin:30px;background:#f7f7f7;color:#222}.cards{display:flex;gap:12px;flex-wrap:wrap}.card{background:white;border:1px solid #ddd;border-radius:8px;padding:16px;min-width:150px}.value{font-size:24px;font-weight:bold}table{border-collapse:collapse;width:100%%;background:white}th,td{border:1px solid #ccc;padding:8px;text-align:left;vertical-align:top}th{background:#eee}pre{white-space:pre-wrap;margin:4px 0}details{margin-top:8px}</style></head><body><h1>Data-Driven Test Dashboard</h1><p><b>Test:</b> %s</p><p><b>Status:</b> %s</p><div class="cards"><div class="card">Iterations<div class="value">%d</div></div><div class="card">Passed<div class="value">%d</div></div><div class="card">Failed<div class="value">%d</div></div><div class="card">Pass Rate<div class="value">%.1f%%</div></div><div class="card">Wall Duration<div class="value">%d ms</div></div><div class="card">Mode<div class="value">%s</div></div><div class="card">Workers<div class="value">%d</div></div><div class="card">Avg Iteration<div class="value">%.1f ms</div></div><div class="card">Est. Speedup<div class="value">%.2fx</div></div></div><p><a href="data-driven-report.json">JSON</a> | <a href="data-driven-report.csv">CSV</a></p><h2>Performance</h2><p>Estimated sequential work: <b>%d ms</b>. Actual wall-clock duration: <b>%d ms</b>. Speedup is an approximation based on measured iteration durations.</p><h2>Iteration Results</h2><table><tr><th>#</th><th>Status</th><th>Duration</th><th>Dataset</th><th>Diagnostics</th></tr>%s</table></body></html>
""".formatted(esc(result.testName),result.passed()?"PASS":"FAIL",result.totalIterations,result.passedIterations,result.failedIterations,rate,result.durationMs,esc(result.executionMode),result.parallelism,result.averageIterationDurationMs(),result.estimatedSpeedup,result.estimatedSequentialDurationMs,result.durationMs,rows);Files.writeString(dir.resolve("data-driven-report.html"),html);}
    private String csv(String v){return v==null?"":"\""+v.replace("\"","\"\"").replace("\n"," ")+"\"";}private String esc(String v){return v==null?"":v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");}
}
