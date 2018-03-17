package com.codingchili.zapperflyasm.model;

import io.vertx.core.*;

import java.util.ArrayList;
import java.util.List;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 */
public class GitExecutor {
    private WorkerExecutor executor;
    private Logger logger;
    private Vertx vertx;


    public GitExecutor(CoreContext core) {
        vertx = core.vertx();
        logger = core.logger(getClass());
        executor = vertx.createSharedWorkerExecutor("git");
    }

    /**
     * @return
     */
    public Future<String> clone(BuildJob job) {
        Future<String> future = Future.future();
        job.setStatus(Status.CLONING);

        // git clone -b <branch> <remote_repo> <folder job.getId> .
        log(job, "cloning");

        executor.executeBlocking((blocking) -> {
            blocking.complete("the random repo folder (id)");

            log(job, "cloned");

            // log(job, "cloneFail") + errMsg

            // if cloning fails ->
            //    job.setStatus(Status.FAILED);
            //    job.setMessage(done.cause().getMessage());


        }, false, future);

        return future;
    }

    private void log(BuildJob job, String event) {
        logger.event(event)
                .put("repository", job.getRepository())
                .put("branch", job.getBranch())
                .send();
    }

    /**
     * @param repositoryId
     * @return
     */
    public Future<Void> delete(String repositoryId) {
        Future<Void> future = Future.future();
        vertx.fileSystem().delete(repositoryId, future);
        return future;
    }

    /**
     *
     * @param buildJob
     * @return
     */
    public Future<List<String>> ls(BuildJob buildJob) {
        Future<List<String>> future = Future.future();
        List<Future> futures = new ArrayList<>();
        List<String> files = new ArrayList<>();

        buildJob.getOutputDirs().forEach(dir -> {
            Future<List<String>> ls = Future.future();
            vertx.fileSystem().readDir(dir, done -> {
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
}