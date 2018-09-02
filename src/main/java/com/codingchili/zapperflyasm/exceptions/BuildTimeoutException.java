package com.codingchili.zapperflyasm.exceptions;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import com.codingchili.zapperflyasm.model.BuildJob;

import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 *
 * Thrown when the build has timed out.
 */
public class BuildTimeoutException extends CoreRuntimeException {

    /**
     * @param job the job that has timed out.
     */
    public BuildTimeoutException(BuildJob job) {
        super(String.format("Build %s has failed due to timeout after %d.",
                job.getId(), ZapperConfig.getEnvironment().getTimeoutSeconds()));
    }
}
