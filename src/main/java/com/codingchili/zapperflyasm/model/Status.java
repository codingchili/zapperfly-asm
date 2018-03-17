package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 * <p>
 * States for build jobs.
 */
public enum Status {
    QUEUED,     // the build is queued and waiting for an executor on the cluster.
    CLONING,    // the build has started and the executor is cloning the repo/branch.
    BUILDING,   // the build is in progress.
    DONE,       // the build has completed - artifacts are now available.
    CANCELLED,  // the build has been cancelled - no artifacts available.
    FAILED      // the build has failed - no artifacts available.
}
