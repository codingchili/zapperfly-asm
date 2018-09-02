package com.codingchili.zapperflyasm.building;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import com.codingchili.zapperflyasm.exceptions.BuildExecutorException;
import com.codingchili.zapperflyasm.model.*;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.model.BuildConfiguration;
import com.codingchili.zapperflyasm.process.AsyncProcess;
import com.codingchili.zapperflyasm.vcs.GitExecutor;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.buffer.Buffer;

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
    private static final String BUILD_SCRIPT_NAME = "_zapperfly";
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
                String command = createCommand(job);
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

    private String createCommand(BuildJob job) {
        BuildConfiguration config = job.getConfig();
        String command;

        if (job.getConfig().getDockerImage().isEmpty()) {
            command = config.getCmdLine();
            job.log("WARNING: build is not running in a container !!!");
        } else {
            String script = "#!/bin/sh\n" + config.getCmdLine();
            core.fileSystem().writeFileBlocking(job.getDirectory() + "/" + BUILD_SCRIPT_NAME, Buffer.buffer(script));
            job.log(String.format("executing build script in docker image '%s'.", config.getDockerImage()));
            command = ZapperConfig.getEnvironment().getDockerLine()
                    .replace("$image", config.getDockerImage())
                    .replace("$script", BUILD_SCRIPT_NAME)
                    .replace("$directory", job.getDirectory());
        }
        return command;
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
        job.log("Build timed out after " + ZapperConfig.getEnvironment().getTimeoutSeconds() + "s.");
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
