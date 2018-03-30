package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

import static com.codingchili.core.configuration.CoreStrings.throwableToString;
import static com.codingchili.core.protocol.RoleMap.PUBLIC;
import static com.codingchili.zapperflyasm.model.ApiRequest.ID_LIST;

/**
 * @author Robin Duda
 * <p>
 * HTTP REST API for retrieving build status, scheduling and configuring builds.
 */
@Roles(PUBLIC)
@Address("builds")
@Description("Handles configuration and build requests.")
@DataModel(ApiRequest.class)
public class BuildHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private JobManager manager;
    private ZapperContext context;

    @Override
    public void init(CoreContext core) {
        this.context = ZapperContext.ensure(core);
        this.manager = context.getJobManager();
    }

    @Api
    @Description("Schedules a new build on the given repo and branch.")
    public void build(ApiRequest request) {
        context.getConfigurationManager().getConfig(request.getRepository(), request.getBranch())
                .setHandler(done -> {
                    if (done.succeeded()) {
                        manager.submit(done.result()).setHandler(request::result);
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

    @Api
    @Description("Retrieves the build log for the running build with the given line offset.")
    public void log(ApiRequest request) {
        manager.getLog(request.getBuildId(), request.getLogOffset()).setHandler(request::result);
    }

    @Api
    @Description("Returns the build status for the given build ID without the build log.")
    public void status(ApiRequest request) {
        getJob(request, request::write);
    }

    @Api
    @Description("Lists all builds that has been executed on the server.")
    public void list(ApiRequest request) {
        manager.getAll().setHandler(done -> {
            if (done.succeeded()) {
                request.write(new JsonObject().put(ID_LIST,
                        done.result().stream()
                                .map(Serializer::json)
                                .collect(Collectors.toList()))
                );
            } else {
                request.error(done.cause());
            }
        });
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

    @Api
    @Description("Lists all executors/instances that has joined the cluster at some point.")
    public void instances(ApiRequest request) {
        manager.instances().setHandler(request::result);
    }

    private void getJob(ApiRequest request, Consumer<BuildJob> consumer) {
        manager.getBuild(request.getBuildId()).setHandler(done -> {
            if (done.succeeded()) {
                consumer.accept(done.result());
            } else {
                throw new CoreRuntimeException(throwableToString(done.cause()));
            }
        });
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(new ApiRequest(request));
    }
}