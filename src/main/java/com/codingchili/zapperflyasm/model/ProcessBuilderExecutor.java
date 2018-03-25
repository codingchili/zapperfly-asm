package com.codingchili.zapperflyasm.model;

import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;

import com.codingchili.core.configuration.CoreStrings;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * Executes builds using the configured command line and uses the
 * directory that #{@link GitExecutor} cloned the repo/branch into as
 * the working directory.
 */
public class ProcessBuilderExecutor implements BuildExecutor {
    private WorkerExecutor executor;
    private CoreContext core;
    private Logger logger;

    /**
     * The core context to execute builds on.
     */
    public ProcessBuilderExecutor(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
        this.executor = core.vertx().createSharedWorkerExecutor(getClass().getSimpleName());
    }

    @Override
    public Future<Void> build(BuildJob job) {
        Future<Void> future = Future.future();

        executor.executeBlocking(blocking -> {
            job.setProgress(Status.BUILDING);
            logEvent("buildBegin", job);
            try {
                String command = job.getConfig().getCmdLine();
                job.log(command);

                AsyncProcess process = new AsyncProcess(core, command)
                        .start(job.getDirectory());

                process.monitorProcessTimeout(job::getStart).setHandler((done) -> {
                    if (done.succeeded()) {
                        if (done.result()) {
                            logEvent("buildComplete", job);
                            job.setProgress(Status.DONE);
                        } else {
                            job.setProgress(Status.FAILED);
                            logEvent("buildTimeout", job);
                        }
                        blocking.complete();
                    } else {
                        Throwable error = new BuildExecutorException(job, done.cause().getMessage());
                        job.setProgress(Status.FAILED);
                        logError(job, error);
                        blocking.fail(error);
                    }
                });

                process.readProcessOutput(job::log, () -> !job.getProgress().equals(Status.BUILDING));

            } catch (Throwable e) {
                logError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
        }, false, future);

        return future;
    }

    private void logEvent(String event, BuildJob job) {
        BuildConfiguration config = job.getConfig();
        logger.event(event)
                .put("repo", config.getRepository())
                .put("branch", config.getBranch())
                .put("cmdline", config.getCmdLine())
                .send();
    }

    private void logError(BuildJob job, Throwable e) {
        logger.event("buildError")
                .put("build", job.getDirectory())
                .put("err", CoreStrings.throwableToString(e))
                .send();
    }
}
