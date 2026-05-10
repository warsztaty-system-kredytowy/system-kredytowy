package com.example.springtrial.LoanApplication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyLiabilities;
    private BigDecimal loanAmount;
    private Integer loanTermYears;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_application_id")
    private List<CreditHistoryEntry> creditHistory;

    @Column(length = 2000)
    private String loanPurpose;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @Column(length = 2000)
    private String denialReason;

    @Column(precision = 3, scale = 1)
    private BigDecimal llmScore;

    @Column(length = 4000)
    private String llmExplanation;
}
