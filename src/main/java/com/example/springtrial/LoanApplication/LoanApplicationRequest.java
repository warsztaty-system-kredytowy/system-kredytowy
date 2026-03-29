package com.example.springtrial.LoanApplication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class LoanApplicationRequest {
    @NotNull(message = "Loan amount is required")
    @Positive(message = "Loan amount must be positive")
    private BigDecimal loanAmount;

    @NotNull(message = "Loan term (years) is required")
    @Min(value = 1, message = "Loan term must be at least 1 year")
    private Integer loanTermYears;

    @NotNull(message = "Monthly income is required")
    @Positive(message = "Monthly income must be positive")
    private BigDecimal monthlyIncome;

    @NotNull(message = "Monthly liabilities are required")
    @PositiveOrZero(message = "Monthly liabilities cannot be negative")
    private BigDecimal monthlyLiabilities;

    private List<CreditHistoryEntry> creditHistory;
    @NotBlank(message = "Loan purpose must not be blank")
    private String loanPurpose;
}