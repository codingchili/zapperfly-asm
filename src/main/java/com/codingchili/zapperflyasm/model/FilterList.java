package com.codingchili.zapperflyasm.model;

import java.util.*;

/**
 * @author Robin Duda
 *
 * Contains a list of values that may be used in filters.
 */
public class FilterList {
    private Set<String> branches = new HashSet<>();
    private Set<String> repositories = new HashSet<>();

    public FilterList(Collection<BuildConfiguration> configurations) {
        configurations.forEach(config -> {
            branches.add(config.getBranch());
            repositories.add(config.getRepository());
        });
    }

    public Collection<String> getBranches() {
        return branches;
    }

    public void setBranches(Set<String> branches) {
        this.branches = branches;
    }

    public Collection<String> getRepositories() {
        return repositories;
    }

    public void setRepositories(Set<String> repositories) {
        this.repositories = repositories;
    }
}
