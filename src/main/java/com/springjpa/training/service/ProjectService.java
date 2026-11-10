package com.springjpa.training.service;

import com.springjpa.training.entity.Department;
import com.springjpa.training.entity.Project;
import com.springjpa.training.exception.BusinessRuleException;
import com.springjpa.training.exception.DepartmentNotFoundException;
import com.springjpa.training.exception.ProjectNotFoundException;
import com.springjpa.training.repository.DepartmentRepository;
import com.springjpa.training.repository.ProjectRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Project business logic.
 *
 * @Transactional only on methods that rely on dirty checking (load, change, no save()).
 * Single-call methods need none, because every repository call is already transactional.
 *
 * Assumption: status is free text in the table (default 'Active'); the values and the allowed
 * transitions below are an example rule set, adjust them to your data.
 */
@Service
public class ProjectService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE =
            Set.of("projectId", "projectName", "budget", "startDate", "endDate", "status");

    // status -> statuses it may move to; Completed and Cancelled are final
    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "Active", Set.of("On Hold", "Completed", "Cancelled"),
            "On Hold", Set.of("Active", "Cancelled"),
            "Completed", Set.<String>of(),
            "Cancelled", Set.<String>of());

    private final ProjectRepository projectRepository;
    private final DepartmentRepository departmentRepository;

    public ProjectService(ProjectRepository projectRepository, DepartmentRepository departmentRepository) {
        this.projectRepository = projectRepository;
        this.departmentRepository = departmentRepository;
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    public Project getById(Integer id) {
        return findOrThrow(id);
    }

    public List<Project> findAll() {
        return projectRepository.findAll();
    }

    public List<Project> findByStatus(String status) {
        return projectRepository.findByStatus(status);
    }

    public List<Project> findByDepartmentId(Integer departmentId) {
        return projectRepository.findByDepartmentDepartmentId(departmentId);
    }

    public long count() {
        return projectRepository.count();
    }

    public Page<Project> page(int page, int size, String sortBy, Sort.Direction direction) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                buildSort(sortBy, direction));
        return projectRepository.findAll(pageable);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    /**
     * Business rules: unique name, department exists and is active,
     * end date not before start date, budget within the department budget.
     * No @Transactional: the only write is the final save.
     */
    public Project create(String projectName, Integer departmentId,
                          LocalDate startDate, LocalDate endDate, BigDecimal budget) {
        if (projectRepository.existsByProjectName(projectName)) {
            throw new BusinessRuleException("Project name already in use: " + projectName);
        }
        Department dept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(departmentId));
        if (!Boolean.TRUE.equals(dept.getActive())) {
            throw new BusinessRuleException("Department " + departmentId + " is inactive");
        }
        checkDates(startDate, endDate);
        checkBudget(budget, dept);

        Project project = new Project(projectName, dept, startDate);
        project.setEndDate(endDate);
        project.setBudget(budget);
        return projectRepository.save(project);
    }

    // ------------------------------------------------------------------
    // UPDATE (dirty checking: load, change, no save() call)
    // ------------------------------------------------------------------

    @Transactional
    public Project reschedule(Integer id, LocalDate startDate, LocalDate endDate) {
        Project project = findOrThrow(id);
        checkDates(startDate, endDate);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        return project;
    }

    @Transactional
    public Project changeBudget(Integer id, BigDecimal newBudget) {
        Project project = findOrThrow(id);
        checkBudget(newBudget, project.getDepartment());   // lazy department loads inside the transaction
        project.setBudget(newBudget);
        return project;
    }

    @Transactional
    public Project changeStatus(Integer id, String newStatus) {
        Project project = findOrThrow(id);
        applyStatus(project, newStatus);
        return project;
    }

    /** Mark as Completed and stamp today as the end date if none was set. */
    @Transactional
    public Project complete(Integer id) {
        Project project = findOrThrow(id);
        applyStatus(project, "Completed");
        if (project.getEndDate() == null) {
            project.setEndDate(LocalDate.now());
        }
        return project;
    }

    /** Move a project to another active department (changes the owning side, project.department_id). */
    @Transactional
    public Project moveToDepartment(Integer projectId, Integer newDepartmentId) {
        Project project = findOrThrow(projectId);
        Department target = departmentRepository.findById(newDepartmentId)
                .orElseThrow(() -> new DepartmentNotFoundException(newDepartmentId));
        if (!Boolean.TRUE.equals(target.getActive())) {
            throw new BusinessRuleException("Department " + newDepartmentId + " is inactive");
        }
        checkBudget(project.getBudget(), target);
        project.setDepartment(target);
        return project;
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    /**
     * Business rule: only finished projects (Completed or Cancelled) can be deleted.
     * Once the EmployeeProject entity exists, also refuse (or remove) projects that still
     * have employee assignments, since employee_project references project.
     */
    public void delete(Integer id) {
        Project project = findOrThrow(id);
        String status = project.getStatus();
        if (!"Completed".equals(status) && !"Cancelled".equals(status)) {
            throw new BusinessRuleException("Only Completed or Cancelled projects can be deleted");
        }
        projectRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers (private: not proxied, they run inside the caller's transaction)
    // ------------------------------------------------------------------

    private Project findOrThrow(Integer id) {
        return projectRepository.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private void applyStatus(Project project, String newStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.get(project.getStatus());
        if (allowed == null || !allowed.contains(newStatus)) {
            throw new BusinessRuleException(
                    "Cannot change status from '" + project.getStatus() + "' to '" + newStatus + "'");
        }
        project.setStatus(newStatus);
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new BusinessRuleException("Start date is required");
        }
        if (end != null && end.isBefore(start)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }
    }

    // Project budget must be non-negative and must not exceed its department's budget (when both are set)
    private void checkBudget(BigDecimal budget, Department dept) {
        if (budget == null) {
            return;
        }
        if (budget.signum() < 0) {
            throw new BusinessRuleException("Budget cannot be negative");
        }
        if (dept.getBudget() != null && budget.compareTo(dept.getBudget()) > 0) {
            throw new BusinessRuleException("Project budget exceeds the department budget");
        }
    }

    private Sort buildSort(String sortBy, Sort.Direction direction) {
        String property = (sortBy == null) ? "projectId" : sortBy;
        if (!SORTABLE.contains(property)) {
            throw new BusinessRuleException("Cannot sort by '" + sortBy + "'");
        }
        return Sort.by(direction == null ? Sort.Direction.ASC : direction, property);
    }
}