package com.example.springtrial.LoanApplication;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class LoanScoringService {

    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal TEN = BigDecimal.TEN;
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final List<String> STABLE_PURPOSE_PATTERNS = List.of(
            "home", "house", "flat", "apartment", "renovat", "educat", "business", "car",
            "dom", "mieszk", "remont", "renowac", "edukac", "studi", "firm", "dzialaln",
            "samoch"
    );

    public void score(LoanApplication application) {
        ScoreDetails details = calculate(application);
        application.setSuggestedScore(details.score());
        application.setSuggestedScoreExplanation(details.explanation());
    }

    public ScoreDetails calculate(LoanApplication application) {
        BigDecimal score = TEN;
        List<String> notes = new ArrayList<>();

        BigDecimal monthlyIncome = nonNegative(application.getMonthlyIncome());
        BigDecimal monthlyLiabilities = nonNegative(application.getMonthlyLiabilities());
        BigDecimal loanAmount = nonNegative(application.getLoanAmount());
        int termYears = application.getLoanTermYears() == null ? 0 : application.getLoanTermYears();

        BigDecimal existingInstallments = existingInstallments(application);
        BigDecimal newInstallment = estimateNewInstallment(loanAmount, termYears);
        BigDecimal totalMonthlyObligations = monthlyLiabilities.add(existingInstallments).add(newInstallment);
        BigDecimal disposableAfterNewLoan = monthlyIncome.subtract(totalMonthlyObligations);

        if (monthlyIncome.compareTo(BigDecimal.ZERO) <= 0) {
            score = score.subtract(new BigDecimal("5.0"));
            notes.add("No positive monthly income was provided.");
        } else {
            BigDecimal dti = ratio(totalMonthlyObligations, monthlyIncome);
            if (dti.compareTo(new BigDecimal("0.65")) > 0) {
                score = score.subtract(new BigDecimal("3.2"));
                notes.add("Debt-to-income ratio is above 65% after the requested loan.");
            } else if (dti.compareTo(new BigDecimal("0.50")) > 0) {
                score = score.subtract(new BigDecimal("2.3"));
                notes.add("Debt-to-income ratio is elevated, above 50%.");
            } else if (dti.compareTo(new BigDecimal("0.35")) > 0) {
                score = score.subtract(new BigDecimal("1.1"));
                notes.add("Debt-to-income ratio is acceptable but not especially comfortable.");
            } else {
                score = score.add(new BigDecimal("0.4"));
                notes.add("Debt-to-income ratio remains in a healthy range.");
            }

            BigDecimal lti = ratio(loanAmount, monthlyIncome.multiply(TWELVE));
            if (lti.compareTo(new BigDecimal("5.0")) > 0) {
                score = score.subtract(new BigDecimal("2.0"));
                notes.add("Requested amount is high compared with annual income.");
            } else if (lti.compareTo(new BigDecimal("3.0")) > 0) {
                score = score.subtract(new BigDecimal("0.9"));
                notes.add("Requested amount is moderate compared with annual income.");
            } else {
                score = score.add(new BigDecimal("0.3"));
                notes.add("Requested amount is proportionate to annual income.");
            }
        }

        if (termYears <= 0) {
            score = score.subtract(new BigDecimal("1.5"));
            notes.add("Loan term is missing or invalid.");
        } else if (termYears > 30) {
            score = score.subtract(new BigDecimal("0.8"));
            notes.add("Loan term is very long, increasing long-term repayment risk.");
        } else if (termYears <= 5) {
            score = score.add(new BigDecimal("0.2"));
            notes.add("Loan term is short, limiting long-term exposure.");
        }

        if (disposableAfterNewLoan.compareTo(BigDecimal.ZERO) < 0) {
            score = score.subtract(new BigDecimal("3.0"));
            notes.add("Disposable income would be negative after the estimated new installment.");
        } else if (newInstallment.compareTo(BigDecimal.ZERO) > 0
                && disposableAfterNewLoan.compareTo(newInstallment) < 0) {
            score = score.subtract(new BigDecimal("1.4"));
            notes.add("Post-loan disposable income is lower than one estimated installment.");
        } else if (newInstallment.compareTo(BigDecimal.ZERO) > 0
                && disposableAfterNewLoan.compareTo(newInstallment.multiply(BigDecimal.valueOf(2))) >= 0) {
            score = score.add(new BigDecimal("0.4"));
            notes.add("Disposable income leaves at least two estimated installments of buffer.");
        }

        CreditSignals creditSignals = creditSignals(application);
        if (creditSignals.overdueCount() > 0) {
            score = score.subtract(new BigDecimal("2.5"));
            notes.add("Existing credit history contains overdue unpaid debt.");
        }
        if (creditSignals.activeCreditCount() >= 3) {
            score = score.subtract(new BigDecimal("0.8"));
            notes.add("Applicant already has several active credit obligations.");
        } else if (creditSignals.activeCreditCount() == 0) {
            score = score.subtract(new BigDecimal("0.2"));
            notes.add("No active credit history was provided, so repayment behavior is harder to assess.");
        }

        String purpose = application.getLoanPurpose();
        if (purpose == null || purpose.trim().length() < 10) {
            score = score.subtract(new BigDecimal("0.4"));
            notes.add("Loan purpose is short or missing.");
        } else if (looksLikeStablePurpose(purpose)) {
            score = score.add(new BigDecimal("0.3"));
            notes.add("Loan purpose appears specific and asset-oriented.");
        }

        BigDecimal finalScore = clamp(score).setScale(1, RoundingMode.HALF_UP);
        return new ScoreDetails(finalScore, buildExplanation(finalScore, newInstallment, disposableAfterNewLoan, notes));
    }

    private BigDecimal existingInstallments(LoanApplication application) {
        if (application.getCreditHistory() == null) {
            return BigDecimal.ZERO;
        }
        return application.getCreditHistory().stream()
                .map(CreditHistoryEntry::getMonthlyCost)
                .filter(Objects::nonNull)
                .map(this::nonNegative)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CreditSignals creditSignals(LoanApplication application) {
        if (application.getCreditHistory() == null || application.getCreditHistory().isEmpty()) {
            return new CreditSignals(0, 0);
        }

        int activeCreditCount = 0;
        int overdueCount = 0;
        for (CreditHistoryEntry entry : application.getCreditHistory()) {
            BigDecimal stillToPay = nonNegative(entry.getAmountStillToPay());
            if (stillToPay.compareTo(BigDecimal.ZERO) > 0) {
                activeCreditCount++;
                if (entry.getDateTillFullRepayment() != null
                        && entry.getDateTillFullRepayment().isBefore(LocalDate.now())) {
                    overdueCount++;
                }
            }
        }
        return new CreditSignals(activeCreditCount, overdueCount);
    }

    private BigDecimal estimateNewInstallment(BigDecimal loanAmount, int termYears) {
        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0 || termYears <= 0) {
            return BigDecimal.ZERO;
        }
        return loanAmount.divide(BigDecimal.valueOf(termYears).multiply(TWELVE), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private BigDecimal clamp(BigDecimal score) {
        if (score.compareTo(TEN) > 0) {
            return TEN;
        }
        if (score.compareTo(ONE) < 0) {
            return ONE;
        }
        return score;
    }

    private boolean looksLikeStablePurpose(String purpose) {
        String normalized = normalizeText(purpose);
        return STABLE_PURPOSE_PATTERNS.stream().anyMatch(normalized::contains);
    }

    private String normalizeText(String text) {
        String withoutDiacritics = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutDiacritics.toLowerCase();
    }

    private String buildExplanation(BigDecimal score, BigDecimal newInstallment,
                                    BigDecimal disposableAfterNewLoan, List<String> notes) {
        List<String> lines = new ArrayList<>();
        lines.add("Estimated new installment: " + newInstallment + " PLN.");
        lines.add("Estimated disposable income after all obligations: " + disposableAfterNewLoan + " PLN.");
        lines.addAll(notes);
        return "- " + String.join("\n- ", lines);
    }

    public record ScoreDetails(BigDecimal score, String explanation) {
    }

    private record CreditSignals(int activeCreditCount, int overdueCount) {
    }
}
