package com.springjpa.training.repository;

import com.springjpa.training.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    boolean existsByEmail(String email);

    boolean existsByManagerId(Integer managerId);

    List<Employee> findByManagerId(Integer managerId);
}
