package com.example.springtrial.LoanApplication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity                    // tells Spring this class = a DB table
@Getter                    // Lombok: auto-generates getters
@Setter                    // Lombok: auto-generates setters
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto-increment ID
    private Long id;

    // Financial data
    private BigDecimal monthlyIncome;       // e.g. 5000.00
    private BigDecimal monthlyLiabilities;  // e.g. 1200.00
    private Integer creditScore;            // e.g. 720

    // Loan purpose (free text from the customer)
    @Column(length = 2000)
    private String loanPurpose;

    // Metadata
    private LocalDateTime submittedAt;      // set automatically on creation
    private String status;                  // starts as "SUBMITTED"
}