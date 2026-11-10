package com.springjpa.training.service;

import com.springjpa.training.entity.Employee;
import com.springjpa.training.repository.EmployeeRepository;
import com.springjpa.training.exception.BusinessRuleException;
import com.springjpa.training.exception.EmployeeNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Employee service working on the employee table only.
 *
 * Methods are grouped by the parent interface of JpaRepository that supplies the repository call:
 *   CrudRepository, ListCrudRepository, PagingAndSortingRepository,
 *   ListPagingAndSortingRepository, then JpaRepository's own extras.
 *
 * Transactions: the class is read-only by default (cheaper reads, no dirty checking).
 * Methods that change data override it with a plain @Transactional.
 *
 * Depends on EmployeeRepository (needs existsByEmail, existsByManagerId, findByManagerId),
 * plus EmployeeNotFoundException and BusinessRuleException (both unchecked, so they roll back).
 */
@Service
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;

    // Whitelist of Employee fields clients may sort by (entity field names, not column names).
    // An unknown name would otherwise fail with PropertyReferenceException.
    private static final Set<String> SORTABLE = Set.of(
            "employeeId", "firstName", "lastName", "email", "hireDate",
            "jobTitle", "department", "salary", "city", "active");

    private final EmployeeRepository employeeRepository;

    // Single constructor: Spring injects the repository proxy, no @Autowired needed
    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // =====================================================================
    // CrudRepository: single-entity operations
    // =====================================================================

    /** Create. save() on a new entity (ID null) runs persist, which issues an INSERT. */

    public Employee create(Employee employee) {
        if (employee.getEmployeeId() != null) {
            // An entity with an ID would make save() run merge, i.e. update an existing row
            throw new BusinessRuleException("New employee must not have an ID");
        }
        checkEmailAvailable(employee.getEmail());
        return employeeRepository.save(employee);   // [CrudRepository] save
    }

    /** Read: throws if missing. findById returns an Optional. */
    public Employee getById(Integer id) {
        return findOrThrow(id);
    }

    /** Read: caller decides what to do when the employee is absent. */
    public Optional<Employee> findById(Integer id) {
        return employeeRepository.findById(id);   // [CrudRepository] findById
    }

    public boolean exists(Integer id) {
        return employeeRepository.existsById(id);   // [CrudRepository] existsById
    }

    public long headcount() {
        return employeeRepository.count();   // [CrudRepository] count
    }

    /**
     * Update through dirty checking: load, change, no save() call.
     * Hibernate compares the entity with its snapshot at commit and issues the UPDATE.
     */
    @Transactional
    public Employee updateContactDetails(Integer id, String email, String phone, String city) {
        Employee emp = findOrThrow(id);
        if (email != null && !email.equals(emp.getEmail())) {
            checkEmailAvailable(email);
            emp.setEmail(email);
        }
        if (phone != null) {
            emp.setPhone(phone);
        }
        if (city != null) {
            emp.setCity(city);
        }
        return emp;
    }

    /** Update through dirty checking with a business rule. percent = 0.10 means 10%. */
    @Transactional
    public Employee giveRaise(Integer id, BigDecimal percent) {
        if (percent.signum() <= 0 || percent.compareTo(BigDecimal.ONE) > 0) {
            throw new BusinessRuleException("Raise must be between 0 and 100%");
        }
        Employee emp = findOrThrow(id);
        if (!Boolean.TRUE.equals(emp.getActive()) || emp.getSalary() == null) {
            throw new BusinessRuleException("Raise needs an active employee with a salary");
        }
        emp.setSalary(emp.getSalary().multiply(BigDecimal.ONE.add(percent)).setScale(2, RoundingMode.HALF_UP));
        return emp;
    }

    /**
     * Update through save() with a detached entity (loaded earlier, changed outside a transaction).
     * save() sees a non-null ID and runs merge: SELECT, copy state, UPDATE. Use the returned copy.
     * Caution: merge overwrites every column, including ones the caller left null.
     */
    public Employee replace(Employee detached) {
        Integer id = detached.getEmployeeId();
        if (id == null || !employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        return employeeRepository.save(detached);   // [CrudRepository] save, merge path
    }

    /** Delete by ID. deleteById silently does nothing for a missing ID, so check first. */
    public void deleteById(Integer id) {
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException(id);
        }
        if (employeeRepository.existsByManagerEmployeeId(id)) {
            throw new BusinessRuleException("Employee " + id + " still has direct reports");
        }
        employeeRepository.deleteById(id);   // [CrudRepository] deleteById
    }

    /** Delete by entity. Works for managed or detached instances. */
    public void delete(Employee employee) {
        employeeRepository.delete(employee);   // [CrudRepository] delete
    }

    /** Delete several by ID: one SELECT + one DELETE per row. No business guards, for brevity. */
    public void deleteByIds(List<Integer> ids) {
        employeeRepository.deleteAllById(ids);   // [CrudRepository] deleteAllById
    }

    // =====================================================================
    // ListCrudRepository: same operations, but returning List
    // =====================================================================

    /** Create many in one transaction: if one fails, none are saved. */
    public List<Employee> createAll(List<Employee> employees) {
        for (Employee e : employees) {
            if (e.getEmployeeId() != null) {
                throw new BusinessRuleException("New employee must not have an ID");
            }
            checkEmailAvailable(e.getEmail());
        }
        // A duplicate email inside the list itself is caught by the DB UNIQUE constraint at flush
        return employeeRepository.saveAll(employees);   // [ListCrudRepository] saveAll returns List
    }

    /** Whole table as a List. Fine for small tables; use paging for large ones. */
    public List<Employee> findAll() {
        return employeeRepository.findAll();   // [ListCrudRepository] findAll returns List
    }

    /** Several IDs in one query. IDs that don't exist are silently left out of the result. */
    public List<Employee> findByIds(List<Integer> ids) {
        return employeeRepository.findAllById(ids);   // [ListCrudRepository] findAllById
    }

    // =====================================================================
    // PagingAndSortingRepository: findAll(Pageable) returns Page
    // =====================================================================

    /**
     * Numbered pagination. page is zero-based. Runs a data query plus a count query,
     * so the Page knows total elements and total pages.
     */
    public Page<Employee> page(int page, int size, String sortBy, Sort.Direction direction) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),   // cap the page size
                buildSort(sortBy, direction));
        return employeeRepository.findAll(pageable);   // [PagingAndSortingRepository] findAll(Pageable)
    }

    /** Top N earners: a Page request used only to limit and order the rows. */
    public List<Employee> topEarners(int n) {
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(n, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Order.desc("salary").nullsLast()));
        return employeeRepository.findAll(pageable).getContent();   // [PagingAndSortingRepository]
    }

    // =====================================================================
    // ListPagingAndSortingRepository: findAll(Sort) returns List
    // =====================================================================

    /** Sorted list, single field. */
    public List<Employee> sorted(String sortBy, Sort.Direction direction) {
        return employeeRepository.findAll(buildSort(sortBy, direction));   // [ListPagingAndSortingRepository]
    }

    /** Sorted list, several fields: department A-Z, then salary high to low (nulls last). */
    public List<Employee> sortedByDepartmentThenSalary() {
        Sort sort = Sort.by(Sort.Order.asc("department"), Sort.Order.desc("salary").nullsLast());
        return employeeRepository.findAll(sort);   // [ListPagingAndSortingRepository]
    }

    // =====================================================================
    // JpaRepository's own extras
    // =====================================================================

    /** saveAndFlush: the INSERT is sent now, so constraint errors surface inside this method. */
    public Employee createAndFlush(Employee employee) {
        checkEmailAvailable(employee.getEmail());
        return employeeRepository.saveAndFlush(employee);   // [JpaRepository] saveAndFlush
    }

    /** One DELETE ... WHERE employee_id IN (...) statement. Fast, but skips cascade and the context. */
    public void deleteInBatch(List<Integer> ids) {
        employeeRepository.deleteAllByIdInBatch(ids);   // [JpaRepository] deleteAllByIdInBatch
    }

    // =====================================================================
    // Transactional business methods (employee table only)
    // =====================================================================

    /**
     * Replace a leaving employee with a new hire, as one atomic unit:
     *   1. insert the replacement (inherits role, department, manager and, if unset, salary)
     *   2. move the leaver's direct reports to the replacement
     *   3. mark the leaver inactive
     * If any step fails, the exception rolls everything back, including the INSERT,
     * so the table is never left with a half-done replacement.
     */
    @Transactional
    public Employee replaceEmployee(Integer leavingId, Employee replacement) {
        if (replacement.getEmployeeId() != null) {
            throw new BusinessRuleException("Replacement must be a new employee (no ID)");
        }
        Employee leaving = findOrThrow(leavingId);
        if (!Boolean.TRUE.equals(leaving.getActive())) {
            throw new BusinessRuleException("Employee " + leavingId + " is already inactive");
        }
        checkEmailAvailable(replacement.getEmail());

        // 1. replacement takes over the leaver's position
        replacement.setJobTitle(leaving.getJobTitle());
        replacement.setDepartment(leaving.getDepartment());
        replacement.setManager(leaving.getManager());
        if (replacement.getSalary() == null) {
            replacement.setSalary(leaving.getSalary());
        }
        Employee saved = employeeRepository.saveAndFlush(replacement);   // ID is generated here

        // 2. direct reports now report to the replacement (dirty checking, no save())
        for (Employee report : employeeRepository.findByManager(leaving)) {
            report.setManager(saved);
        }

        // 3. the leaver stays in the table as inactive (soft delete)
        leaving.setActive(false);
        return saved;
    }

    /**
     * Deactivate several employees, all or nothing.
     * If any ID doesn't exist, nobody is deactivated.
     */
    @Transactional
    public void deactivateAll(List<Integer> ids) {
        List<Employee> found = employeeRepository.findAllById(ids);   // [ListCrudRepository]
        if (found.size() != new HashSet<>(ids).size()) {
            throw new BusinessRuleException("One or more employees were not found; nothing was changed");
        }
        found.forEach(e -> e.setActive(false));   // dirty checking issues the UPDATEs at commit
    }

    // =====================================================================
    // helpers (private: not proxied, they run inside the caller's transaction)
    // =====================================================================

    private Employee findOrThrow(Integer id) {
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException(id));
    }

    private void checkEmailAvailable(String email) {
        if (email != null && employeeRepository.existsByEmail(email)) {
            throw new BusinessRuleException("Email already in use: " + email);
        }
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        String property = (sortBy == null) ? "employeeId" : sortBy;
        if (!SORTABLE.contains(property)) {
            throw new BusinessRuleException("Cannot sort by '" + sortBy + "'");
        }
        return Sort.by(direction == null ? Sort.Direction.ASC : direction, property);
    }
}