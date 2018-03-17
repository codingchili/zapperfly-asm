package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.*;
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
    private BuildJob job = new BuildJob(new BuildConfiguration());
    private BuildExecutor executor;

    @Before
    public void setUp() {
        job.setDirectory(TestConfig.TEST_DIR);
        core = new SystemContext();
        executor = new ProcessBuilderExecutor(core);
        ZapperConfig.get().setTimeoutSeconds(3);
    }

    @After
    public void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    public void testExecuteProcess(TestContext test) {
        Async async = test.async();

        setExecutable("build");
        executor.build(job).setHandler(build -> {
            test.assertTrue(build.succeeded());
            async.complete();
        });
    }

    @Test
    public void testExecuteFailedProcess(TestContext test) {
        Async async = test.async();

        setExecutable("failedbuild");
        executor.build(job).setHandler(build -> {
            test.assertTrue(build.failed());
            async.complete();
        });
    }

    @Test
    public void testExecuteTimeout(TestContext test) {
        Async async = test.async();

        setExecutable("timeout");
        executor.build(job).setHandler(build -> {
            test.assertTrue(build.failed());
            async.complete();
        });
    }

    private void setExecutable(String file) {
        String os = System.getProperty("os.name");

        System.err.println(os);

        if (os.toLowerCase().contains("windows")) {
            // assume cmd.exe exists on windows.
            job.setCmdLine("cmd.exe /C " + file + ".bat");
        } else {
            // assume bash exists on unix.
            job.setCmdLine("/bin/bash -E " + file + ".sh");
        }
    }
}
