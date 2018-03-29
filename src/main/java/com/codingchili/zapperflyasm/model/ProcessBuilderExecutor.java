package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
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
                            onSuccess(job);
                        } else {
                            onTimeout(job);
                        }
                        blocking.complete();
                    } else {
                        Throwable error = new BuildExecutorException(job, done.cause().getMessage());
                        onError(job, done.cause());
                        blocking.fail(error);
                    }
                });

                process.readProcessOutput(job::log, () -> !job.getProgress().equals(Status.BUILDING));
            } catch (Throwable e) {
                onError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
        }, false, future);

        return future;
    }

    private void onError(BuildJob job, Throwable error) {
        job.setProgress(Status.FAILED);
        job.log("Build failed: " + error.getMessage());
        logger.event("buildError")
                .put("build", job.getDirectory())
                .put("branch", job.getConfig().getBranch())
                .put("repo", job.getConfig().getRepository())
                .put("cmd", job.getConfig().getCmdLine())
                .put("err", CoreStrings.throwableToString(error))
                .send();
    }

    private void onTimeout(BuildJob job) {
        job.setProgress(Status.FAILED);
        logEvent("buildTimeout", job);
        job.log("Build timed out after " + ZapperConfig.get().getTimeoutSeconds() + "s.");
    }

    private void onSuccess(BuildJob job) {
        logEvent("buildComplete", job);
        job.setProgress(Status.DONE);
        job.log("Build completed successfully.");
    }

    private void logEvent(String event, BuildJob job) {
        BuildConfiguration config = job.getConfig();
        logger.event(event)
                .put("repo", config.getRepository())
                .put("branch", config.getBranch())
                .put("cmdline", config.getCmdLine())
                .send();
    }
}
