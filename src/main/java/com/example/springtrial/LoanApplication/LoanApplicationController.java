package com.example.springtrial.LoanApplication;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/loans")
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanApplication submitApplication(@Valid @RequestBody LoanApplicationRequest request) {
        return service.submit(request);
    }

    @GetMapping("/{id}")
    public LoanApplication getApplication(@PathVariable("id") Long id) {
        return service.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }
}