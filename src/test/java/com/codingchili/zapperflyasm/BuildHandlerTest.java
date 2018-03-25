package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.BuildHandler;
import com.codingchili.zapperflyasm.controller.ZapperConfig;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

import com.codingchili.core.context.*;
import com.codingchili.core.protocol.Serializer;
import com.codingchili.core.storage.AsyncStorage;
import com.codingchili.core.storage.JsonMap;
import com.codingchili.core.testing.RequestMock;
import com.codingchili.core.testing.ResponseListener;

import static com.codingchili.core.configuration.CoreStrings.ID_COLLECTION;
import static com.codingchili.core.protocol.ResponseStatus.*;
import static com.codingchili.zapperflyasm.model.BuildRequest.*;

/**
 * @author Robin Duda
 * <p>
 * Tests for the build handler.
 */
@RunWith(VertxUnitRunner.class)
public class BuildHandlerTest {
    private static final String BRANCH = "branch";
    private static final String REPOSITORY = "repository";
    public static final String TEST_ID = "TEST";
    public static final String TEST_DIR = "test_dir";
    private BuildHandler handler = new BuildHandler();
    private ZapperConfig config = ZapperConfig.get();
    private AsyncStorage<BuildConfiguration> configs;
    private AsyncStorage<BuildJob> jobs;
    private AsyncStorage<LogEvent> logs;
    private CoreContext core;

    @Before
    public void setup(TestContext test) {
        Async async = test.async();
        core = new SystemContext();
        config.setBuildPath("test/resources/");
        config.setTimeoutSeconds(5);
        handler.init(core);

        ZapperConfig.get().setStorage(JsonMap.class.getName());

        configs = new JsonMap<>(Future.future(), new StorageContextMock<>(BuildConfiguration.class));
        jobs = new JsonMap<>(Future.future(), new StorageContextMock<>(BuildJob.class));
        logs = new JsonMap<>(Future.future(), new StorageContextMock<>(LogEvent.class));

        ClusteredJobManager manager = new ClusteredJobManager(core, this.jobs, logs, this.configs);
        manager.setVCSProvider(new VCSMock(core));
        manager.setBuildExecutor(new BuildExecutorMock(true));

        BuildConfiguration build = new BuildConfiguration();
        build.setRepository(REPOSITORY);
        build.setBranch(BRANCH);
        manager.putConfig(build);

        logs.put(new LogEvent().setBuild(TEST_ID), (done) -> {});

        handler.setManager(manager);

        this.jobs.put(getTestBuild(), (done) -> async.complete());
    }

    private BuildJob getTestBuild() {
        BuildJob job = new BuildJob(new BuildConfiguration(), (j) -> {}, (j, l) -> {});
        job.setId(TEST_ID);
        job.setCommit("commit");
        job.setDirectory(TEST_DIR);
        job.setInstance("localhost");
        job.setStart(ZonedDateTime.now().toEpochSecond());
        Collection<String> lines = new ArrayList<>();
        lines.add("log line 1");
        lines.forEach(job::log);
        return job;
    }

    @After
    public void tearDown(TestContext test) {
        core.close(test.asyncAssertSuccess());
    }

    @Test
    public void submitBuild(TestContext test) {
        Async async = test.async();

        handler.build(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()
                .put(REPOSITORY, REPOSITORY)
                .put(BRANCH, BRANCH)));
    }

    @Test
    @Ignore("not supported - yet.")
    public void cancelBuild(TestContext test) {
        Async async = test.async();

        handler.cancel(request((response, status) -> {
            // not implemented yet - expect an error.
            test.assertEquals(ERROR, status);
            async.complete();
        }, new JsonObject().put(ID_BUILD, TEST_ID)));
    }

    @Test
    public void getBuildStatus(TestContext test) {
        Async async = test.async();

        handler.status(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            test.assertEquals(TEST_ID, response.getString(ID_BUILD));
            async.complete();
        }, new JsonObject()
                .put(ID_BUILD, TEST_ID)));
    }

    @Test
    public void getBuildLog(TestContext test) {
        Async async = test.async();

        handler.log(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);

            // logs should be included.
            test.assertFalse(response.getJsonArray(ID_COLLECTION).isEmpty());
            async.complete();
        }, new JsonObject()
                .put(ID_BUILD, TEST_ID)));
    }

    @Test
    public void removeBuild(TestContext test) {
        Async async = test.async();

        Paths.get(TEST_DIR).toFile().mkdirs();

        handler.remove(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()
                .put(ID_BUILD, TEST_ID)));
    }


    @Test
    public void listBuildHistory(TestContext test) {
        Async async = test.async();

        handler.list(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            test.assertTrue(response.getJsonArray(ID_LIST).size() > 0);
            async.complete();
        }, new JsonObject()));
    }

    @Test
    public void listBuildArtifacts(TestContext test) {
        Async async = test.async();

        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("repo");
        config.setBranch("mybranch");

        configs.put(config, (done) -> {
            handler.artifacts(request((response, status) -> {
                test.assertEquals(ACCEPTED, status);
                async.complete();
            }, new JsonObject()
                    .put(ID_BUILD, TEST_ID)));
        });
    }

    @Test
    public void configureBuild(TestContext test) {
        Async async = test.async();

        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("repo_test");
        config.setBranch("br_test");

        // and now attempt to remove the configured build.
        Runnable unconfigure = () -> {
            handler.unconfigure(request((response, status) -> {
                test.assertEquals(ACCEPTED, status);
                async.complete();
            }, new JsonObject()
                    .put(ID_REPO, config.getRepository())
                    .put(ID_BRANCH, config.getBranch())));
        };

        // configure the build.
        handler.configure(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            unconfigure.run();
        }, new JsonObject()
                .put(ID_CONFIG, Serializer.json(config))));
    }

    @Test
    public void listConfigurations(TestContext test) {
        Async async = test.async();

        handler.configurations(request((response, status) -> {
            test.assertTrue(response.getJsonArray(ID_COLLECTION).size() > 0);
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()));
    }

    private BuildRequest request(ResponseListener response, JsonObject data) {
        return new BuildRequest(RequestMock.get("n/a", response, data));
    }
}
