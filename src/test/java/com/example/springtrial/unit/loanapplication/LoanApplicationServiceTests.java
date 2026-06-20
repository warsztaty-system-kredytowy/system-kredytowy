package com.example.springtrial.unit.loanapplication;

import com.example.springtrial.LoanApplication.LlmEvaluationService;
import com.example.springtrial.LoanApplication.LoanApplication;
import com.example.springtrial.LoanApplication.LoanApplicationRepository;
import com.example.springtrial.LoanApplication.LoanApplicationRequest;
import com.example.springtrial.LoanApplication.LoanApplicationService;
import com.example.springtrial.LoanApplication.LoanScoringService;
import com.example.springtrial.LoanApplication.LoanStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoanApplicationService}.
 *
 * <p>The service is exercised in isolation: the repository and downstream
 * evaluation services are mocked so the tests only assert the service's own
 * orchestration logic (status transitions, ownership checks and history
 * recording). The {@link SecurityContextHolder} is reset around every test to
 * keep the static security state deterministic.</p>
 */
@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTests {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private LlmEvaluationService llmEvaluationService;

    @Mock
    private LoanScoringService loanScoringService;

    private LoanApplicationService service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        service = new LoanApplicationService(repository, llmEvaluationService, loanScoringService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Returns the saved entity so {@code repository.save(...)} behaves like the real one. */
    private void stubSaveToReturnArgument() {
        when(repository.save(any(LoanApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private LoanApplication applicationWithStatus(Long id, LoanStatus status) {
        LoanApplication application = new LoanApplication();
        application.setId(id);
        application.setStatus(status);
        return application;
    }

    @Nested
    class Submit {

        @Test
        void shouldCopyRequestFields_scoreAndPersistWithInitialHistory() {
            stubSaveToReturnArgument();
            LoanApplicationRequest request = new LoanApplicationRequest();
            request.setLoanAmount(new BigDecimal("120000"));
            request.setLoanTermYears(10);
            request.setMonthlyIncome(new BigDecimal("16000"));
            request.setMonthlyLiabilities(new BigDecimal("1500"));
            request.setLoanPurpose("Apartment renovation");

            LoanApplication saved = service.submit(request);

            // Request data is mapped onto the persisted entity.
            assertThat(saved.getLoanAmount()).isEqualByComparingTo("120000");
            assertThat(saved.getLoanTermYears()).isEqualTo(10);
            assertThat(saved.getMonthlyIncome()).isEqualByComparingTo("16000");
            assertThat(saved.getLoanPurpose()).isEqualTo("Apartment renovation");
            assertThat(saved.getSubmittedAt()).isNotNull();
            assertThat(saved.getOwner()).isNotBlank();

            // A fresh application starts in NEW with exactly one history entry.
            assertThat(saved.getStatus()).isEqualTo(LoanStatus.NEW);
            assertThat(saved.getStatusHistory()).hasSize(1);
            assertThat(saved.getStatusHistory().get(0).getStatus()).isEqualTo(LoanStatus.NEW);
            assertThat(saved.getStatusHistory().get(0).getNote()).isEqualTo("Application submitted");

            verify(loanScoringService).score(saved);
            verify(repository).save(saved);
        }
    }

    @Nested
    class Approve {

        @Test
        void shouldTransitionToApprovedAndAppendHistory() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.NEW)));

            LoanApplication result = service.approve(1L).orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(result.isFinalDecision()).isFalse();
            assertThat(result.getStatusHistory())
                    .extracting(h -> h.getStatus())
                    .containsExactly(LoanStatus.APPROVED);
            verify(repository).save(result);
        }

        @Test
        void shouldMarkDecisionAsFinalWhenApprovingAnAppeal() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.APPEALED)));

            LoanApplication result = service.approve(1L).orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(result.isFinalDecision()).isTrue();
        }

        @Test
        void shouldReturnEmptyAndNotPersistWhenApplicationMissing() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            Optional<LoanApplication> result = service.approve(99L);

            assertThat(result).isEmpty();
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class Deny {

        @Test
        void shouldStoreReasonAndTransitionToDenied() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.IN_PROCESSING)));

            LoanApplication result = service.deny(1L, "Insufficient income").orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.DENIED);
            assertThat(result.getDenialReason()).isEqualTo("Insufficient income");
            assertThat(result.getStatusHistory().get(0).getNote())
                    .contains("Insufficient income");
        }

        @Test
        void shouldRecordNoneWhenReasonIsNull() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.NEW)));

            LoanApplication result = service.deny(1L, null).orElseThrow();

            assertThat(result.getStatusHistory().get(0).getNote()).contains("None");
        }

        @Test
        void shouldMarkDecisionAsFinalWhenDenyingAnAppeal() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.APPEALED)));

            LoanApplication result = service.deny(1L, "Final rejection").orElseThrow();

            assertThat(result.isFinalDecision()).isTrue();
            assertThat(result.getStatus()).isEqualTo(LoanStatus.DENIED);
        }
    }

    @Nested
    class StartReview {

        @Test
        void shouldRescoreAndMoveNewApplicationIntoProcessing() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.NEW)));

            LoanApplication result = service.startReview(1L).orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.IN_PROCESSING);
            verify(loanScoringService).score(result);
            verify(repository).save(result);
        }

        @Test
        void shouldRescoreButNotTransitionWhenAlreadyBeyondNew() {
            stubSaveToReturnArgument();
            when(repository.findById(1L)).thenReturn(Optional.of(applicationWithStatus(1L, LoanStatus.APPROVED)));

            LoanApplication result = service.startReview(1L).orElseThrow();

            // Already-decided applications are re-scored but their status is untouched.
            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPROVED);
            assertThat(result.getStatusHistory()).isNull();
            verify(loanScoringService).score(result);
        }
    }

    @Nested
    class Evaluate {

        @Test
        void shouldRunRuleBasedScoringThenLlmEvaluationThenPersist() {
            stubSaveToReturnArgument();
            LoanApplication application = applicationWithStatus(1L, LoanStatus.IN_PROCESSING);
            when(repository.findById(1L)).thenReturn(Optional.of(application));

            LoanApplication result = service.evaluate(1L).orElseThrow();

            assertThat(result).isSameAs(application);
            verify(loanScoringService).score(application);
            verify(llmEvaluationService).evaluateApplication(application);
            verify(repository).save(application);
        }

        @Test
        void shouldReturnEmptyWhenApplicationMissing() {
            when(repository.findById(7L)).thenReturn(Optional.empty());

            assertThat(service.evaluate(7L)).isEmpty();
            verify(loanScoringService, never()).score(any());
            verify(llmEvaluationService, never()).evaluateApplication(any());
        }
    }

    @Nested
    class Appeal {

        @Test
        void shouldTransitionDeniedNonFinalApplicationToAppealed() {
            stubSaveToReturnArgument();
            LoanApplication application = applicationWithStatus(1L, LoanStatus.DENIED);
            application.setFinalDecision(false);
            when(repository.findByIdAndOwner(eq(1L), anyString())).thenReturn(Optional.of(application));

            LoanApplication result = service.appeal(1L, "Please reconsider").orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.APPEALED);
            assertThat(result.getStatusHistory().get(0).getNote()).contains("Please reconsider");
        }

        @Test
        void shouldNotTransitionWhenDecisionIsAlreadyFinal() {
            stubSaveToReturnArgument();
            LoanApplication application = applicationWithStatus(1L, LoanStatus.DENIED);
            application.setFinalDecision(true);
            when(repository.findByIdAndOwner(eq(1L), anyString())).thenReturn(Optional.of(application));

            LoanApplication result = service.appeal(1L, "Reopen").orElseThrow();

            // Final decisions cannot be appealed: status stays DENIED, no history added.
            assertThat(result.getStatus()).isEqualTo(LoanStatus.DENIED);
            assertThat(result.getStatusHistory()).isNull();
        }

        @Test
        void shouldNotTransitionWhenApplicationIsNotDenied() {
            stubSaveToReturnArgument();
            LoanApplication application = applicationWithStatus(1L, LoanStatus.NEW);
            when(repository.findByIdAndOwner(eq(1L), anyString())).thenReturn(Optional.of(application));

            LoanApplication result = service.appeal(1L, "Too early").orElseThrow();

            assertThat(result.getStatus()).isEqualTo(LoanStatus.NEW);
            assertThat(result.getStatusHistory()).isNull();
        }
    }

    @Nested
    class OwnershipAndQueries {

        @Test
        void isCurrentUserOwnerOf_shouldBeFalseWhenOwnerDiffers() {
            LoanApplication application = applicationWithStatus(1L, LoanStatus.NEW);
            application.setOwner("someone-else@example.com");
            when(repository.findById(1L)).thenReturn(Optional.of(application));

            assertThat(service.isCurrentUserOwnerOf(1L)).isFalse();
        }

        @Test
        void isCurrentUserOwnerOf_shouldBeFalseWhenApplicationMissing() {
            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThat(service.isCurrentUserOwnerOf(1L)).isFalse();
        }

        @Test
        void isCurrentUserEmployee_shouldBeFalseForUnauthenticatedContext() {
            assertThat(service.isCurrentUserEmployee()).isFalse();
        }

        @Test
        void findAllForCurrentUser_shouldQueryRepositoryByOwnerKey() {
            ArgumentCaptor<String> ownerCaptor = ArgumentCaptor.forClass(String.class);
            when(repository.findByOwner(anyString())).thenReturn(List.of());

            service.findAllForCurrentUser();

            verify(repository).findByOwner(ownerCaptor.capture());
            assertThat(ownerCaptor.getValue()).isNotBlank();
        }

        @Test
        void findById_shouldDelegateToRepository() {
            LoanApplication application = applicationWithStatus(5L, LoanStatus.NEW);
            when(repository.findById(5L)).thenReturn(Optional.of(application));

            assertThat(service.findById(5L)).containsSame(application);
            verify(repository, times(1)).findById(5L);
        }
    }
}
