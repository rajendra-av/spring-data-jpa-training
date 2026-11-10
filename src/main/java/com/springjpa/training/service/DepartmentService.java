package com.springjpa.training.service;

import com.springjpa.training.entity.Department;
import com.springjpa.training.entity.Employee;
import com.springjpa.training.entity.Project;
import com.springjpa.training.exception.BusinessRuleException;
import com.springjpa.training.exception.DepartmentNotFoundException;
import com.springjpa.training.exception.EmployeeNotFoundException;
import com.springjpa.training.repository.DepartmentRepository;
import com.springjpa.training.repository.EmployeeRepository;
import com.springjpa.training.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Department business logic.
 *
 * @Transactional is used only where needed: methods that rely on dirty checking
 * (load, change, no save) or must change several rows all-or-nothing.
 * Methods with a single repository call need none, because every repository call
 * is already transactional.
 */
@Service
public class DepartmentService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE =
            Set.of("departmentId", "departmentName", "location", "budget", "active");

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;

    // Single constructor: Spring injects the three repository proxies, no @Autowired needed
    public DepartmentService(DepartmentRepository departmentRepository,
                             EmployeeRepository employeeRepository,
                             ProjectRepository projectRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    public Department getById(Integer id) {
        return findOrThrow(id);
    }

    public Department getByName(String name) {
        return departmentRepository.findByDepartmentName(name)
                .orElseThrow(() -> new DepartmentNotFoundException(name));
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public List<Department> findActive() {
        return departmentRepository.findByActiveTrue();
    }

    public List<Department> findByLocation(String location) {
        return departmentRepository.findByLocation(location);
    }

    public long count() {
        return departmentRepository.count();
    }

    /** Numbered pagination; sort field is checked against a whitelist of entity fields. */
    public Page<Department> page(int page, int size, String sortBy, Sort.Direction direction) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                buildSort(sortBy, direction));
        return departmentRepository.findAll(pageable);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    /** The name must be unique: employee.department refers to it. No @Transactional: one save. */
    public Department create(Department department) {
        if (department.getDepartmentId() != null) {
            throw new BusinessRuleException("New department must not have an ID");
        }
        checkNameAvailable(department.getDepartmentName());
        checkBudget(department.getBudget());
        return departmentRepository.save(department);
    }

    // ------------------------------------------------------------------
    // UPDATE (dirty checking: load, change, no save() call)
    // ------------------------------------------------------------------

    @Transactional
    public Department updateDetails(Integer id, String location, BigDecimal budget) {
        Department dept = findOrThrow(id);
        if (location != null) {
            dept.setLocation(location);
        }
        if (budget != null) {
            checkBudget(budget);
            dept.setBudget(budget);
        }
        return dept;
    }

    /**
     * Rename. employee.department stores this NAME as its foreign key; the DDL's
     * ON UPDATE CASCADE makes the database update those employee rows automatically.
     */
    @Transactional
    public Department rename(Integer id, String newName) {
        Department dept = findOrThrow(id);
        if (!dept.getDepartmentName().equals(newName)) {
            checkNameAvailable(newName);
            dept.setDepartmentName(newName);
        }
        return dept;
    }

    /** Business rule: the manager must be an existing, active employee. */
    @Transactional
    public Department assignManager(Integer departmentId, Integer employeeId) {
        Department dept = findOrThrow(departmentId);
        Employee manager = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        if (!Boolean.TRUE.equals(manager.getActive())) {
            throw new BusinessRuleException("An inactive employee cannot manage a department");
        }
        dept.setManager(manager);   // owning side: writes department.manager_id
        return dept;
    }

    /** Business rule: a department with active projects cannot be deactivated. */
    @Transactional
    public Department deactivate(Integer id) {
        Department dept = findOrThrow(id);
        if (projectRepository.existsByDepartmentAndStatus(dept, "Active")) {
            throw new BusinessRuleException("Department " + id + " still has active projects");
        }
        dept.setActive(false);
        return dept;
    }

    @Transactional
    public Department activate(Integer id) {
        Department dept = findOrThrow(id);
        dept.setActive(true);
        return dept;
    }

    /**
     * Merge one department into another, all or nothing:
     *   1. every employee of the source moves to the target
     *   2. every project of the source moves to the target
     *   3. the source is deactivated
     * Any failure rolls back all three steps, so nothing is left half moved.
     * Changes go through the owning sides (Employee.department, Project.department).
     */
    @Transactional
    public Department mergeInto(Integer sourceId, Integer targetId) {
        if (sourceId.equals(targetId)) {
            throw new BusinessRuleException("Cannot merge a department into itself");
        }
        Department source = findOrThrow(sourceId);
        Department target = findOrThrow(targetId);
        if (!Boolean.TRUE.equals(target.getActive())) {
            throw new BusinessRuleException("Target department " + targetId + " is inactive");
        }
        for (Employee employee : employeeRepository.findByDepartment(source)) {
            employee.setDepartment(target);   // dirty checking, no save()
        }
        for (Project project : projectRepository.findByDepartment(source)) {
            project.setDepartment(target);
        }
        source.setActive(false);
        return target;
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    /**
     * Business rule: only an empty department can be deleted (no employees, no projects).
     * The database would reject it anyway with a foreign key error; this gives a clear message.
     */
    public void delete(Integer id) {
        Department dept = findOrThrow(id);
        if (employeeRepository.existsByDepartment(dept)) {
            throw new BusinessRuleException("Department " + id + " still has employees");
        }
        if (projectRepository.existsByDepartment(dept)) {
            throw new BusinessRuleException("Department " + id + " still has projects");
        }
        departmentRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers (private: not proxied, they run inside the caller's transaction)
    // ------------------------------------------------------------------

    private Department findOrThrow(Integer id) {
        return departmentRepository.findById(id).orElseThrow(() -> new DepartmentNotFoundException(id));
    }

    private void checkNameAvailable(String name) {
        if (departmentRepository.existsByDepartmentName(name)) {
            throw new BusinessRuleException("Department name already in use: " + name);
        }
    }

    private void checkBudget(BigDecimal budget) {
        if (budget != null && budget.signum() < 0) {
            throw new BusinessRuleException("Budget cannot be negative");
        }
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        String property = (sortBy == null) ? "departmentId" : sortBy;
        if (!SORTABLE.contains(property)) {
            throw new BusinessRuleException("Cannot sort by '" + sortBy + "'");
        }
        return Sort.by(direction == null ? Sort.Direction.ASC : direction, property);
    }
}