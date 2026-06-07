package com.example.springtrial.evaluation;

import com.example.springtrial.LoanApplication.CreditHistoryEntry;
import com.example.springtrial.LoanApplication.LoanApplication;
import com.example.springtrial.LoanApplication.LoanStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

@Service
public class LoanEvaluationService {

    public LoanStatus evaluate(LoanApplication app) {
        int score = 100;

        // 1. Sum existing monthly credit costs
        BigDecimal existingMonthlyCosts = app.getCreditHistory().stream()
                .map(CreditHistoryEntry::getMonthlyCost)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Estimated new installment (flat, no interest)
        BigDecimal newInstallment = app.getLoanAmount()
                .divide(BigDecimal.valueOf((long) app.getLoanTermYears() * 12), 2, RoundingMode.HALF_UP);

        // 3. DTI — total monthly obligations vs income
        BigDecimal totalObligations = app.getMonthlyLiabilities()
                .add(existingMonthlyCosts)
                .add(newInstallment);
        double dti = totalObligations
                .divide(app.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                .doubleValue();

        if (dti > 0.50)      score -= 50;
        else if (dti > 0.35) score -= 20;

        // 4. LTI — loan amount vs annual income
        double lti = app.getLoanAmount()
                .divide(app.getMonthlyIncome().multiply(BigDecimal.valueOf(12)), 4, RoundingMode.HALF_UP)
                .doubleValue();

        if (lti > 5)      score -= 40;
        else if (lti > 3) score -= 15;

        // 5. Overdue debts — past repayment date but still has balance
        boolean hasOverdue = app.getCreditHistory().stream().anyMatch(e ->
                e.getAmountStillToPay() != null &&
                e.getAmountStillToPay().compareTo(BigDecimal.ZERO) > 0 &&
                e.getDateTillFullRepayment() != null &&
                e.getDateTillFullRepayment().isBefore(LocalDate.now())
        );
        if (hasOverdue) score -= 60;

        // 6. Map score → status
        if (score >= 70) return LoanStatus.APPROVED;
        if (score >= 40) return LoanStatus.IN_PROCESSING;
        return LoanStatus.DENIED;
    }
}