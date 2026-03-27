package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public LoanApplication submit(LoanApplicationRequest request) {
        LoanApplication application = new LoanApplication();
        application.setLoanAmount(request.getLoanAmount());
        application.setLoanTermYears(request.getLoanTermYears());
        application.setMonthlyIncome(request.getMonthlyIncome());
        application.setMonthlyLiabilities(request.getMonthlyLiabilities());
        application.setCreditHistory(request.getCreditHistory());
        application.setLoanPurpose(request.getLoanPurpose());

        application.setSubmittedAt(LocalDateTime.now());
        application.setStatus(LoanStatus.CREATED);

        return repository.save(application);
    }

    public Optional<LoanApplication> findById(Long id) {
        return repository.findById(id);
    }
}