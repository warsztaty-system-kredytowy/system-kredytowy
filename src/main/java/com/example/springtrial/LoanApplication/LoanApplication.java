// src/main/java/com/example/springtrial/LoanApplication/LoanApplication.java
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

    // Financial data
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyLiabilities;

    // Credit history — each entry is its own row in a joined table
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "loan_application_id")
    private List<CreditHistoryEntry> creditHistory;

    // Loan purpose (free text from the customer)
    @Column(length = 2000)
    private String loanPurpose;

    // Timestamp of when the customer submitted the form
    private LocalDateTime submittedAt;

    // Current status of this application
    @Enumerated(EnumType.STRING)
    private LoanStatus status;
}