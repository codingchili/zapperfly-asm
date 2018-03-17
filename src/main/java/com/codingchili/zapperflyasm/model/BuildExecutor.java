package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 */
public class BuildExecutor {
    private WorkerExecutor executor;
    private CoreContext core;
    private Logger logger;

    public BuildExecutor(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
        this.executor = core.vertx().createSharedWorkerExecutor(getClass().getSimpleName());
    }

    public Future<Void> build(BuildJob job) {
        Future<Void> future = Future.future();
        job.setStatus(Status.BUILDING);
        logger.event("building")
                .put("branch", job.getBranch())
                .put("repository", job.getRepository())
                .put("cmd", job.getCmdLine()).send();
        job.setStatus(Status.DONE);
        future.complete();
        // processbuilder and update job status.
        return future;
    }
}
