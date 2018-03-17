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
 *
 * Handler for new builds.
 */
@Address("builds")
public class BuildHandler implements CoreHandler {
    private Protocol<Request> protocol = new Protocol<>(this);
    private JobManager manager;

    @Override
    public void init(CoreContext core) {
        this.manager = new JobManager(core);
    }

    @Api
    public void build(BuildRequest request) {
        BuildConfiguration config = manager.getConfig(request.getRepository(), request.getBranch());
        request.write(manager.submit(config));
    }

    @Api
    public void log(BuildRequest request) {
        request.write(manager.get(request.getBuildId()).getLog().stream()
            .skip(request.getLogOffset())
            .collect(Collectors.toList()));
    }

    @Api
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
    public void artifacts(BuildRequest request) {
        BuildJob job = manager.get(request.getBuildId());

        if (job.getStatus().equals(Status.DONE)) {
            manager.artifacts(request.getBuildId()).setHandler(request::result);
        } else {
            request.write(new ArrayList<>());
        }
    }

    @Api
    public void configure(BuildRequest request) {
        manager.putConfig(request.getConfiguration());
        request.accept();
    }

    @Override
    public void handle(Request request) {
        protocol.get(request.route()).submit(new BuildRequest(request));
    }
}
