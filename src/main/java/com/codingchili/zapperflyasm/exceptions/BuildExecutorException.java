package com.codingchili.zapperflyasm.exceptions;

import com.codingchili.zapperflyasm.model.BuildJob;

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
    public BuildExecutorException(BuildJob job, String message) {
        super(String.format("Build %s failed to execute on branch %s in repo %s. " +
                        "%s.",
                job.getId(),
                job.getConfig().getBranch(),
                job.getConfig().getRepository(),
                message));
    }
}
