package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final LlmEvaluationService llmEvaluationService;

    public LoanApplicationService(LoanApplicationRepository repository, LlmEvaluationService llmEvaluationService) {
        this.repository = repository;
        this.llmEvaluationService = llmEvaluationService;
    }

    private void addHistory(LoanApplication app, LoanStatus status, String note) {
        if (app.getStatusHistory() == null) {
            app.setStatusHistory(new ArrayList<>());
        }
        ApplicationStatusHistory history = new ApplicationStatusHistory();
        history.setApplication(app);
        history.setStatus(status);
        history.setTimestamp(LocalDateTime.now());
        history.setNote(note);
        app.getStatusHistory().add(history);
        app.setStatus(status);
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
        addHistory(application, LoanStatus.NEW, "Application submitted");

        return repository.save(application);
    }

    public List<LoanApplication> findAll() {
        return repository.findAll();
    }

    public Optional<LoanApplication> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<LoanApplication> startReview(Long id) {
        return repository.findById(id).map(app -> {
            if (app.getStatus() == LoanStatus.NEW) {
                addHistory(app, LoanStatus.IN_PROCESSING, "Review started by employee");
            }
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> approve(Long id) {
        return repository.findById(id).map(app -> {
            if (app.getStatus() == LoanStatus.APPEALED) {
                app.setFinalDecision(true);
            }
            addHistory(app, LoanStatus.APPROVED, "Application approved by employee");
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> deny(Long id, String reason) {
        return repository.findById(id).map(app -> {
            if (app.getStatus() == LoanStatus.APPEALED) {
                app.setFinalDecision(true);
            }
            app.setDenialReason(reason);
            addHistory(app, LoanStatus.DENIED, "Application denied. Reason: " + (reason != null ? reason : "None"));
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> appeal(Long id, String reason) {
        return repository.findById(id).map(app -> {
            if (app.getStatus() == LoanStatus.DENIED && !app.isFinalDecision()) {
                addHistory(app, LoanStatus.APPEALED, "User appealed. Reason: " + reason);
            }
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> evaluate(Long id) {
        return repository.findById(id).map(app -> {
            llmEvaluationService.evaluateApplication(app);
            return repository.save(app);
        });
    }
}