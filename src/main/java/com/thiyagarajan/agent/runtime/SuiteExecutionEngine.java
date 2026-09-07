package com.thiyagarajan.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiyagarajan.agent.config.EnvironmentManager;
import com.thiyagarajan.agent.model.EnvironmentProfile;
import com.thiyagarajan.agent.model.TestPlan;
import com.thiyagarajan.agent.model.TestSuite;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Executes independent test plans sequentially or in parallel with failure isolation. */
public final class SuiteExecutionEngine {
    private final ObjectMapper mapper;
    private final AgentRunner runner;
    private final int parallelism;
    private final EnvironmentManager environments;
    private final EnvironmentProfile profile;

    public SuiteExecutionEngine(ObjectMapper mapper, AgentRunner runner, int parallelism) {
        this(mapper, runner, parallelism, null, null);
    }

    public SuiteExecutionEngine(ObjectMapper mapper, AgentRunner runner, int parallelism, EnvironmentManager environments, EnvironmentProfile profile) {
        if (mapper == null) throw new IllegalArgumentException("ObjectMapper cannot be null");
        if (runner == null) throw new IllegalArgumentException("Runner cannot be null");
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism must be at least 1");
        this.mapper = mapper; this.runner = runner; this.parallelism = parallelism;
        this.environments = environments; this.profile = profile;
    }

    public SuiteExecutionResult execute(TestSuite suite, Path suiteDirectory) throws Exception {
        validateSuite(suite, suiteDirectory);
        long started = System.currentTimeMillis();
        SuiteExecutionResult out = new SuiteExecutionResult(); out.suiteName = suite.name;
        List<Callable<SuiteExecutionResult.TestExecution>> tasks = new ArrayList<>();
        for (int i=0;i<suite.plans.size();i++){ final int index=i; final String file=suite.plans.get(i); tasks.add(()->executeOne(index,file,suiteDirectory)); }
        ExecutorService executor=Executors.newFixedThreadPool(Math.min(parallelism,tasks.size()));
        try { for(Future<SuiteExecutionResult.TestExecution> f:executor.invokeAll(tasks)){ try{out.tests.add(f.get());}catch(ExecutionException e){Throwable c=e.getCause()==null?e:e.getCause();out.tests.add(failedInfrastructureResult(out.tests.size(),"unknown",c));} } }
        finally { executor.shutdown(); }
        out.tests.sort(java.util.Comparator.comparingInt(t->t.index)); out.totalTests=out.tests.size();
        out.passedTests=(int)out.tests.stream().filter(t->"PASS".equals(t.status)).count(); out.failedTests=out.totalTests-out.passedTests;
        out.status=out.failedTests==0?"PASS":"FAIL"; out.durationMs=System.currentTimeMillis()-started; out.completedAt=Instant.now().toString(); return out;
    }

    private SuiteExecutionResult.TestExecution executeOne(int index,String planFile,Path dir)throws Exception{
        Path resolved=resolvePlan(dir,planFile); long started=System.currentTimeMillis();
        TestPlan plan=mapper.readValue(Files.readString(resolved),TestPlan.class);
        if(environments!=null && profile!=null) environments.apply(plan,profile);
        ExecutionResult result=runner.execute(plan);
        if(!result.passed()) try{runner.analyzeFailure(plan,result);}catch(Exception e){result.failureAnalysis("AI failure analysis unavailable: "+e.getMessage());}
        return new SuiteExecutionResult.TestExecution(index,planFile,plan.name,result.passed()?"PASS":"FAIL",System.currentTimeMillis()-started,result);
    }
    private SuiteExecutionResult.TestExecution failedInfrastructureResult(int i,String file,Throwable e){ExecutionResult r=new ExecutionResult();r.testName=file;r.passed=false;r.failureAnalysis="Suite execution error: "+e;r.steps.add(new ExecutionResult.StepResult("suite-execution",false,e.toString(),0));return new SuiteExecutionResult.TestExecution(i,file,file,"FAIL",0,r);}
    private void validateSuite(TestSuite s,Path dir){if(s==null)throw new IllegalArgumentException("Suite cannot be null");if(s.plans==null||s.plans.isEmpty())throw new IllegalArgumentException("Suite contains no plans");if(dir==null)throw new IllegalArgumentException("Suite directory is required");for(String f:s.plans){if(f==null||f.isBlank())throw new IllegalArgumentException("Suite contains a blank plan path");resolvePlan(dir,f);}}
    private Path resolvePlan(Path dir,String file){Path root=dir.toAbsolutePath().normalize(),resolved=root.resolve(file).normalize();if(!resolved.startsWith(root))throw new IllegalArgumentException("Plan path escapes suite directory: "+file);if(!Files.isRegularFile(resolved))throw new IllegalArgumentException("Plan file not found: "+resolved);return resolved;}
}
