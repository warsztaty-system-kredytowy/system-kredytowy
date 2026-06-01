package com.example.springtrial.LoanApplication;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

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

    @GetMapping
    public List<LoanApplication> getAllApplications() {
        return service.findAllForCurrentUser();
    }

    @GetMapping("/{id}")
    public LoanApplication getApplication(@PathVariable("id") Long id) {
        return service.findByIdForCurrentUser(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }

    @PutMapping("/{id}/approve")
    public LoanApplication approveApplication(@PathVariable("id") Long id) {
        if (service.isCurrentUserOwnerOf(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot approve your own application!");
        }
        return service.approve(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }

    @PutMapping("/{id}/deny")
    public LoanApplication denyApplication(
            @PathVariable("id") Long id,
            @RequestBody(required = false) DenyRequest body) {
        if (service.isCurrentUserOwnerOf(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot deny your own application!");
        }
        String reason = (body != null) ? body.getReason() : null;
        return service.deny(id, reason)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }

    @PostMapping("/{id}/evaluate")
    public LoanApplication evaluateApplication(@PathVariable("id") Long id) {
        if (service.isCurrentUserOwnerOf(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot evaluate your own application!");
        }
        return service.evaluate(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }

    @PostMapping("/{id}/review")
    public LoanApplication startReview(@PathVariable("id") Long id) {
        if (service.isCurrentUserOwnerOf(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot review your own application!");
        }
        return service.startReview(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }

    @PostMapping("/{id}/appeal")
    public LoanApplication appealApplication(
            @PathVariable("id") Long id,
            @RequestBody(required = false) AppealRequest body) {
        String reason = (body != null) ? body.getReason() : null;
        return service.appeal(id, reason)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Application not found with id: " + id));
    }
}