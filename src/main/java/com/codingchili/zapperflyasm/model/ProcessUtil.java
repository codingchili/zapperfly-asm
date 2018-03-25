package com.codingchili.zapperflyasm.model;

import java.io.*;
import java.util.function.Consumer;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * Process helper methods.
 */
public class ProcessUtil {
    private static final int LOG_DELAY = 100;
    private CoreContext core;
    private Logger logger;

    /**
     * @param core the core context for which the util schedules async tasks.
     */
    public ProcessUtil(CoreContext core) {
        this.core = core;
        this.logger = core.logger(getClass());
    }

    /**
     * Reads the output of a process on both the stdout and stderr. The output
     * is written to the given build log.
     *
     * @param process a started process to read output from.
     * @param job     the job that is the owner of the log of where to store the output.
     */
    public void readProcessOutput(Process process, BuildJob job) {
        core.periodic(() -> LOG_DELAY, "processReader", (id) -> {

            if (job.getProgress().equals(Status.BUILDING)) {
                InputStream std = process.getInputStream();
                InputStream err = process.getErrorStream();

                Consumer<InputStream> log = (stream) -> {
                    try {
                        job.log(new BufferedReader(new InputStreamReader(stream)).readLine());
                    } catch (IOException e) {
                        logger.onError(e);
                    }
                };
                try {
                    if (std.available() > 0) {
                        log.accept(std);
                    }
                    if (err.available() > 0) {
                        log.accept(err);
                    }
                } catch (IOException e) {
                    logger.onError(e);
                }

            } else {
                core.cancel(id);
            }
        });
    }

}
