package com.springjpa.training.exception;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(Integer id) {
        super("Project not found with id " + id);
    }
}