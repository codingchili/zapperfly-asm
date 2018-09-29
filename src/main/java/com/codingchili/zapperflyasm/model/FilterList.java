package com.codingchili.zapperflyasm.model;

import java.util.*;

import com.codingchili.core.protocol.Serializer;

/**
 * @author Robin Duda
 *
 * Contains a list of values that may be used in filters.
 */
public class FilterList {
    private Map<String, List<String>> repositories = new HashMap<>();

    public FilterList() {}

    public FilterList(Collection<BuildConfiguration> configurations) {
        configurations.forEach(config -> {

            if (!config.getRepository().isEmpty()) {
                repositories.putIfAbsent(config.getRepositoryName(), new ArrayList<>());
            }

            if (!config.getBranch().isEmpty()) {
                repositories.get(config.getRepositoryName()).add(config.getBranch());
            }
        });
    }

    public Map<String, List<String>> getRepositories() {
        return repositories;
    }

    public void setRepositories(Map<String, List<String>> repositories) {
        this.repositories = repositories;
    }

    public static void main(String[] args) {
        BuildConfiguration configuration = new BuildConfiguration();
        configuration.setRepository("repo");
        configuration.setBranch("branch");
        FilterList list = new FilterList(Collections.singleton(configuration));
        System.out.println(Serializer.json(list));
    }
}
