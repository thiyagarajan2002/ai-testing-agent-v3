package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.ai.OllamaClient;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.config.EnvironmentManager;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import com.thiyagarajan.agent.report.ReportManager;
import com.thiyagarajan.agent.report.SuiteReportManager;
import com.thiyagarajan.agent.runtime.AgentRunner;
import com.thiyagarajan.agent.runtime.ExecutionResult;
import com.thiyagarajan.agent.runtime.HistoryAnalyticsManager;
import com.thiyagarajan.agent.runtime.RunHistoryManager;
import com.thiyagarajan.agent.runtime.SuiteExecutionEngine;
import com.thiyagarajan.agent.runtime.SuiteExecutionResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class Main {
    private static final String VERSION = "3.12.0";

    public static void main(String[] args) throws Exception {
        Config config=Config.load(); ObjectMapper mapper=new ObjectMapper(); OllamaClient llm=new OllamaClient(config.ollamaUrl(),config.ollamaModel()); AgentRunner runner=new AgentRunner(llm,mapper); ReportManager reports=new ReportManager();
        String env=option(args,"--env"); EnvironmentManager environments=new EnvironmentManager(mapper); EnvironmentProfile profile=env==null?null:environments.load(env,Path.of("."));
        if(args.length>=2&&"plan".equalsIgnoreCase(args[0])){executeFile(args[1],mapper,runner,reports,profile,environments);return;}
        if(args.length>=2&&"suite".equalsIgnoreCase(args[0])){executeSuite(args[1],mapper,runner,config,profile,environments);return;}
        System.out.println("=== AI Testing Agent v"+VERSION+" ==="); System.out.println("Commands: plan <file> [--env <name>] | suite <file> [--env <name>] | interactive | exit"); interactive(mapper,runner,reports);
    }
    private static String option(String[] args,String name){for(int i=0;i<args.length-1;i++)if(name.equalsIgnoreCase(args[i]))return args[i+1];return null;}
    private static void executeFile(String file,ObjectMapper mapper,AgentRunner runner,ReportManager reports,EnvironmentProfile profile,EnvironmentManager environments)throws Exception{
        Path path=Path.of(file); if(!Files.isRegularFile(path))throw new IllegalArgumentException("Plan file not found: "+file); TestPlan plan=mapper.readValue(Files.readString(path),TestPlan.class); if(profile!=null)environments.apply(plan,profile);
        ExecutionResult result=runner.execute(plan); if(!result.passed())try{runner.analyzeFailure(plan,result);}catch(Exception e){System.err.println("AI failure analysis unavailable: "+e.getMessage());} reports.writeAll(result);
        System.out.println("Environment: "+(profile==null?"default":profile.name)); System.out.println("Result: "+(result.passed()?"PASSED":"FAILED")); System.out.println("Reports written to ./reports"); if(!result.passed())System.exit(1);
    }
    private static void executeSuite(String file,ObjectMapper mapper,AgentRunner runner,Config config,EnvironmentProfile profile,EnvironmentManager environments)throws Exception{
        Path suitePath=Path.of(file).toAbsolutePath().normalize(); if(!Files.isRegularFile(suitePath))throw new IllegalArgumentException("Suite file not found: "+file); TestSuite suite=mapper.readValue(Files.readString(suitePath),TestSuite.class); Path dir=suitePath.getParent()==null?Path.of(".").toAbsolutePath():suitePath.getParent();
        SuiteExecutionEngine engine=new SuiteExecutionEngine(mapper,runner,config.parallelism(),environments,profile); SuiteExecutionResult result=engine.execute(suite,dir); Path reports=Path.of(config.reportsDir(),"suite").toAbsolutePath().normalize(); new SuiteReportManager(reports).writeAll(result);
        try {
            Path history=Path.of(config.reportsDir(),"history").toAbsolutePath().normalize();
            RunHistoryManager.RecordedRun recorded = new RunHistoryManager(history).recordSuite(result);
            HistoryAnalyticsManager.AnalyticsReport analytics = new HistoryAnalyticsManager(history).writeReports();
            System.out.println("History run: "+recorded.runId());
            System.out.println("Regressions: "+recorded.comparison().regressions+" | Fixed: "+recorded.comparison().fixed);
            System.out.println("Flaky tests: "+analytics.flakyTests.size());
            System.out.println("History dashboard: "+history.resolve("index.html"));
            System.out.println("Analytics dashboard: "+history.resolve("analytics.html"));
        } catch (Exception e) {
            System.err.println("History/analytics unavailable: "+e.getMessage());
        }
        System.out.println("Suite: "+suite.name); System.out.println("Environment: "+(profile==null?"default":profile.name)); System.out.println("Execution mode: parallel | threads="+Math.min(config.parallelism(),Math.max(1,result.totalTests))); System.out.println("Tests: "+result.totalTests+" | Passed: "+result.passedTests+" | Failed: "+result.failedTests); System.out.println("Duration: "+result.durationMs+" ms"); System.out.println("Suite reports: "+reports); if(!result.passed())System.exit(1);
    }
    private static void interactive(ObjectMapper mapper,AgentRunner runner,ReportManager reports)throws Exception{
        System.out.println("Enter a testing requirement. Type 'exit' to quit."); try(Scanner scanner=new Scanner(System.in)){while(true){System.out.print("\nRequirement> ");if(!scanner.hasNextLine())break;String requirement=scanner.nextLine();if("exit".equalsIgnoreCase(requirement.trim()))break;if(requirement.isBlank())continue;try{TestPlan plan=runner.plan(requirement);System.out.println("\nGenerated plan:");System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(plan));ExecutionResult result=runner.execute(plan);if(!result.passed())try{runner.analyzeFailure(plan,result);System.out.println("AI analysis: "+result.failureAnalysis());}catch(Exception e){System.err.println("AI failure analysis unavailable: "+e.getMessage());}reports.writeAll(result);System.out.println("\nResult: "+(result.passed()?"PASSED":"FAILED"));System.out.println("Reports written to ./reports");}catch(Exception e){System.err.println("Agent error: "+e.getMessage());}}}
    }
}
