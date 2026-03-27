package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service   // marks this as a Spring-managed service bean
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    // Spring automatically "injects" the repository here (Dependency Injection)
    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public LoanApplication submit(LoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();

        application.setMonthlyIncome(request.getMonthlyIncome());
        application.setMonthlyLiabilities(request.getMonthlyLiabilities());
        application.setCreditScore(request.getCreditScore());
        application.setLoanPurpose(request.getLoanPurpose());

        // These are set by the system, not the customer
        application.setSubmittedAt(LocalDateTime.now());
        application.setStatus("SUBMITTED");

        return repository.save(application); // saves to DB, returns saved object with ID
    }
}
