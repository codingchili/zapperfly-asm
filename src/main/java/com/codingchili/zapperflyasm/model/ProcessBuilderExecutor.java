package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;

import java.io.*;
import java.time.ZonedDateTime;

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
    private static final int PROCESS_POLL_DELAY = 1000;
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
            job.setStatus(Status.BUILDING);
            logEvent("buildBegin", job);
            try {
                Process process = new ProcessBuilder(job.getCmdLine().split(" "))
                        .directory(new File(job.getDirectory()))
                        .start();

                monitorProcessTimeout(process, blocking, job);

                String line;
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = reader.readLine()) != null) {
                    job.getLog().add(line);
                }
            } catch (Throwable e) {
                logError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
            job.setStatus(Status.DONE);
        }, false, future);

        return future;
    }

    private void monitorProcessTimeout(Process process, Future<Void> blocking, BuildJob job) {
        core.periodic(() -> PROCESS_POLL_DELAY, "processPoller", (id) -> {
            try {
                int exitCode = process.exitValue();

                // process is finished - retrieved exit code without error.
                core.cancel(id);

                if (exitCode == 0) {
                    logEvent("buildComplete", job);
                    blocking.complete();
                } else {
                    Throwable error = new BuildExecutorException(job, process.exitValue());
                    logError(job, error);
                    blocking.fail(error);
                }
            } catch (IllegalThreadStateException e) {
                // process not finished yet - if timeout then fail.
                if (timeout(job)) {
                    core.cancel(id);
                    blocking.fail(new BuildTimeoutException(job));
                    process.destroyForcibly();
                    logEvent("buildTimeout", job);
                }
            }
        });
    }

    private boolean timeout(BuildJob job) {
        return (ZonedDateTime.now().minusSeconds(config().getTimeoutSeconds()).isAfter(job.getStart()));
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
