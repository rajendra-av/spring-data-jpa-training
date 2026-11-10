package com.springjpa.training.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Integer id) {
        super("Employee " + id + " not found");
    }
}
