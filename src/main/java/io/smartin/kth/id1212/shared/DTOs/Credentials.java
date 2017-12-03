package io.smartin.kth.id1212.shared.DTOs;

import java.io.Serializable;

public class Credentials implements Serializable {
    private final String username;
    private final String password;

    public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
