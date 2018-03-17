package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;

import java.io.*;
import java.util.concurrent.TimeUnit;

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
public class BuildExecutor {
    private WorkerExecutor executor;
    private Logger logger;

    /**
     * The core context to execute builds on.
     */
    public BuildExecutor(CoreContext core) {
        this.logger = core.logger(getClass());
        this.executor = core.vertx().createSharedWorkerExecutor(getClass().getSimpleName());
    }

    /**
     * Executes the given build job.
     *
     * @param job the build job to be executed. The configured commandline is
     *            passed to the processbuilder.
     * @return a future that completes when the build succeeds or fails.
     */
    public Future<Void> build(BuildJob job) {
        Future<Void> future = Future.future();

        executor.executeBlocking(blocking -> {
            job.setStatus(Status.BUILDING);
            logEvent("buildBegin", job);
            try {
                Process process = new ProcessBuilder(job.getCmdLine().split(" "))
                        .directory(new File(job.getDirectory()))
                        .start();

                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while ((line = reader.readLine()) != null) {
                    job.getLog().add(line);
                }

                process.waitFor(config().getTimeoutSeconds(), TimeUnit.SECONDS);

                if (process.exitValue() == 0) {
                    logEvent("buildComplete", job);
                    blocking.complete();
                } else {
                    blocking.fail(new BuildExecutorException(job, process.exitValue()));
                }
            } catch (Throwable e) {
                logError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
            job.setStatus(Status.DONE);
            future.complete();
        }, false, future);

        return future;
    }

    private ZapperConfig config() {
        return ZapperConfig.get();
    }

    private void logEvent(String event, BuildJob job) {
        logger.event(event)
                .put("repo", job.getRepository())
                .put("branch", job.getBranch())
                .put("cmdline", job.getCmdLine())
                .send();
    }

    private void logError(BuildJob job, Throwable e) {
        logger.event("buildError")
                .put("err", CoreStrings.throwableToString(e))
                .send();
    }
}
