package com.codingchili.zapperflyasm.model;

import static com.codingchili.core.protocol.RoleMap.USER;

/**
 * @author Robin Duda
 * <p>
 * Represents an user in the configuration.
 */
public class User {
    private String role = USER;
    private String username;
    private String password;

    /**
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set.
     * @return fluent
     */
    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * @return a hashed password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the hashed password to set.
     * @return fluent
     */
    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * @return a list of roles the
     */
    public String getRole() {
        return role;
    }

    /**
     * @param roles a list of roles to set.
     */
    public User setRole(String roles) {
        this.role = roles;
        return this;
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof User) && ((User) obj).getUsername().equals(username);
    }
}
