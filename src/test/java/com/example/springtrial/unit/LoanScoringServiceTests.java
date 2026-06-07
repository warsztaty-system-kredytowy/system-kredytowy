package com.example.springtrial.unit.loanapplication;

import com.example.springtrial.LoanApplication.CreditHistoryEntry;
import com.example.springtrial.LoanApplication.LoanApplication;
import com.example.springtrial.LoanApplication.LoanScoringService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoanScoringServiceTests {

    private final LoanScoringService scoringService = new LoanScoringService();

    @Test
    void shouldProduceHighAdvisoryScoreForComfortableApplication() {
        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("16000"));
        application.setMonthlyLiabilities(new BigDecimal("1500"));
        application.setLoanAmount(new BigDecimal("120000"));
        application.setLoanTermYears(10);
        application.setLoanPurpose("Apartment renovation and energy efficiency improvements");

        scoringService.score(application);

        assertThat(application.getSuggestedScore()).isGreaterThanOrEqualTo(new BigDecimal("8.0"));
        assertThat(application.getSuggestedScoreExplanation()).contains("- Estimated new installment");
    }

    @Test
    void shouldRecognizePolishStableLoanPurposeKeywords() {
        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("16000"));
        application.setMonthlyLiabilities(new BigDecimal("1500"));
        application.setLoanAmount(new BigDecimal("120000"));
        application.setLoanTermYears(10);
        application.setLoanPurpose("Celem kredytu jest zakup samochodu do dojazdow do pracy");

        scoringService.score(application);

        assertThat(application.getSuggestedScoreExplanation()).contains("asset-oriented");
    }

    @Test
    void shouldProduceLowAdvisoryScoreWhenDebtBurdenIsTooHigh() {
        CreditHistoryEntry overdueCredit = new CreditHistoryEntry();
        overdueCredit.setAmountStillToPay(new BigDecimal("50000"));
        overdueCredit.setMonthlyCost(new BigDecimal("2500"));
        overdueCredit.setDateTillFullRepayment(LocalDate.now().minusMonths(1));

        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("4500"));
        application.setMonthlyLiabilities(new BigDecimal("1800"));
        application.setLoanAmount(new BigDecimal("300000"));
        application.setLoanTermYears(8);
        application.setLoanPurpose("Cash");
        application.setCreditHistory(List.of(overdueCredit));

        scoringService.score(application);

        assertThat(application.getSuggestedScore()).isLessThanOrEqualTo(new BigDecimal("3.0"));
        assertThat(application.getSuggestedScoreExplanation()).contains("overdue unpaid debt");
    }
}
