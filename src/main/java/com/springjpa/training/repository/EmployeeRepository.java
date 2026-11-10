package com.springjpa.training.repository;

import com.springjpa.training.entity.Department;
import com.springjpa.training.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Updated for the relationship-mapped Employee: department and manager are now objects,
 * so derived queries use entities or a full property path (manager -> employeeId).
 */
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    boolean existsByEmail(String email);

    boolean existsByManagerEmployeeId(Integer managerId);

    List<Employee> findByManager(Employee manager);

    boolean existsByDepartment(Department department);

    List<Employee> findByDepartment(Department department);
}
