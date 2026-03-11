package com.chhavi.busbuddy_backend.controller;

import com.chhavi.busbuddy_backend.dto.request.CreateEmployeeRequest;
import com.chhavi.busbuddy_backend.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping("/companies/{companyId}/employees")
    public ResponseEntity<String> createEmployee(@PathVariable String companyId,
                                                 @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.createEmployee(companyId, request));
    }

    @GetMapping("/companies/{companyId}/employees")
    public ResponseEntity<List<Map<String, Object>>> listEmployees(@PathVariable String companyId) {
        return ResponseEntity.ok(employeeService.listEmployees(companyId));
    }
}
