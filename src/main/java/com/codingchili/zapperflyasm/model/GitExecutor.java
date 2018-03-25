package com.codingchili.zapperflyasm.model;

import com.codingchili.zapperflyasm.controller.ZapperConfig;
import io.vertx.core.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.codingchili.core.configuration.CoreStrings;
import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.LogMessage;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * Provides cloning functionality for the GIT versioning system.
 */
public class GitExecutor implements VersionControlSystem {
    private WorkerExecutor executor;
    private Logger logger;
    private CoreContext core;


    /**
     * @param core the core context to run the executor on.
     */
    public GitExecutor(CoreContext core) {
        this.core = core;
        logger = core.logger(getClass());
        executor = core.vertx().createSharedWorkerExecutor("git");
    }

    @Override
    public Future<String> clone(BuildJob job) {
        Future<String> future = Future.future();

        job.setProgress(Status.CLONING);
        onBegin(job);

        executor.executeBlocking((blocking) -> {
            job.setDirectory(getDirectory(job));

            try {
                String command = getCommand(job);
                job.log(command);

                Process process = new ProcessBuilder(command.split(" "))
                        .start();

                new ProcessUtil(core).readProcessOutput(process, job);
                process.waitFor(config().getTimeoutSeconds(), TimeUnit.SECONDS);

                if (process.exitValue() == 0) {
                    onCloned(job);
                    blocking.complete(job.getDirectory());
                } else {
                    blocking.fail(new BuildExecutorException(job, process.exitValue()));
                }
            } catch (Throwable e) {
                job.setProgress(Status.FAILED);
                job.setMessage(e.getMessage());
                onError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
        }, false, future);
        return future;
    }

    private String getCommand(BuildJob job) {
        BuildConfiguration config = job.getConfig();
        return String.format("git clone -b %s %s %s",
                config.getBranch(),
                config.getRepository(),
                job.getDirectory());
    }

    private ZapperConfig config() {
        return ZapperConfig.get();
    }

    @Override
    public Future<List<String>> artifacts(BuildJob buildJob) {
        Future<List<String>> future = Future.future();
        List<Future> futures = new ArrayList<>();
        List<String> files = new ArrayList<>();

        buildJob.getConfig().getOutputDirs().forEach(dir -> {
            Future<List<String>> ls = Future.future();
            core.vertx().fileSystem().readDir(dir, done -> {
                if (done.succeeded()) {
                    files.addAll(done.result());
                } else {
                    throw new CoreRuntimeException(done.cause().getMessage());
                }
            });
            futures.add(ls);
        });

        CompositeFuture.all(futures).setHandler(done -> {
            future.complete(files);
        });
        return future;
    }

    private String getDirectory(BuildJob job) {
        return ZapperConfig.get().getBuildPath() + "/" + job.getId();
    }

    @Override
    public Future<Void> delete(BuildJob job) {
        Future<Void> future = Future.future();
        core.vertx().fileSystem().deleteRecursive(job.getDirectory(), true, future);
        return future;
    }

    private void onCloned(BuildJob job) {
        event(job, "cloneComplete")
                .put("directory", job.getDirectory())
                .send();
    }

    private void onBegin(BuildJob job) {
        event(job, "onCloneBegin").send();
    }

    private void onError(BuildJob job, Throwable e) {
        event(job, "cloneFailed")
                .put("error", CoreStrings.throwableToString(e))
                .send();
    }

    private LogMessage event(BuildJob job, String event) {
        return logger.event(event)
                .put("repository", job.getConfig().getRepository())
                .put("branch", job.getConfig().getBranch());
    }
}