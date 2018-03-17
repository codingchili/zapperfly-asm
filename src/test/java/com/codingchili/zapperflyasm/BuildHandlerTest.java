package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.BuildHandler;
import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.SimpleJobManager;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.ext.unit.TestContext;
import org.junit.*;
import org.junit.runner.RunWith;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.SystemContext;

/**
 * @author Robin Duda
 *
 * Tests for the build handler.
 */
@RunWith(VertxUnitRunner.class)
public class BuildHandlerTest {
    private BuildHandler handler = new BuildHandler();
    private ZapperConfig config = ZapperConfig.get();
    private CoreContext core;

    @Before
    void setup(TestContext test) {
        core = new SystemContext();
        config.setBuildPath("test/resources/");
        config.setTimeoutSeconds(5);
        handler.init(core);

        SimpleJobManager manager = new SimpleJobManager(core);
        manager.setVCSProvider(new VCSMock(core));

        handler.setManager(manager);
    }

    @After
    void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    void submitBuild() {
        handler.build(null);
    }

    @Test
    void testBuildTimeouts() {
        handler.build(null);
    }

    @Test
    void cancelBuild() {
        handler.cancel(null);
    }

    @Test
    void getBuildStatus() {
        handler.status(null);
    }

    @Test
    void removeBuild() {
        handler.remove(null);
    }

    @Test
    void listBuildArtifacts() {
        handler.list(null);
    }

    @Test
    void getBuildLog() {
        handler.log(null);
    }

    @Test
    void configureBuild() {
        handler.configure(null);
    }

    @Test
    void unconfigureBuild() {
        handler.unconfigure(null);
    }
}
