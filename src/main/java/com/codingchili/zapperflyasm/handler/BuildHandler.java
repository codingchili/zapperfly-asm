package com.codingchili.zapperflyasm.handler;

import com.codingchili.zapperflyasm.ZapperContext;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.building.BuildManager;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.configuration.CoreStrings.throwableToString;
import static com.codingchili.core.protocol.RoleMap.*;
import static com.codingchili.zapperflyasm.model.ApiRequest.*;

/**
 * @author Robin Duda
 * <p>
 * HTTP REST API for retrieving build status, scheduling and configuring builds.
 */
@Roles(USER)
@Address("builds")
@Description("Handles configuration and build requests.")
@DataModel(ApiRequest.class)
public class BuildHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private BuildManager manager;
    private ZapperContext core;

    @Override
    public void init(CoreContext core) {
        this.core = ZapperContext.ensure(core);
        this.manager = this.core.getJobManager();
        protocol.authenticator(this.core.authenticator()::getRoleByRequest);
    }

    @Api(PUBLIC)
    @Description("Schedules a new build on the given repo and branch.")
    public void submit(ApiRequest request) {
        core.getConfigurationManager().retrieveById(request.getBuildId())
                .setHandler(done -> {
                    if (done.succeeded()) {
                        BuildJob job = new BuildJob();
                        job.setConfig(done.result());
                        job.setAuthor(request.token().getDomain());

                        manager.submit(job).setHandler(request::result);
                    } else {
                        request.error(done.cause());
                    }
                });
    }

    @Api
    @Description("Cancels a scheduled build")
    public void cancel(ApiRequest request) {
        getJob(request, job -> manager.cancel(job).setHandler(request::result));
    }

    @Api
    @Description("Deletes the build files for the given job.")
    public void remove(ApiRequest request) {
        getJob(request, job -> manager.delete(job).setHandler(request::result));
    }

    @Api(ADMIN)
    @Description("Clears the build history")
    public void clear(ApiRequest request) {
        manager.clear().setHandler(clear -> {
            if (clear.succeeded()) {
                list(request);
            } else {
                request.error(clear.cause());
            }
        });
    }

    @Api
    @Description("Retrieves the build log for the running build with the given line offset.")
    public void log(ApiRequest request) {
        manager.logByIdWithOffset(request.getBuildId(), request.getLogOffset()).setHandler(request::result);
    }

    @Api(PUBLIC)
    @Description("Returns the build status for the given build ID without the build log.")
    public void status(ApiRequest request) {
        getJob(request, request::write);
    }

    @Api(PUBLIC)
    @Description("Lists all jobs that have been queued.")
    public void queued(ApiRequest request) {
        manager.queued().setHandler(done -> {
            if (done.succeeded()) {
                request.write(buildsToList(done.result()));
            } else {
                request.error(done.cause());
            }
        });
    }

    @Api(PUBLIC)
    @Description("Lists all builds that has been executed on the server.")
    public void list(ApiRequest request) {
        manager.history(request.getRepository(), request.getBranch()).setHandler(done -> {
            if (done.succeeded()) {
                request.write(buildsToList(done.result()));
            } else {
                request.error(done.cause());
            }
        });
    }

    private JsonObject buildsToList(Collection<BuildJob> jobs) {
        return new JsonObject().put(ID_LIST,
                jobs.stream()
                        .map(Serializer::json)
                        .peek(json -> json.remove(ID_DIRECTORY))
                        .collect(Collectors.toList()));
    }

    @Api
    @Description("Lists artifacts available for download.")
    public void artifacts(ApiRequest request) {
        getJob(request, (job) -> {
            if (job.getProgress().equals(Status.DONE)) {
                manager.artifacts(job).setHandler(request::result);
            } else {
                request.write(new ArrayList<>());
            }
        });
    }

    @Api(PUBLIC)
    @Description("Lists all executors/instances that has joined the cluster at some point.")
    public void instances(ApiRequest request) {
        manager.instances().setHandler(request::result);
    }

    private void getJob(ApiRequest request, Consumer<BuildJob> consumer) {
        manager.buildById(request.getBuildId()).setHandler(done -> {
            if (done.succeeded()) {
                consumer.accept(done.result());
            } else {
                request.error(new CoreRuntimeException(done.cause().getMessage()));
            }
        });
    }

    @Override
    public void handle(Request request) {
        protocol.process(new ApiRequest(request));
    }
}