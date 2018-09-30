package com.codingchili.zapperflyasm.exceptions;

import com.codingchili.core.context.CoreRuntimeException;

/**
 * @author Robin Duda
 *
 * Thrown when a requested build cannot be found.
 */
public class NoSuchBuildException extends CoreRuntimeException {

    /**
     * @param buildId the ID of the build that was not found.
     */
    public NoSuchBuildException(String buildId) {
        super("Could not find build " + buildId + " in the job manager.");
    }

}
