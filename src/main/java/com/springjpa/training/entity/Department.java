package com.springjpa.training.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements Serializable because employee.department references department_name,
 * a non-primary-key column: entities that are the target of such a reference should be Serializable.
 */
@Entity
@Table(name = "department")
public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Integer departmentId;

    // Unique, and the target of employee.department (referencedColumnName = "department_name")
    @Column(name = "department_name", nullable = false, unique = true, length = 50)
    private String departmentName;

    @Column(name = "location", length = 50)
    private String location = "Hyderabad";

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    // ---- relationships -------------------------------------------------

    /** OWNING side: department.manager_id references employee.employee_id. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /** INVERSE side: employees whose employee.department points at this department. */
    @OneToMany(mappedBy = "department")
    private List<Employee> employees = new ArrayList<>();

    /**
     * INVERSE side: projects of this department (project.department_id).
     * No cascade here yet; cascade and orphanRemoval are introduced in their own section.
     */
    @OneToMany(mappedBy = "department")
    private List<Project> projects = new ArrayList<>();

    // Required by JPA
    protected Department() {
    }

    public Department(String departmentName) {
        this.departmentName = departmentName;
    }

    // ---- helper methods keep both sides in sync ---------------------------

    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);   // owning side: this is what writes employee.department
    }

    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }

    public void addProject(Project project) {
        projects.add(project);
        project.setDepartment(this);    // owning side: this is what writes project.department_id
    }

    public void removeProject(Project project) {
        projects.remove(project);
        project.setDepartment(null);    // fails on save: project.department_id is NOT NULL
    }

    // ---- getters and setters --------------------------------------------

    public Integer getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    // Read-only views: change relationships through the helper methods
    public List<Employee> getEmployees() {
        return Collections.unmodifiableList(employees);
    }

    public List<Project> getProjects() {
        return Collections.unmodifiableList(projects);
    }

    // ---- equals / hashCode on the ID only (uses getters so lazy proxies work) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Department other)) return false;
        return getDepartmentId() != null && getDepartmentId().equals(other.getDepartmentId());
    }

    @Override
    public int hashCode() {
        return Department.class.hashCode();
    }

    @Override
    public String toString() {
        return "Department{departmentId=" + departmentId
                + ", departmentName='" + departmentName + '\''
                + ", location='" + location + '\''
                + ", budget=" + budget
                + ", active=" + active + '}';
    }
}