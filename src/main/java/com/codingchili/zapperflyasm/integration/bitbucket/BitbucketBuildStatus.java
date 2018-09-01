package com.codingchili.zapperflyasm.integration.bitbucket;

import com.codingchili.zapperflyasm.model.Status;

/**
 * @author Robin Duda
 *
 * Contains available statuses for the status REST API.
 */
public enum BitbucketBuildStatus {
    INPROGRESS, SUCCESSFUL, FAILED;

    /**
     * Maps a zapperfly internal status to a bitbucket server build status.
     * @param status the internal status to be mapped to a bitbucket build status.
     * @return a bitbucket status mapped from the given status.
     */
    public static BitbucketBuildStatus fromBuildStatus(Status status) {
        switch (status) {
            case QUEUED:
            case CLONING:
            case BUILDING:
                return INPROGRESS;
            case DONE:
                return SUCCESSFUL;
            case CANCELLED:
            case FAILED:
                return FAILED;
            default:
                return INPROGRESS;
        }
    }
}
