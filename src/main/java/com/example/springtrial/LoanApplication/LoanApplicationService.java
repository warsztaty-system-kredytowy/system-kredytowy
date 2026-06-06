package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final LlmEvaluationService llmEvaluationService;
    private final LoanScoringService loanScoringService;

    public LoanApplicationService(LoanApplicationRepository repository, LlmEvaluationService llmEvaluationService,
                                  LoanScoringService loanScoringService) {
        this.repository = repository;
        this.llmEvaluationService = llmEvaluationService;
        this.loanScoringService = loanScoringService;
    }

    private String getCurrentUserOwnerKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                return oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getName();
            } else if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
                String email = (String) oauth2User.getAttribute("email");
                return email != null ? email : oauth2User.getName();
            }
            return auth.getName();
        }
        
        // Fallback to session ID for anonymous users
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(true);
            return "anonymous_" + session.getId();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String getCurrentUserFullName() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            if (auth.getPrincipal() instanceof OidcUser oidcUser) {
                if (oidcUser.getFullName() != null) return oidcUser.getFullName();
                if (oidcUser.getEmail() != null) return oidcUser.getEmail();
            } else if (auth.getPrincipal() instanceof OAuth2User oauth2User) {
                String name = (String) oauth2User.getAttribute("name");
                if (name != null) return name;
                String email = (String) oauth2User.getAttribute("email");
                if (email != null) return email;
            }
            return auth.getName();
        }
        return "System / AI";
    }

    public boolean isCurrentUserEmployee() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return auth.getAuthorities().stream()
                    .anyMatch(ga -> ga.getAuthority().equals("ROLE_EMPLOYEE"));
        }
        return false;
    }

    public boolean isCurrentUserOwnerOf(Long id) {
        return findById(id)
                .map(app -> app.getOwner() != null && app.getOwner().equals(getCurrentUserOwnerKey()))
                .orElse(false);
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
        
        // Record who evaluated this step
        history.setEvaluatedBy(getCurrentUserFullName());
        
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
        
        // Assign owner key (session ID or username/email)
        application.setOwner(getCurrentUserOwnerKey());
        loanScoringService.score(application);
        
        addHistory(application, LoanStatus.NEW, "Application submitted");

        return repository.save(application);
    }

    public List<LoanApplication> findAll() {
        return repository.findAll();
    }

    public List<LoanApplication> findAllForCurrentUser() {
        return repository.findByOwner(getCurrentUserOwnerKey());
    }

    public Optional<LoanApplication> findById(Long id) {
        return repository.findById(id);
    }

    public Optional<LoanApplication> findByIdForCurrentUser(Long id) {
        return repository.findByIdAndOwner(id, getCurrentUserOwnerKey());
    }

    public Optional<LoanApplication> startReview(Long id) {
        return repository.findById(id).map(app -> {
            loanScoringService.score(app);
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
        // Allow owner to appeal
        return findByIdForCurrentUser(id).map(app -> {
            if (app.getStatus() == LoanStatus.DENIED && !app.isFinalDecision()) {
                addHistory(app, LoanStatus.APPEALED, "User appealed. Reason: " + reason);
            }
            return repository.save(app);
        });
    }

    public Optional<LoanApplication> evaluate(Long id) {
        return repository.findById(id).map(app -> {
            loanScoringService.score(app);
            llmEvaluationService.evaluateApplication(app);
            return repository.save(app);
        });
    }
}
