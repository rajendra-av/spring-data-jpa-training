package com.springjpa.training.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee")
public class Employee {

    // serial -> int4 + sequence-backed default; IDENTITY lets the DB generate it
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "job_title", length = 50)
    private String jobTitle;

    // Plain text column in this table (not a foreign key)
    @Column(name = "department", length = 50)
    private String department;

    // numeric(10,2); DB check constraint enforces salary > 0
    @Column(name = "salary", precision = 10, scale = 2)
    private BigDecimal salary;

    // Self-reference to another employee; mapped as a plain value for now,
    // becomes @ManyToOne Employee manager in the relationship mapping section
    @Column(name = "manager_id")
    private Integer managerId;

    // DB default is 'Hyderabad'; Hibernate inserts every column, so mirror the default in Java
    @Column(name = "city", length = 50)
    private String city = "Hyderabad";

    // DB default is true; field named 'active' to avoid isIsActive() style accessors
    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    // Not a column: derived value, not persisted
    @Transient
    private String fullName;

    // Required by JPA (used by Hibernate via reflection)
    protected Employee() {
    }

    // Convenience constructor with the mandatory (NOT NULL) columns
    public Employee(String firstName, String lastName, LocalDate hireDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.hireDate = hireDate;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Getters and setters

    public Integer getEmployeeId() {
        return employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public Integer getManagerId() {
        return managerId;
    }

    public void setManagerId(Integer managerId) {
        this.managerId = managerId;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    // equals/hashCode on the ID only (null-safe for new, unsaved entities)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee other)) return false;
        return employeeId != null && employeeId.equals(other.employeeId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId=" + employeeId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", hireDate=" + hireDate +
                ", jobTitle='" + jobTitle + '\'' +
                ", department='" + department + '\'' +
                ", salary=" + salary +
                ", city='" + city + '\'' +
                ", active=" + active +
                '}';
    }
}