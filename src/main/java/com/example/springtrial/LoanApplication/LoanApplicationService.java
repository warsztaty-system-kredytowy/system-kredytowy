package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
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

    public List<LoanApplication> findAll() {
        return repository.findAll();
    }

    public Optional<LoanApplication> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<LoanApplication> approve(Long id) {
        return repository.findById(id).map(app -> {
            app.setStatus(LoanStatus.ACCEPTED);
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> deny(Long id, String reason) {
        return repository.findById(id).map(app -> {
            app.setStatus(LoanStatus.DENIED);
            app.setDenialReason(reason);
            return repository.save(app);
        });
    }
}