package com.example.springtrial.LoanApplication;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController              // this class handles HTTP requests and returns JSON
@RequestMapping("/api/loans") // all endpoints here start with /api/loans
public class LoanApplicationController {

    private final LoanApplicationService service;

    public LoanApplicationController(LoanApplicationService service) {
        this.service = service;
    }

    @PostMapping             // handles POST /api/loans
    @ResponseStatus(HttpStatus.CREATED)  // returns HTTP 201 on success
    public LoanApplication submitApplication(@RequestBody LoanApplicationRequest request) {
        return service.submit(request);
    }
}