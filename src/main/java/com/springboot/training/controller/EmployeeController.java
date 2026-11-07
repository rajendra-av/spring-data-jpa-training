package com.springboot.training.controller;

import com.springboot.training.dto.CreateEmployeeRequest;
import com.springboot.training.dto.EmployeeResponse;
import com.springboot.training.dto.UpdateEmployeeRequest;
import com.springboot.training.service.EmployeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/employees")   // base path shared by all methods in this class
public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService service;

    // Constructor injection: the service comes from the root context
    public EmployeeController(EmployeeService service) {
        this.service = service;
    }

    // GET /employees or GET /employees?name=asha
    @GetMapping
    public List<EmployeeResponse> list(@RequestParam(required = false) String name) {
        return service.findAll(name);
    }

    // GET /employees/1
    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable @Positive Long id) {
        log.debug("controller: get({})", id);   // used later to trace the request flow
        return service.get(id);
    }

    // POST /employees, body validated by @Valid before the method runs
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = service.create(request);
        // Location header pointing to the new resource, e.g. .../employees/2
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);   // 201 Created
    }

    // PUT /employees/1, replaces the employee
    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable @Positive Long id,
                                   @Valid @RequestBody UpdateEmployeeRequest request) {
        return service.update(id, request);
    }

    // DELETE /employees/1
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)   // 204, no body
    public void delete(@PathVariable @Positive Long id) {
        service.delete(id);
    }
}