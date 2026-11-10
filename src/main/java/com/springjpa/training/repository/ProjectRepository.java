package com.springjpa.training.repository;

import com.springjpa.training.entity.Department;
import com.springjpa.training.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    boolean existsByProjectName(String projectName);

    List<Project> findByStatus(String status);

    // Path traversal: Project.department -> Department.departmentId
    List<Project> findByDepartmentDepartmentId(Integer departmentId);

    // By entity: used when the caller already holds the Department
    List<Project> findByDepartment(Department department);

    boolean existsByDepartment(Department department);

    boolean existsByDepartmentAndStatus(Department department, String status);
}
