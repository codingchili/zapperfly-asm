package com.codingchili.zapperflyasm.controller;

import com.codingchili.zapperflyasm.model.*;

import java.util.ArrayList;
import java.util.stream.Collectors;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.listener.CoreHandler;
import com.codingchili.core.listener.Request;
import com.codingchili.core.protocol.*;

/**
 * @author Robin Duda
 * <p>
 * HTTP REST API for retrieving build status, scheduling and configuring builds.
 */
@DataModel(BuildRequest.class)
@Address("builds")
public class BuildHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private SimpleJobManager manager;

    @Override
    public void init(CoreContext core) {
        this.manager = new SimpleJobManager(core);
    }

    @Api
    @Description("Schedules a new build on the given repo and branch.")
    public void build(BuildRequest request) {
        BuildConfiguration config = manager.getConfig(request.getRepository(), request.getBranch());

        if (config == null) {
            request.error(new NotConfiguredException(request));
        } else {
            request.write(manager.submit(config));
        }
    }

    @Api
    @Description("Deletes the build files for the given job.")
    public void delete(BuildRequest request) {
        manager.delete(manager.get(request.getBuildId()));
        request.accept();
    }

    @Api
    @Description("Retrieves the build log for the running build with the given line offset.")
    public void log(BuildRequest request) {
        request.write(manager.get(request.getBuildId()).getLog().stream()
                .skip(request.getLogOffset())
                .collect(Collectors.toList()));
    }

    @Api
    @Description("Returns the build status for the given build ID without the build log.")
    public void status(BuildRequest request) {
        BuildJob job = manager.get(request.getBuildId());

        BuildJob data = Serializer.kryo((k) -> {
            BuildJob copy = k.copy(job);

            // require clients to request the full log explicitly.
            copy.getLog().clear();
            return copy;
        });
        request.write(data);
    }

    @Api
    @Description("Lists all builds that has been executed on the server.")
    public void list(BuildRequest request) {
        // todo: probably introduce an offset and limit here.
        request.write(
                manager.getAll().stream()
                        .map(job -> Serializer.kryo(k -> {
                            BuildJob copy = k.copy(job);
                            copy.getLog().clear();
                            return copy;
                        })).collect(Collectors.toList())
        );
    }

    @Api
    @Description("Lists artifacts available for download.")
    public void artifacts(BuildRequest request) {
        BuildJob job = manager.get(request.getBuildId());

        if (job.getStatus().equals(Status.DONE)) {
            manager.artifacts(job).setHandler(request::result);
        } else {
            request.write(new ArrayList<>());
        }
    }

    @Api
    @Description("Adds configuration for the given repository and branch.")
    public void configure(BuildRequest request) {
        manager.putConfig(request.getConfiguration());
        request.accept();
    }

    @Api
    @Description("Removes configuration for the given repository and branch.")
    public void unconfigure(BuildRequest request) {
        manager.removeConfig(request.getRepository(), request.getBranch());
        request.accept();
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(new BuildRequest(request));
    }
}
