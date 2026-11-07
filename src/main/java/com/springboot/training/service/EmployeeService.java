package com.springboot.training.service;


import com.springboot.training.dto.CreateEmployeeRequest;
import com.springboot.training.dto.EmployeeResponse;
import com.springboot.training.dto.UpdateEmployeeRequest;
import com.springboot.training.exception.EmployeeNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class EmployeeService {

    // In-memory store standing in for a database. A service bean is a singleton shared by
    // all request threads, so a thread-safe map is required.
    private final Map<Long, EmployeeResponse> Employees = new ConcurrentHashMap<>();

    // Generates ids, like a database sequence. Thread-safe and starts at 0, so the first id is 1.
    private final AtomicLong idCounter = new AtomicLong();

    public EmployeeService() {
        // Sample record so that GET /Employees/1 works before any POST. Remove if not wanted.
        create(new CreateEmployeeRequest("Asha", "asha@example.com"));
    }

    // Returns all Employees ordered by id, optionally filtered by name (case-insensitive).
    public List<EmployeeResponse> findAll(String name) {
        return Employees.values().stream()
                .filter(c -> name == null || c.name().toLowerCase().contains(name.toLowerCase()))
                .sorted(Comparator.comparing(EmployeeResponse::id))
                .toList();
    }

    public EmployeeResponse get(Long id) {
        EmployeeResponse Employee = Employees.get(id);
        if (Employee == null) {
            throw new EmployeeNotFoundException(id);
        }
        return Employee;
    }

    public EmployeeResponse create(CreateEmployeeRequest request) {
        long id = idCounter.incrementAndGet();
        EmployeeResponse Employee = new EmployeeResponse(id, request.name(), request.email());
        Employees.put(id, Employee);
        return Employee;
    }

    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        // Replaces the entry only if the id exists; returns null when it does not.
        EmployeeResponse updated = Employees.computeIfPresent(id,
                (key, existing) -> new EmployeeResponse(key, request.name(), request.email()));
        if (updated == null) {
            throw new EmployeeNotFoundException(id);
        }
        return updated;
    }

    public void delete(Long id) {
        // remove() returns the removed value, or null if the id was not present.
        if (Employees.remove(id) == null) {
            throw new EmployeeNotFoundException(id);
        }
    }
}
