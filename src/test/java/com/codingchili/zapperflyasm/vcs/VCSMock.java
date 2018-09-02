package com.codingchili.zapperflyasm.vcs;

import com.codingchili.zapperflyasm.TestConfig;
import com.codingchili.zapperflyasm.model.BuildJob;
import com.codingchili.zapperflyasm.vcs.GitExecutor;
import com.codingchili.zapperflyasm.vcs.VersionControlSystem;
import io.vertx.core.Future;

import java.util.List;

import com.codingchili.core.context.CoreContext;

/**
 * @author Robin Duda
 *
 * We simplify things and mock the git in our unit tests.
 */
public class VCSMock implements VersionControlSystem {
    private GitExecutor git;

    public VCSMock(CoreContext core) {
        this.git = new GitExecutor(core);
    }

    @Override
    public Future<String> clone(BuildJob job) {
        // clone functionality not supported in unit tests :)
        Future<String> future = Future.future();
        future.complete(TestConfig.TEST_DIR + "/" + job.getId());
        return future;
    }

    @Override
    public Future<List<String>> artifacts(BuildJob buildJob) {
        // this functionality is testable.
        return git.artifacts(buildJob);
    }

    @Override
    public Future<Void> delete(BuildJob job) {
        // this functionality is testable.
        return git.delete(job);
    }
}
