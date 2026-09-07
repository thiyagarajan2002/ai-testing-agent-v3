package com.thiyagarajan.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.Config;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.runtime.*;
import java.util.*;

public final class Main {
 private static final String VERSION="3.18.0"; private Main(){}
 public static void main(String[] a){try{int c=run(a==null?new String[0]:a);if(c!=0)System.exit(c);}catch(AgentExecutionException e){System.err.println("Agent error ["+e.category()+"]: "+e.getMessage());System.exit(2);}catch(Exception e){System.err.println("Agent error [INFRASTRUCTURE]: "+e.getMessage());System.exit(2);}}
 static int run(String[] a)throws Exception{Config c=Config.load();ObjectMapper m=new ObjectMapper();String env=option(a,"--env");
  if(a.length>=2&&"plan".equalsIgnoreCase(a[0]))try(TestOrchestrator o=new TestOrchestrator(c,m,env)){var r=o.executePlan(a[1]);return r.passed()?0:1;}
  if(a.length>=2&&"suite".equalsIgnoreCase(a[0]))try(TestOrchestrator o=new TestOrchestrator(c,m,env)){var r=o.executeSuite(a[1]);return r.passed()?0:1;}
  if(a.length>=3&&"data-driven".equalsIgnoreCase(a[0]))try(TestOrchestrator o=new TestOrchestrator(c,m,env)){var r=o.executeDataDriven(a[1],a[2],filters(a));System.out.println("Data-driven: "+r.testName+" | Iterations: "+r.totalIterations+" | Passed: "+r.passedIterations+" | Failed: "+r.failedIterations);System.out.println("Reports: ./reports/data-driven/"+safe(r.testName)+"/");return r.passed()?0:1;}
  if(a.length>=2&&"validate".equalsIgnoreCase(a[0]))try(TestOrchestrator o=new TestOrchestrator(c,m,env)){if("plan".equalsIgnoreCase(a[1])&&a.length>=3){var r=o.validatePlan(a[2]);return r.valid()?0:1;}if("suite".equalsIgnoreCase(a[1])&&a.length>=3){var r=o.validateSuite(a[2]);return r.valid()?0:1;}return 2;}
  if(a.length>0&&"interactive".equalsIgnoreCase(a[0])){interactive(c,m,env);return 0;}usage();return a.length==0?0:2; }
 private static Map<String,String> filters(String[] a){Map<String,String> f=new LinkedHashMap<>();for(int i=0;i<a.length-1;i++)if("--filter".equalsIgnoreCase(a[i])){String[] p=a[i+1].split("=",2);if(p.length!=2||p[0].isBlank())throw new AgentExecutionException(AgentExecutionException.Category.PLAN_VALIDATION,"Filter must use key=value");f.put(p[0],p[1]);}return f;}
 private static String option(String[]a,String n){for(int i=0;i<a.length-1;i++)if(n.equalsIgnoreCase(a[i]))return a[i+1].startsWith("--")?null:a[i+1];return null;}
 private static String safe(String v){String s=v==null||v.isBlank()?"data-driven-test":v.replaceAll("[^a-zA-Z0-9._-]+","_");return s.length()>80?s.substring(0,80):s;}
 private static void interactive(Config c,ObjectMapper m,String e)throws Exception{try(TestOrchestrator o=new TestOrchestrator(c,m,e);Scanner s=new Scanner(System.in)){while(true){System.out.print("\nRequirement> ");if(!s.hasNextLine())break;String q=s.nextLine();if("exit".equalsIgnoreCase(q.trim()))break;if(q.isBlank())continue;try{TestPlan p=o.plan(q);ExecutionResult r=o.execute(p);o.analyzeIfFailed(p,r);o.writeReport(r);}catch(Exception x){System.err.println("Agent error: "+x.getMessage());}}}}
 private static void usage(){System.out.println("=== AI Testing Agent v"+VERSION+" ===\nCommands:\n  plan <file> [--env <name>]\n  suite <file> [--env <name>]\n  data-driven <plan> <data-file> [--filter key=value] [--env <name>]\n  validate plan <file> [--env <name>]\n  validate suite <file> [--env <name>]\n  interactive [--env <name>]");}
}
