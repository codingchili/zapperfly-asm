package com.codingchili.zapperflyasm.process;

import com.codingchili.zapperflyasm.TestConfig;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.building.BuildExecutor;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.BuildConfiguration;
import com.codingchili.zapperflyasm.building.ProcessBuilderExecutor;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;

/**
 * @author Robin Duda
 *
 * Tests for the process builder executor.
 */
@RunWith(VertxUnitRunner.class)
public class ProcessBuilderExecutorTest {
    private CoreContext core;
    private BuildJob job = new BuildJob();
    private BuildExecutor executor;

    @Before
    public void setUp() {
        job.setConfig(new BuildConfiguration());
        job.setDirectory(TestConfig.TEST_DIR);
        core = new SystemContext();
        executor = new ProcessBuilderExecutor(core);
        ZapperConfig.getEnvironment().setTimeoutSeconds(3);
    }

    @After
    public void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    public void testExecuteProcess(TestContext test) {
        Async async = test.async();

        job.getConfig().setCmdLine("./build");

        job.setLogger((job, line) -> {
            System.out.println(line);
        });

        executor.build(job).setHandler(build -> {
            test.assertTrue(build.succeeded());
            async.complete();
        });
    }

    @Test
    public void testExecuteFailedProcess(TestContext test) {
        Async async = test.async();

        job.getConfig().setCmdLine("./failedbuild");
        executor.build(job).setHandler(build -> {
            test.assertTrue(build.failed());
            test.assertEquals(Status.FAILED, job.getProgress());
            async.complete();
        });
    }

    @Test
    public void testExecuteTimeout(TestContext test) {
        Async async = test.async();

        job.getConfig().setCmdLine("./timeout");
        executor.build(job).setHandler(build -> {
            test.assertTrue(build.succeeded());
            test.assertEquals(Status.FAILED, job.getProgress());
            async.complete();
        });
    }
}
