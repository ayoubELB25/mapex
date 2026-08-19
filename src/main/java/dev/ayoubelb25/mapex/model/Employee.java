package dev.ayoubelb25.mapex.model;

import java.io.Serializable;

/**
 * Simple non-persistent model with no backing database table.
 */
public class Employee implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String position;
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
