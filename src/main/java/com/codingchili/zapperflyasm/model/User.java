package com.codingchili.zapperflyasm.model;

/**
 * @author Robin Duda
 * <p>
 * Represents an user in the configuration.
 */
public class User {
    private String username;
    private String password;

    /**
     * @return the username.
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return a hashed password.
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
