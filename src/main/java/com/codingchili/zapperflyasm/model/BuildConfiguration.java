package com.codingchili.zapperflyasm.model;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.codingchili.core.context.CoreRuntimeException;
import com.codingchili.core.storage.Storable;

/**
 * @author Robin Duda
 * <p>
 * Contains build configuration for a repo and branch combination.
 */
public class BuildConfiguration implements Storable {
    private static final Pattern safe = Pattern.compile("[0-9A-Za-z-./_]+");
    private static final Pattern urlsafe = Pattern.compile("htt(p|ps)://[A-Za-z/.-]+");
    private List<String> outputDirs = Arrays.asList("out", "build", "target");
    private boolean autoclean = false;
    private String dockerImage = "";
    private String repository = "";
    private String branch = "";
    private String cmdLine = "";

    public BuildConfiguration() {
    }

    /**
     * @param repository the repository the configuration applies to.
     * @param branch     the branch the configuration applies to.
     */
    public BuildConfiguration(String repository, String branch) {
        this.repository = repository;
        this.branch = branch;
    }

    /**
     * @return the repository for which the configuration applies.
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @return the branch for which the configuration applies.
     */
    public String getBranch() {
        return branch;
    }

    /**
     * @return true if the build catalog is to be removed when builds complete.
     */
    public boolean isAutoclean() {
        return autoclean;
    }

    public void setOutputDirs(List<String> outputDirs) {
        this.outputDirs = outputDirs;
    }

    /**
     * @return a list of output paths from where to locate artifacts for download.
     */
    public List<String> getOutputDirs() {
        return outputDirs;
    }

    /**
     * @return the name of the docker image to run the build inside.
     */
    public String getDockerImage() {
        return dockerImage;
    }

    /**
     * @param dockerImage the name (alphanumeric) of the docker image to run the build inside.
     */
    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    /**
     * @return the commandline to be executed.
     */
    public String getCmdLine() {
        return cmdLine;
    }

    /**
     * @param cmdLine the commandline to execute when starting the build.
     */
    public void setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
    }

    /**
     * @return the unique id of the configuration.
     */
    public String getId() {
        return toKey(repository, branch);
    }

    /**
     * Converts the given repo and branch into a unique identifier for the config.
     *
     * @param repository the name of the repository.
     * @param branch     the name of the branch.
     * @return a unique key.
     */
    public static String toKey(String repository, String branch) {
        return repository + "@" + branch;
    }

    /**
     * @param autoclean true if the build should be cleaned after exiting.
     */
    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    /**
     * @param repository an url to the git repository to clone.
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }

    /**
     * @param branch the branch in the git repository to clone.
     */
    public void setBranch(String branch) {
        this.branch = branch;
    }

    /**
     * Performs input validation. Throws a {@link CoreRuntimeException} if input is invalid.
     */
    public void sanitize() {
        assertAlphanumeric(branch);
        assertAlphanumeric(dockerImage);
        assertUrlSafe(repository);
    }

    private String assertAlphanumeric(String input) {
        if (input != null && !input.isEmpty()) {
            if (safe.matcher(input).matches()) {
                return input;
            } else {
                throw new CoreRuntimeException(
                        String.format("Input '%s' does not match '%s'.",
                                input, safe.pattern()));
            }
        } else {
            return input;
        }
    }

    private String assertUrlSafe(String url) {
        if (url != null && !url.isEmpty()) {
            if (urlsafe.matcher(url).matches()) {
                return url;
            } else {
                throw new CoreRuntimeException(
                        String.format("URL '%s' does not match '%s'.",
                                url, urlsafe.pattern()));
            }
        } else {
            return url;
        }
    }
}
