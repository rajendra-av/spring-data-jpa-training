package com.springjpa.training.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "project_name", nullable = false, unique = true, length = 100)
    private String projectName;

    @Column(name = "budget", precision = 12, scale = 2)
    private BigDecimal budget;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // DB default is 'Active'; repeated here because Hibernate inserts every column
    @Column(name = "status", length = 20)
    private String status = "Active";

    /**
     * OWNING side: project.department_id references department.department_id.
     * optional = false / nullable = false mirror the NOT NULL constraint.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // Required by JPA
    protected Project() {
    }

    public Project(String projectName, Department department, LocalDate startDate) {
        this.projectName = projectName;
        this.department = department;
        this.startDate = startDate;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    // ---- equals / hashCode on the ID only (uses getters so lazy proxies work) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project other)) return false;
        return getProjectId() != null && getProjectId().equals(other.getProjectId());
    }

    @Override
    public int hashCode() {
        return Project.class.hashCode();
    }

    @Override
    public String toString() {
        return "Project{projectId=" + projectId
                + ", projectName='" + projectName + '\''
                + ", budget=" + budget
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + ", status='" + status + '\'' + '}';
    }
}