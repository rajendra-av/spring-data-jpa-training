package com.springjpa.training.exception;

/** Unchecked, so a surrounding @Transactional method rolls back when it is thrown. */
public class DepartmentNotFoundException extends RuntimeException {

    public DepartmentNotFoundException(Integer id) {
        super("Department not found with id " + id);
    }

    public DepartmentNotFoundException(String name) {
        super("Department not found with name " + name);
    }
}