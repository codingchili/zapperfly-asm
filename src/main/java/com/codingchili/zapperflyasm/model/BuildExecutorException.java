package com.codingchili.zapperflyasm.model;

import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 * <p>
 * Thrown when a build has failed.
 */
public class BuildExecutorException extends CoreRuntimeException {

    /**
     * @param job the job that failed execution.
     */
    public BuildExecutorException(BuildJob job, int statusCode) {
        super(String.format("Build %s failed to execute on branch %s in repo %s." +
                        "Build exited with status '%d'.",
                job.getId(), job.getBranch(), job.getRepository(), statusCode));
    }
}
