package com.codingchili.zapperflyasm.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.LogMessage;
import com.codingchili.core.logging.Logger;
import com.codingchili.zapperflyasm.controller.ZapperConfig;
import static com.codingchili.core.configuration.CoreStrings.throwableToString;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;

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

        job.setStart(ZonedDateTime.now().toInstant().toEpochMilli());
        job.setProgress(Status.CLONING);
        job.setDirectory(getDirectory(job));
        onBegin(job);

        executor.executeBlocking((blocking) -> {
            try {
                String command = getCloneCommand(job);
                job.log(command);

                AsyncProcess process = new AsyncProcess(core, command).start();
                process.readProcessOutput(job::log, () -> !job.getProgress().equals(Status.CLONING));

                process.monitorProcessTimeout(job::getStart).setHandler((done) -> {
                    if (done.succeeded()) {
                        if (done.result()) {

                            // cloning competed - get the revision number.
                            head(job).setHandler(head -> {
                                if (head.failed()) {
                                    logger.onError(head.cause());
                                    blocking.fail(head.cause());
                                } else {
                                    onCloned(job);
                                    blocking.complete();
                                }
                            });

                        } else {
                            Throwable error = new BuildExecutorException(job, "Clone timed out.");
                            job.setProgress(Status.FAILED);
                            onError(job, error);
                            blocking.fail(error);
                        }
                    } else {
                        Throwable error = new BuildExecutorException(job, done.cause().getMessage());
                        job.setProgress(Status.FAILED);
                        blocking.fail(error);
                    }
                });
            } catch (Throwable e) {
                job.setProgress(Status.FAILED);
                onError(job, e);
                blocking.fail(new CoreRuntimeException(e.getMessage()));
            }
        }, false, future);
        return future;
    }

    private String getCloneCommand(BuildJob job) {
        BuildConfiguration config = job.getConfig();
        return String.format("git clone -b %s %s %s --depth 5",
                config.getBranch(),
                config.getRepository(),
                job.getDirectory());
    }

    private Future<Void> head(BuildJob job) {
        Future<Void> future = Future.future();

        executor.executeBlocking(blocking -> {
            try {
                String command = getHeadCommand(job);
                job.log(command);
                AsyncProcess process = new AsyncProcess(core, command).start();
                AtomicBoolean complete = new AtomicBoolean(false);

                process.readProcessOutput((line) -> {
                    String commit = line.split(" ")[0];
                    job.log(line);
                    job.setCommit(commit);
                    job.setMessage(line.replace(commit, "").trim());
                    job.save();
                    complete.set(true);
                }, complete::get);

                process.monitorProcessTimeout(job::getStart).setHandler((done) -> {
                    if (done.succeeded()) {
                        if (done.result()) {
                            onHead(job);
                        } else {
                            job.setProgress(Status.FAILED);
                            onError(job, new BuildExecutorException(job, "Head timed out."));
                        }
                        blocking.complete();
                    } else {
                        Throwable error = new BuildExecutorException(job, done.cause().getMessage());
                        job.setProgress(Status.FAILED);
                        blocking.fail(error);
                    }
                    complete.set(true);
                });
            } catch (Throwable e) {
                blocking.fail(e.getCause());
            }
        }, false, future);
        return future;
    }

    private String getHeadCommand(BuildJob job) {
        return String.format("git -C %s show --oneline -s", job.getDirectory());
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
        job.log("Cleaning build directory.. " + job.getDirectory());
        Future<Void> future = Future.future();

        core.blocking(blocking -> {
            try {
                Files.walk(Paths.get(job.getDirectory()))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .peek(file -> file.setWritable(true))
                    .forEach(File::delete);

                job.log("Cleaning completed.");
                blocking.complete();

            } catch (IOException e) {
                job.log("Failed to clean: " + throwableToString(e));
                blocking.fail(e);
            }
        }, future);

        return future;
    }

    private void onCloned(BuildJob job) {
        event(job, "cloneComplete")
                .put("directory", job.getDirectory())
                .send();
    }

    private void onHead(BuildJob job) {
        event(job, "headComplete")
                .put("message", job.getMessage())
                .put("commit", job.getCommit())
                .send();
    }

    private void onBegin(BuildJob job) {
        event(job, "onCloneBegin").send();
    }

    private void onError(BuildJob job, Throwable e) {
        event(job, "cloneFailed")
                .put("error", throwableToString(e))
                .send();
    }

    private LogMessage event(BuildJob job, String event) {
        return logger.event(event)
                .put("repository", job.getConfig().getRepository())
                .put("branch", job.getConfig().getBranch());
    }
}