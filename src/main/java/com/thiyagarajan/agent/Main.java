package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.cli.CliParser;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.io.RunLogManager;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.plugin.PluginManager;
import com.thiyagarajan.agent.runtime.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class Main {
    private static final String VERSION = "3.39.0";
    private Main() {}
    public static void main(String[] a) {
        String[] args = a == null ? new String[0] : a; int exitCode = 0;
        try {
            Config config = Config.load(); String area = logArea(args); String command = args.length == 0 ? "help" : args[0];
            try (RunContext context = RunContext.start(); RunLogManager logs = RunLogManager.start(config.reportsDir(), area, command, context.runId())) {
                logs.log("Run ID: " + context.runId()); logs.log("Command: " + String.join(" ", args));
                try { exitCode = run(args); } catch (AgentExecutionException e) { logs.log("Agent error [" + e.category() + "]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                catch (Exception e) { logs.log("Agent error [INFRASTRUCTURE]: " + e.getMessage()); e.printStackTrace(System.err); exitCode = 2; }
                logs.log("Exit code: " + exitCode);
            }
        } catch (Exception e) { System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage()); exitCode = 2; }
        if (exitCode != 0) System.exit(exitCode);
    }
    static int run(String[] args) throws Exception {
        CliParser cli = CliParser.parse(args);
        if (cli.versionRequested()) { System.out.println("AI Testing Agent v" + VERSION); return 0; }
        if (cli.helpRequested()) { usage(); return 0; }
        Config c = Config.load(); ObjectMapper m = new ObjectMapper(); String env = cli.option("--env");
        return switch (cli.command()) {
            case "history" -> history(c, cli.option("--limit"));
            case "plugins" -> plugins();
            case "plan" -> executePlan(c, m, env, cli);
            case "suite" -> executeSuite(c, m, env, cli);
            case "data-driven" -> dataDriven(c, m, env, cli);
            case "validate" -> validate(c, m, env, cli);
            case "interactive" -> { interactive(c, m, env); yield 0; }
            default -> 2;
        };
    }
    private static int executePlan(Config c, ObjectMapper m, String env, CliParser cli) throws Exception { String[] p=cli.positional(); if(p.length<1) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,"Usage: plan <file>"); try(TestOrchestrator o=new TestOrchestrator(c,m,env)){return o.executePlan(p[0]).passed()?0:1;} }
    private static int executeSuite(Config c, ObjectMapper m, String env, CliParser cli) throws Exception { String[] p=cli.positional(); if(p.length<1) throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,"Usage: suite <file>"); try(TestOrchestrator o=new TestOrchestrator(c,m,env)){return o.executeSuite(p[0]).passed()?0:1;} }
    private static int dataDriven(Config c,ObjectMapper m,String env,CliParser cli)throws Exception{String[] p=cli.positional();if(p.length<2)throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,"Usage: data-driven <plan> <data-file>");int workers=cli.option("--parallelism")==null?c.parallelism():Integer.parseInt(cli.option("--parallelism"));Map<String,String> f=new LinkedHashMap<>();if(cli.option("--filter")!=null){String[] x=cli.option("--filter").split("=",2);if(x.length!=2)throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION,"Filter must use key=value");f.put(x[0],x[1]);}try(TestOrchestrator o=new TestOrchestrator(c,m,env)){var r=o.executeDataDriven(p[0],p[1],f,workers);System.out.println("Data-driven: "+r.testName+" | Iterations: "+r.totalIterations+" | Passed: "+r.passedIterations+" | Failed: "+r.failedIterations);return r.passed()?0:1;}}
    private static int validate(Config c,ObjectMapper m,String env,CliParser cli)throws Exception{String[]p=cli.positional();if(p.length<2)throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,"Usage: validate <plan|suite> <file>");try(TestOrchestrator o=new TestOrchestrator(c,m,env)){if("plan".equalsIgnoreCase(p[0]))return o.validatePlan(p[1]).valid()?0:1;if("suite".equalsIgnoreCase(p[0]))return o.validateSuite(p[1]).valid()?0:1;throw new AgentExecutionException(AgentExecutionException.Category.CONFIGURATION,"Validation target must be plan or suite");}}
    private static int plugins(){PluginManager p=new PluginManager();var plugins=p.discover();System.out.println("=== Plugins ===");if(plugins.isEmpty())System.out.println("No plugins discovered.");else plugins.forEach(x->System.out.println(x.id()+" | "+x.version()));return 0;}
    private static int history(Config c,String limitText)throws Exception{int limit=limitText==null?10:Integer.parseInt(limitText);Path root=Path.of(c.reportsDir(),"history").toAbsolutePath().normalize();Path index=root.resolve("index.json");if(!Files.isRegularFile(index)){System.out.println("No execution history found.");return 0;}ObjectMapper mapper=new ObjectMapper().findAndRegisterModules();List<RunHistoryManager.RunSummary> all=mapper.readValue(index.toFile(),new com.fasterxml.jackson.core.type.TypeReference<List<RunHistoryManager.RunSummary>>(){});all.stream().skip(Math.max(0,all.size()-limit)).forEach(r->System.out.println(r.runId+" | "+r.suiteName+" | "+r.status+" | "+r.passRate+"% | "+r.durationMs+" ms"));return 0;}
    private static String logArea(String[] a){return a.length>0?a[0]:"terminal";}
    private static void interactive(Config c,ObjectMapper m,String e)throws Exception{try(TestOrchestrator o=new TestOrchestrator(c,m,e);Scanner s=new Scanner(System.in)){while(true){System.out.print("\nRequirement> ");if(!s.hasNextLine())break;String q=s.nextLine();if("exit".equalsIgnoreCase(q.trim()))break;if(q.isBlank())continue;try{TestPlan p=o.plan(q);ExecutionResult r=o.execute(p);o.analyzeIfFailed(p,r);o.writeReport(r);}catch(Exception x){System.err.println("Agent error: "+x.getMessage());}}}}
    private static void usage(){System.out.println("=== AI Testing Agent v"+VERSION+" ===\nCommands:\n  plan <file> [--env <name>]\n  suite <file> [--env <name>]\n  data-driven <plan> <data-file> [--filter key=value] [--parallelism <N>] [--env <name>]\n  history [--limit <N>]\n  validate plan <file> [--env <name>]\n  validate suite <file> [--env <name>]\n  plugins\n  interactive [--env <name>]\n  version\n  help\n\nGlobal options: --help, --version");}
}
