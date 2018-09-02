package com.codingchili.zapperflyasm.process;

import com.codingchili.zapperflyasm.model.ZapperConfig;
import io.vertx.core.Future;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.codingchili.core.context.CoreContext;
import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.logging.Logger;

/**
 * @author Robin Duda
 * <p>
 * Process helper methods for managing processes asynchronously..
 */
public class AsyncProcess {
    private static final int PROCESS_POLL_DELAY = 1000;
    private static final int LOG_DELAY = 100;
    private int timeout = ZapperConfig.getEnvironment().getTimeoutSeconds();
    private CoreContext core;
    private Logger logger;
    private String[] command;
    private Process process;

    /**
     * @param core the core context for which the util schedules async tasks.
     */
    public AsyncProcess(CoreContext core, String command) {
        this.command = (getShell() + command).split(" ");
        this.core = core;
        this.logger = core.logger(getClass());
    }

    /**
     * Starts the process in the current working directory.
     *
     * @return fluent.
     */
    public AsyncProcess start() {
        return start(new File("").toPath().toAbsolutePath().toString());
    }

    /**
     * Starts the process.
     *
     * @param directory the working dir of the process.
     * @return fluent.
     */
    public AsyncProcess start(String directory) {
        try {
            process = new ProcessBuilder(command)
                    .directory(new File(directory))
                    .start();
        } catch (IOException e) {
            throw new CoreRuntimeException(e.getMessage());
        }
        return this;
    }

    String nextLine = "";

    /**
     * Reads the output of a process on both the stdout and stderr. The output
     * is written to the given build log.
     *
     * @param reader the output of the reader.
     * @param done   indiciates when the process is done.
     */
    public void readProcessOutput(Consumer<String> reader, Supplier<Boolean> done) {
        core.periodic(() -> LOG_DELAY, "processReader", (id) -> {

            if (done.get()) {
                core.cancel(id);
            } else {
                InputStream std = process.getInputStream();
                InputStream err = process.getErrorStream();

                Consumer<InputStream> log = (stream) -> {
                    try {
                        byte[] buffer = new byte[stream.available()];
                        stream.read(buffer, 0, buffer.length);

                        String line = nextLine + new String(buffer);

                        if (line.contains("\n")) {
                            nextLine = line.substring(line.lastIndexOf("\n"), line.length());
                            line = line.substring(0, line.lastIndexOf("\n"));
                            reader.accept(line);
                        } else {
                            nextLine = line;
                        }
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

            }
        });
    }

    /**
     * Monitors the given process' output without blocking the thread.
     *
     * @return a callback completed when the process completes without an error.
     * fails if the process returns an error code. Completes with false
     * if the process has timed out.
     */
    public Future<Boolean> monitorProcessTimeout(Supplier<Long> start) {
        Future<Boolean> future = Future.future();
        core.periodic(() -> PROCESS_POLL_DELAY, "processPoller", (id) -> {
            try {
                int exitCode = process.exitValue();

                // process is finished - retrieved exit code without error.
                core.cancel(id);

                if (exitCode == 0) {
                    future.complete(true);
                } else {
                    future.fail("process exited with: " + exitCode);
                }
            } catch (IllegalThreadStateException e) {
                // process not finished yet - if timeout then fail.
                if (timeout(start.get())) {
                    core.cancel(id);
                    future.complete(false);
                    process.destroyForcibly();
                }
            }
        });
        return future;
    }

    /**
     * @return the default shell of the operating system.
     * move this into the global configuration.
     */
    public String getShell() {
        String os = System.getProperty("os.name");

        if (os.toLowerCase().contains("windows")) {
            return ZapperConfig.getEnvironment().getWindowsShell() + " ";
        } else {
            // assume bash exists on unix.
            return ZapperConfig.getEnvironment().getUnixShell() + " ";
        }
    }

    private boolean timeout(Long start) {
        return ZonedDateTime.now().minusSeconds(timeout).toInstant().toEpochMilli() > start;
    }
}
