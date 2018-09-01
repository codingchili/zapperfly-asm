package com.codingchili.zapperflyasm;

import com.codingchili.zapperflyasm.building.BuildExecutor;
import com.codingchili.zapperflyasm.model.BuildJob;
import io.vertx.core.Future;

import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 * <p>
 * Used in test cases to avoid executing actual "builds".
 */
public class BuildExecutorMock implements BuildExecutor {
    private boolean isSuccess;

    /**
     * @param isSuccess indicates if the build completes successfully.
     */
    public BuildExecutorMock(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    @Override
    public Future<Void> build(BuildJob job) {
        Future<Void> future = Future.future();
        if (isSuccess) {
            future.complete();
        } else {
            future.fail(new CoreRuntimeException("mock build failed."));
        }
        return future;
    }
}
