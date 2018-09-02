package com.codingchili.zapperflyasm.vcs;

import com.codingchili.zapperflyasm.TestConfig;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.vcs.GitExecutor;
import com.codingchili.zapperflyasm.vcs.VersionControlSystem;
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
        ZapperConfig.get().getEnvironment().setBuildPath(TestConfig.TEST_DIR);
    }

    @After
    public void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    @Ignore("Does not clean up files after it is done - some git index files are still locked after clone.")
    public void cloneRepository(TestContext test) {
        Async async = test.async();
        BuildJob job = new BuildJob();
        job.setConfig(getConfig());
        vcs.clone(job).setHandler(clone -> {
            test.assertTrue(clone.succeeded());
            async.complete();
        });
    }

    @Test
    public void deleteDirectory(TestContext test) {
        Async async = test.async();

        BuildJob job = new BuildJob();
        job.setConfig(new BuildConfiguration());
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
