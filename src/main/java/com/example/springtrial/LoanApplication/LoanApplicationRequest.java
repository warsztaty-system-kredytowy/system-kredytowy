package com.example.springtrial.LoanApplication;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class LoanApplicationRequest {
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyLiabilities;
    private List<CreditHistoryEntry> creditHistory;
    private String loanPurpose;
}