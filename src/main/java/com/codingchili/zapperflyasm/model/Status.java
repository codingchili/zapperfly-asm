package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 * <p>
 * States for build jobs.
 */
public enum Status {
    QUEUED(false),     // the build is queued and waiting for an executor on the cluster.
    CLONING(false),    // the build has started and the executor is cloning the repo/branch.
    BUILDING(false),   // the build is in progress.
    DONE(true),       // the build has completed - artifacts are now available.
    CANCELLED(true),  // the build has been cancelled - no artifacts available.
    FAILED(true);      // the build has failed - no artifacts available.

    private boolean isFinal;

    Status(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public boolean isFinal() {
        return this.isFinal;
    }
}
