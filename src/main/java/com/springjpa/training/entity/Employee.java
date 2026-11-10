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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "employee")
public class Employee {

    // serial -> int4 with a sequence-backed default; IDENTITY lets the database generate it
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

    // numeric(10,2); the DB check (salary > 0) is not expressible in JPA
    @Column(name = "salary", precision = 10, scale = 2)
    private BigDecimal salary;

    // DB defaults are repeated in Java: Hibernate inserts every column, so a null would override them
    @Column(name = "city", length = 50)
    private String city = "Hyderabad";

    @Column(name = "is_active")
    private Boolean active = Boolean.TRUE;

    // ---- relationships -------------------------------------------------

    /**
     * OWNING side. The FK column employee.department holds a department NAME,
     * not an ID: it references department.department_name (a unique, non-primary-key column),
     * hence referencedColumnName.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department", referencedColumnName = "department_name")
    private Department department;

    /**
     * OWNING side of the self-reference: employee.manager_id points to another employee.
     * The DDL has no FK constraint for it, but the mapping works the same.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /** INVERSE side: employees who report to this employee. No column in this table. */
    @OneToMany(mappedBy = "manager")
    private List<Employee> reports = new ArrayList<>();

    /** INVERSE side: departments this employee manages (department.manager_id). */
    @OneToMany(mappedBy = "manager")
    private List<Department> managedDepartments = new ArrayList<>();

    // Derived value: there is no field behind it, so nothing is mapped (field access is used).
    // To keep a field out of the table, mark the FIELD with @Transient instead.
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Required by JPA
    protected Employee() {
    }

    public Employee(String firstName, String lastName, LocalDate hireDate) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.hireDate = hireDate;
    }

    // ---- helper methods keep both sides of the self-reference in sync ----

    public void addReport(Employee report) {
        reports.add(report);
        report.setManager(this);   // owning side: this is what writes manager_id
    }

    public void removeReport(Employee report) {
        reports.remove(report);
        report.setManager(null);
    }

    // ---- getters and setters --------------------------------------------

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

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Employee getManager() {
        return manager;
    }

    public void setManager(Employee manager) {
        this.manager = manager;
    }

    // Read-only views: change relationships through the helper methods, not the collections
    public List<Employee> getReports() {
        return Collections.unmodifiableList(reports);
    }

    public List<Department> getManagedDepartments() {
        return Collections.unmodifiableList(managedDepartments);
    }

    // ---- equals / hashCode on the ID only (null-safe for new, unsaved entities) ----
    // Uses getters, not fields: a lazy proxy has empty fields until initialized.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Employee other)) return false;
        return getEmployeeId() != null && getEmployeeId().equals(other.getEmployeeId());
    }

    @Override
    public int hashCode() {
        return Employee.class.hashCode();
    }

    // Relationships are left out on purpose: printing them would trigger lazy loads or recurse
    @Override
    public String toString() {
        return "Employee{employeeId=" + employeeId
                + ", firstName='" + firstName + '\''
                + ", lastName='" + lastName + '\''
                + ", email='" + email + '\''
                + ", jobTitle='" + jobTitle + '\''
                + ", salary=" + salary
                + ", city='" + city + '\''
                + ", active=" + active + '}';
    }
}