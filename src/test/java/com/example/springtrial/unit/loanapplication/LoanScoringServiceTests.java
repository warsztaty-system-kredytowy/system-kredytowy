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

    @Test
    void shouldPenalizeMissingIncomeAndStayWithinBounds() {
        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(BigDecimal.ZERO);
        application.setMonthlyLiabilities(new BigDecimal("500"));
        application.setLoanAmount(new BigDecimal("80000"));
        application.setLoanTermYears(5);
        application.setLoanPurpose("Cash");

        scoringService.score(application);

        assertThat(application.getSuggestedScoreExplanation())
                .contains("No positive monthly income was provided.");
        assertThat(application.getSuggestedScore())
                .isGreaterThanOrEqualTo(new BigDecimal("1.0"))
                .isLessThanOrEqualTo(new BigDecimal("10.0"));
    }

    @Test
    void shouldNeverDropBelowTheMinimumScore() {
        // A deliberately worst-case application must still clamp at the 1.0 floor.
        CreditHistoryEntry overdue = new CreditHistoryEntry();
        overdue.setAmountStillToPay(new BigDecimal("90000"));
        overdue.setMonthlyCost(new BigDecimal("4000"));
        overdue.setDateTillFullRepayment(LocalDate.now().minusYears(1));

        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("2000"));
        application.setMonthlyLiabilities(new BigDecimal("1900"));
        application.setLoanAmount(new BigDecimal("500000"));
        application.setLoanTermYears(40);
        application.setLoanPurpose("x");
        application.setCreditHistory(List.of(overdue));

        scoringService.score(application);

        assertThat(application.getSuggestedScore()).isEqualByComparingTo(new BigDecimal("1.0"));
    }

    @Test
    void shouldFlagSeveralActiveCreditObligations() {
        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("16000"));
        application.setMonthlyLiabilities(new BigDecimal("1000"));
        application.setLoanAmount(new BigDecimal("60000"));
        application.setLoanTermYears(10);
        application.setLoanPurpose("Apartment renovation and improvements");
        application.setCreditHistory(List.of(
                activeCredit(new BigDecimal("100")),
                activeCredit(new BigDecimal("100")),
                activeCredit(new BigDecimal("100"))));

        scoringService.score(application);

        assertThat(application.getSuggestedScoreExplanation())
                .contains("several active credit obligations");
    }

    @Test
    void scoreShouldBeRoundedToASingleDecimalPlace() {
        LoanApplication application = new LoanApplication();
        application.setMonthlyIncome(new BigDecimal("16000"));
        application.setMonthlyLiabilities(new BigDecimal("1500"));
        application.setLoanAmount(new BigDecimal("120000"));
        application.setLoanTermYears(10);
        application.setLoanPurpose("Apartment renovation");

        scoringService.score(application);

        assertThat(application.getSuggestedScore().scale()).isEqualTo(1);
    }

    private static CreditHistoryEntry activeCredit(BigDecimal monthlyCost) {
        CreditHistoryEntry entry = new CreditHistoryEntry();
        entry.setAmountStillToPay(new BigDecimal("5000"));
        entry.setMonthlyCost(monthlyCost);
        entry.setDateTillFullRepayment(LocalDate.now().plusYears(2));
        return entry;
    }
}
