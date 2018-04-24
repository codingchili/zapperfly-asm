package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.controller.ConfigurationHandler;
import com.codingchili.zapperflyasm.model.ApiRequest;
import com.codingchili.zapperflyasm.model.BuildConfiguration;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.codingchili.core.protocol.Serializer;
import com.codingchili.core.testing.RequestMock;
import com.codingchili.core.testing.ResponseListener;

import static com.codingchili.core.configuration.CoreStrings.ID_COLLECTION;
import static com.codingchili.core.protocol.ResponseStatus.ACCEPTED;
import static com.codingchili.zapperflyasm.model.ApiRequest.*;

/**
 * @author Robin Duda
 *
 * Handler tests for the configuration handler.
 */
@RunWith(VertxUnitRunner.class)
public class ConfigurationHandlerTest {
    private static final String BRANCH = "BRANCH";
    private static final String REPO = "https://REPO/";
    private ConfigurationHandler handler;

    @Before
    public void setUp(TestContext context) {
        Async async = context.async();

        ZapperContextMock.create().setHandler(done -> {
            ZapperContextMock mock = done.result();

            mock.addConfig(new BuildConfiguration(REPO, BRANCH));

            handler = new ConfigurationHandler();
            handler.init(mock);
            async.complete();
        });
    }

    @Test
    public void configureBuild(TestContext test) {
        Async async = test.async();

        BuildConfiguration config = new BuildConfiguration();
        config.setRepository("https://repo/");
        config.setBranch("br_test");
        config.setCmdLine("./build");
        config.setDockerImage("anapsix/alpine-java");

        // and now attempt to remove the configured build.
        Runnable unconfigure = () -> {
            handler.remove(request((response, status) -> {
                test.assertEquals(ACCEPTED, status);
                async.complete();
            }, new JsonObject()
                    .put(ID_REPO, config.getRepository())
                    .put(ID_BRANCH, config.getBranch())));
        };

        // configure the build.
        handler.add(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            unconfigure.run();
        }, new JsonObject()
                .put(ID_CONFIG, Serializer.json(config))));
    }

    @Test
    public void listConfigurations(TestContext test) {
        Async async = test.async();

        handler.list(request((response, status) -> {
            test.assertTrue(response.getJsonArray(ID_COLLECTION).size() > 0);
            test.assertEquals(ACCEPTED, status);
            async.complete();
        }, new JsonObject()));
    }

    @Test
    public void listClusterConfig(TestContext test) {
        Async async = test.async();

        handler.cluster(request((response, status) -> {
            test.assertEquals(ACCEPTED, status);
            test.assertTrue(response.containsKey("groupName"));
            async.complete();
        }, new JsonObject()));
    }

    private ApiRequest request(ResponseListener response, JsonObject data) {
        return new ApiRequest(RequestMock.get("n/a", response, data));
    }
}
