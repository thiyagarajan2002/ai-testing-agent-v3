package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.runtime.AgentExecutionException;
import com.thiyagarajan.agent.runtime.DataDrivenExecutionResult;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.TestOrchestrator;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import com.thiyagarajan.agent.model.TestPlan;

import java.util.Scanner;

/** Thin CLI entry point. Execution lifecycle is delegated to TestOrchestrator. */
public final class Main {
    private static final String VERSION = "3.16.0";
    private Main() { }
    public static void main(String[] args) { try { int code = run(args == null ? new String[0] : args); if (code != 0) System.exit(code); } catch (AgentExecutionException e) { System.err.println("Agent error [" + e.category() + "]: " + e.getMessage()); System.exit(2); } catch (Exception e) { System.err.println("Agent error [INFRASTRUCTURE]: " + e.getMessage()); System.exit(2); } }

    static int run(String[] args) throws Exception {
        Config config = Config.load(); ObjectMapper mapper = new ObjectMapper(); String environment = option(args, "--env");
        if (args.length >= 2 && "plan".equalsIgnoreCase(args[0])) try (TestOrchestrator o = new TestOrchestrator(config, mapper, environment)) { ExecutionResult r=o.executePlan(args[1]); printPlanResult(r,o); return r.passed()?0:1; }
        if (args.length >= 2 && "suite".equalsIgnoreCase(args[0])) try (TestOrchestrator o = new TestOrchestrator(config, mapper, environment)) { SuiteExecutionResult r=o.executeSuite(args[1]); printSuiteResult(r,o); return r.passed()?0:1; }
        if (args.length >= 2 && "data-driven".equalsIgnoreCase(args[0])) {
            if (args.length < 3) { System.err.println("Usage: data-driven <plan> <data-file> [--env <name>]"); return 2; }
            try (TestOrchestrator o = new TestOrchestrator(config, mapper, environment)) { DataDrivenExecutionResult r=o.executeDataDriven(args[1],args[2]); printDataDrivenResult(r,o); return r.passed()?0:1; }
        }
        if (args.length >= 2 && "validate".equalsIgnoreCase(args[0])) try (TestOrchestrator o = new TestOrchestrator(config, mapper, environment)) { if("plan".equalsIgnoreCase(args[1])&&args.length>=3)return validatePlan(o,args[2]); if("suite".equalsIgnoreCase(args[1])&&args.length>=3)return validateSuite(o,args[2]); return 2; }
        if (args.length>0 && "interactive".equalsIgnoreCase(args[0])) { interactive(config,mapper,environment); return 0; }
        printUsage(); return args.length==0?0:2;
    }
    private static int validatePlan(TestOrchestrator o,String file)throws Exception{var r=o.validatePlan(file);System.out.println("Plan preflight: "+(r.valid()?"VALID":"INVALID"));r.warnings().forEach(w->System.out.println("WARNING: "+w));r.errors().forEach(e->System.out.println("ERROR: "+e));return r.valid()?0:1;}
    private static int validateSuite(TestOrchestrator o,String file)throws Exception{var r=o.validateSuite(file);for(var p:r.plans()){System.out.println((p.valid()?"VALID":"INVALID")+"  "+p.file());p.warnings().forEach(w->System.out.println("  WARNING: "+w));p.errors().forEach(e->System.out.println("  ERROR: "+e));}System.out.println("Suite preflight: "+(r.valid()?"VALID":"INVALID"));return r.valid()?0:1;}
    private static void interactive(Config c,ObjectMapper m,String e)throws Exception{try(TestOrchestrator o=new TestOrchestrator(c,m,e);Scanner s=new Scanner(System.in)){System.out.println("=== AI Testing Agent v"+VERSION+" ===");while(true){System.out.print("\nRequirement> ");if(!s.hasNextLine())break;String q=s.nextLine();if("exit".equalsIgnoreCase(q.trim()))break;if(q.isBlank())continue;try{TestPlan p=o.plan(q);System.out.println(m.writerWithDefaultPrettyPrinter().writeValueAsString(p));ExecutionResult r=o.execute(p);o.analyzeIfFailed(p,r);o.writeReport(r);printPlanResult(r,o);}catch(Exception x){System.err.println("Agent error: "+x.getMessage());}}}}
    private static void printPlanResult(ExecutionResult r,TestOrchestrator o){System.out.println("Environment: "+(o.profile()==null?"default":o.profile().name));System.out.println("Result: "+(r.passed()?"PASSED":"FAILED"));System.out.println("Reports written to ./reports");}
    private static void printSuiteResult(SuiteExecutionResult r,TestOrchestrator o){int threads=Math.min(o.config().parallelism(),Math.max(1,r.totalTests));System.out.println("Suite: "+r.suiteName);System.out.println("Environment: "+(o.profile()==null?"default":o.profile().name));System.out.println("Execution mode: parallel | threads="+threads);System.out.println("Tests: "+r.totalTests+" | Passed: "+r.passedTests+" | Failed: "+r.failedTests);System.out.println("Duration: "+r.durationMs+" ms");}
    private static void printDataDrivenResult(DataDrivenExecutionResult r,TestOrchestrator o){System.out.println("Environment: "+(o.profile()==null?"default":o.profile().name));System.out.println("Data-driven test: "+r.testName);System.out.println("Iterations: "+r.totalIterations+" | Passed: "+r.passedIterations+" | Failed: "+r.failedIterations);System.out.println("Duration: "+r.durationMs+" ms");}
    private static String option(String[] a,String n){for(int i=0;i<a.length-1;i++)if(n.equalsIgnoreCase(a[i])){String v=a[i+1];return v.startsWith("--")?null:v;}return null;}
    private static void printUsage(){System.out.println("=== AI Testing Agent v"+VERSION+" ===");System.out.println("Commands:");System.out.println("  plan <file> [--env <name>]");System.out.println("  suite <file> [--env <name>]");System.out.println("  data-driven <plan> <data-file> [--env <name>]");System.out.println("  validate plan <file> [--env <name>]");System.out.println("  validate suite <file> [--env <name>]");System.out.println("  interactive [--env <name>]");}
}
