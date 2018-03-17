package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.unit.TestContext;
import org.junit.*;
import org.junit.runner.RunWith;

import java.nio.file.Paths;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;

/**
 * @author Robin Duda
 * <p>
 * Tests for the git executor.
 */
@RunWith(VertxUnitRunner.class)
public class GitExecutorTest {
    private CoreContext core;
    private VersionControlSystem vcs;

    @Before
    public void setUp() {
        this.core = new SystemContext();
        this.vcs = new GitExecutor(core);
        ZapperConfig.get().setBuildPath(TestConfig.TEST_DIR);
    }

    @After
    public void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    @Ignore("Does not clean up files after it is done - some git index files are still locked after clone.")
    public void cloneRepository(TestContext test) {
        Async async = test.async();
        BuildJob job = new BuildJob(getConfig());
        vcs.clone(job).setHandler(clone -> {
            test.assertTrue(clone.succeeded());
            async.complete();
        });
    }

    @Test
    public void deleteDirectory(TestContext test) {
        Async async = test.async();

        BuildJob job = new BuildJob(new BuildConfiguration());
        job.setDirectory(TestConfig.TEST_DIR + "git-dir");
        Paths.get(job.getDirectory()).toFile().mkdirs();
        vcs.delete(job).setHandler(done -> {
            test.assertTrue(done.succeeded());
            async.complete();
        });
    }

    private BuildConfiguration getConfig() {
        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("https://github.com/codingchili/zapperfly-asm.git");
        config.setBranch("master");
        return config;
    }
}
