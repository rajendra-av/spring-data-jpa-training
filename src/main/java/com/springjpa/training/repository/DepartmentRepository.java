package com.springjpa.training.repository;

import com.springjpa.training.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Interface only: Spring generates the implementation. The methods below are derived queries. */
public interface DepartmentRepository extends JpaRepository<Department, Integer> {

    Optional<Department> findByDepartmentName(String departmentName);

    boolean existsByDepartmentName(String departmentName);

    List<Department> findByActiveTrue();

    List<Department> findByLocation(String location);
}