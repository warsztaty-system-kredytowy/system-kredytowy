package com.example.springtrial.LoanApplication;


import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class LoanApplicationRequest {
    private BigDecimal monthlyIncome;
    private BigDecimal monthlyLiabilities;
    private Integer creditScore;
    private String loanPurpose;
}