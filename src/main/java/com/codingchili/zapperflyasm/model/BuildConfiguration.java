package com.codingchili.zapperflyasm.model;

import java.util.*;
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
    private static final Pattern urlsafe = Pattern.compile("((ssh|(htt(p|ps)))://[0-9A-Za-z/._:@-]+)|[a-zA-Z0-9 ]+");
    private List<String> outputDirs = Arrays.asList("out", "build", "target");
    private String id = UUID.randomUUID().toString();
    private boolean autoclean = false;
    private String dockerImage = "";
    private String repository = "script";
    private String cmdLine = "";
    private String branch = "";

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

    public BuildConfiguration setOutputDirs(List<String> outputDirs) {
        this.outputDirs = outputDirs;
        return this;
    }

    /**
     * @return a list of output paths from where to locate artifacts for download.
     */
    public List<String> getOutputDirs() {
        return outputDirs;
    }

    /**
     * @return the short name of the repository, url safe.
     */
    public String getRepositoryName() {
        if (!repository.isEmpty() && repository.contains("/")) {
            String repositoryName = repository.substring(repository.lastIndexOf("/"), repository.length());
            repositoryName = repositoryName.replaceFirst("/", "");
            return repositoryName;
        } else {
            return repository;
        }
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
    public BuildConfiguration setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
        return this;
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
    public BuildConfiguration setCmdLine(String cmdLine) {
        this.cmdLine = cmdLine;
        return this;
    }

    /**
     * @return the unique id of the configuration.
     */
    public String getId() {
        return id;
    }

    public BuildConfiguration setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @param autoclean true if the build should be cleaned after exiting.
     */
    public BuildConfiguration setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
        return this;
    }

    /**
     * @param repository an url to the git repository to clone.
     */
    public BuildConfiguration setRepository(String repository) {
        this.repository = repository;
        return this;
    }

    /**
     * @param branch the branch in the git repository to clone.
     */
    public BuildConfiguration setBranch(String branch) {
        this.branch = branch;
        return this;
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
