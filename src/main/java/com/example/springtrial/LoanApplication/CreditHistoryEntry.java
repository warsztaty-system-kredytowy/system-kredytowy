package com.example.springtrial.LoanApplication;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
public class CreditHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amountLoaned;        // original loan amount
    private BigDecimal amountStillToPay;    // remaining balance
    private LocalDate dateTillFullRepayment; // expected payoff date
    private BigDecimal monthlyCost;         // monthly installment
}