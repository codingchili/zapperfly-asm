package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.reactivex.ext.unit.TestContext;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;

/**
 * @author Robin Duda
 * <p>
 * Tests for the git executor.
 */
@RunWith(VertxUnitRunner.class)
public class GitExecutorIT {
    private CoreContext core;
    private VersionControlSystem vcs;

    @Before
    void setUp(TestContext test) {
        this.core = new SystemContext();
        this.vcs = new GitExecutor(core);
        ZapperConfig.get().setBuildPath("src/test/resources/");
    }

    @After
    void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    void cloneRepository(TestContext test) {
        BuildJob job = new BuildJob(getConfig());
        vcs.clone(job).setHandler(clone -> {
            if (clone.failed()) {
                throw new RuntimeException(clone.cause());
            }
            test.assertTrue(clone.succeeded());
        });
    }


    private BuildConfiguration getConfig() {
        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("https://github.com/codingchili/zapperfly-asm.git");
        config.setBranch("master");
        return config;
    }
}
