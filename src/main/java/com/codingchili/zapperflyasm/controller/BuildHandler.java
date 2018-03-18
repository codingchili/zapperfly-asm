package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.codingchili.core.configuration.CoreStrings;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;
import com.codingchili.core.storage.*;

import static com.codingchili.zapperflyasm.model.BuildRequest.ID_LOG;

/**
 * @author Robin Duda
 * <p>
 * HTTP REST API for retrieving build status, scheduling and configuring builds.
 */
@Address("builds")
@Description("Handles configuration and build requests.")
@DataModel(BuildRequest.class)
public class BuildHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private ClusteredJobManager manager;
    private CoreContext core;

    @Override
    public void init(CoreContext core) {
        this.core = core;
    }

    @Override
    public void start(Future<Void> start) {
        CompositeFuture.join(
                ZapperConfig.getStorage(core, BuildJob.class),
                ZapperConfig.getStorage(core, BuildConfiguration.class)
        ).setHandler(done -> {
            if (done.succeeded()) {
                AsyncStorage<BuildJob> jobs = done.result().resultAt(0);
                AsyncStorage<BuildConfiguration> configs = done.result().resultAt(1);

                this.manager = new ClusteredJobManager(core, jobs, configs);
                start.complete();
            } else {
                start.fail(done.cause());
            }
        });
    }

    @Api
    @Description("Schedules a new build on the given repo and branch.")
    public void build(BuildRequest request) {
        getConfig(request, config -> {
            if (config == null) {
                request.error(new NotConfiguredException(request));
            } else {
                request.write(manager.submit(config));
            }
        });
    }

    @Api
    @Description("Cancels a scheduled build")
    public void cancel(BuildRequest request) {
        getJob(request, job -> manager.cancel(job).setHandler(request::result));
    }

    @Api
    @Description("Deletes the build files for the given job.")
    public void remove(BuildRequest request) {
        getJob(request, job -> manager.delete(job).setHandler(request::result));
    }

    @Api
    @Description("Retrieves the build log for the running build with the given line offset.")
    public void log(BuildRequest request) {
        getJob(request, job -> {
            request.write(new JsonObject().put(ID_LOG, job.getLog().stream()
                    .skip(request.getLogOffset())
                    .collect(Collectors.toList())));
        });
    }

    @Api
    @Description("Returns the build status for the given build ID without the build log.")
    public void status(BuildRequest request) {
        getJob(request, job -> request.write(job.copyWithoutLog()));
    }

    @Api
    @Description("Lists all builds that has been executed on the server.")
    public void list(BuildRequest request) {
        manager.getAll().setHandler(done -> {
            if (done.succeeded()) {
                request.write(
                        done.result().stream()
                                .map(BuildJob::copyWithoutLog)
                                .collect(Collectors.toList())
                );
            } else {
                request.error(done.cause());
            }
        });
    }

    @Api
    @Description("Lists artifacts available for download.")
    public void artifacts(BuildRequest request) {
        getJob(request, (job) -> {
            if (job.getProgress().equals(Status.DONE)) {
                manager.artifacts(job).setHandler(request::result);
            } else {
                request.write(new ArrayList<>());
            }
        });
    }

    @Api
    @Description("Adds configuration for the given repository and branch.")
    public void configure(BuildRequest request) {
        manager.putConfig(request.getConfiguration()).setHandler(request::result);
    }

    @Api
    @Description("Removes configuration for the given repository and branch.")
    public void unconfigure(BuildRequest request) {
        manager.removeConfig(request.getRepository(), request.getBranch()).setHandler(request::result);
    }

    private void getJob(BuildRequest request, Consumer<BuildJob> consumer) {
        manager.get(request.getBuildId()).setHandler(done -> {
            if (done.succeeded()) {
                consumer.accept(done.result());
            } else {
                throw new CoreRuntimeException(CoreStrings.throwableToString(done.cause()));
            }
        });
    }

    private void getConfig(BuildRequest request, Consumer<BuildConfiguration> consumer) {
        manager.getConfig(request.getRepository(), request.getBranch()).setHandler(done -> {
            if (done.succeeded()) {
                consumer.accept(done.result());
            } else {
                throw new CoreRuntimeException(CoreStrings.throwableToString(done.cause()));
            }
        });
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(new BuildRequest(request));
    }

    public void setManager(ClusteredJobManager manager) {
        this.manager = manager;
    }
}