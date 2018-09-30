package com.codingchili.zapperflyasm.handler;

import com.codingchili.zapperflyasm.ZapperContextMock;
import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.*;
import org.junit.runner.RunWith;

import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.*;

import com.codingchili.core.protocol.Serializer;
import com.codingchili.core.testing.RequestMock;
import com.codingchili.core.testing.ResponseListener;

import static com.codingchili.core.configuration.CoreStrings.ID_COLLECTION;
import static com.codingchili.core.protocol.ResponseStatus.*;
import static com.codingchili.zapperflyasm.model.ApiRequest.*;

/**
 * @author Robin Duda
 * <p>
 * Tests for the build handler.
 */
@RunWith(VertxUnitRunner.class)
public class BuildHandlerTest {
    private static final String BRANCH = "branch";
    private static final String REPOSITORY = "repository";
    private static final String TEST_ID = "TEST";
    private static final String TEST_DIR = "test_dir";
    private BuildHandler handler = new BuildHandler();
    private ZapperContextMock context;
    private String id = UUID.randomUUID().toString();

    @Before
    public void setup(TestContext test) {
        Async async = test.async();

        ZapperContextMock.create().setHandler(create -> {
            context = create.result();
            handler.init(context);

            context.addConfig(new BuildConfiguration(REPOSITORY, BRANCH).setId(id));
            context.log("testLine");
            context.addJob(getTestBuild());

            async.complete();
        });
    }

    private BuildJob getTestBuild() {
        BuildJob job = new BuildJob(new BuildConfiguration(REPOSITORY, BRANCH), (j) -> {
        }, (j, l) -> {
        });
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
        context.close(test.asyncAssertSuccess());
    }

    @Test
    public void submitBuild(TestContext test) {
        Async async = test.async();

        handler.submit(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()
                .put(ID, id)));
    }

    @Test
    @Ignore("not supported - yet.")
    public void cancelBuild(TestContext test) {
        Async async = test.async();

        handler.cancel(request((response, status) -> {
            // not implemented yet - expect an error.
            test.assertEquals(ERROR, status);
            async.complete();
        }, new JsonObject().put(ID, TEST_ID)));
    }

    @Test
    public void getBuildStatus(TestContext test) {
        Async async = test.async();

        handler.status(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            test.assertEquals(TEST_ID, response.getString(ID));
            async.complete();
        }, new JsonObject()
                .put(ID, TEST_ID)));
    }

    @Test
    public void getBuildLog(TestContext test) {
        Async async = test.async();

        handler.log(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);

            // logs should be included.
            test.assertFalse(response.getJsonArray(ID_COLLECTION).isEmpty());
            async.complete();
        }, Serializer.json(
                new BuildConfiguration()
                        .setBranch("master")
                        .setRepository("https://test")
                        .setCmdLine("./gradlew"))));
    }

    @Test
    public void clearBuildLog(TestContext test) {
        handler.clear(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
        }, new JsonObject()));
    }

    @Test
    public void getBuildQueue(TestContext test) {
        handler.queued(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
        }, new JsonObject()));
    }

    @Test
    public void removeBuild(TestContext test) {
        Async async = test.async();

        Paths.get(TEST_DIR).toFile().mkdirs();

        handler.remove(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()
                .put(ID, TEST_ID)));
    }


    @Ignore("StreamQuery<T> in core finds no results when there are no operators in the query.")
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

        context.addConfig(config);

        handler.artifacts(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()
                .put(ID, TEST_ID)));
    }

    @Test
    public void listInstances(TestContext test) {
        Async async = test.async();

        handler.instances(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            test.assertFalse(response.getJsonArray("collection").isEmpty());
            async.complete();
        }, new JsonObject()));
    }

    private ApiRequest request(ResponseListener response, JsonObject data) {
        return new ApiRequest(RequestMock.get("n/a", response, data));
    }
}
