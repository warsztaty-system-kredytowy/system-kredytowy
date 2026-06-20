package com.example.springtrial.unit.loanapplication;

import com.example.springtrial.LoanApplication.LoanApplication;
import com.example.springtrial.LoanApplication.LoanApplicationController;
import com.example.springtrial.LoanApplication.LoanApplicationService;
import com.example.springtrial.LoanApplication.LoanStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer unit tests for {@link LoanApplicationController}.
 *
 * <p>Uses a standalone {@link MockMvc} setup with a mocked service so the tests
 * focus on the controller's HTTP contract: the "you cannot review your own
 * application" guard (403), and the not-found mapping (404). The default
 * {@code ResponseStatusExceptionResolver} registered by the standalone builder
 * translates the thrown {@code ResponseStatusException}s into status codes.</p>
 */
@ExtendWith(MockitoExtension.class)
class LoanApplicationControllerTests {

    @Mock
    private LoanApplicationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LoanApplicationController controller = new LoanApplicationController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void shouldRejectApprovingOwnApplicationWithForbidden() throws Exception {
        when(service.isCurrentUserOwnerOf(1L)).thenReturn(true);

        mockMvc.perform(put("/api/loans/1/approve"))
                .andExpect(status().isForbidden());

        verify(service, never()).approve(anyLong());
    }

    @Test
    void shouldRejectDenyingOwnApplicationWithForbidden() throws Exception {
        when(service.isCurrentUserOwnerOf(1L)).thenReturn(true);

        mockMvc.perform(put("/api/loans/1/deny"))
                .andExpect(status().isForbidden());

        verify(service, never()).deny(anyLong(), any());
    }

    @Test
    void shouldRejectReviewingOwnApplicationWithForbidden() throws Exception {
        when(service.isCurrentUserOwnerOf(1L)).thenReturn(true);

        mockMvc.perform(post("/api/loans/1/review"))
                .andExpect(status().isForbidden());

        verify(service, never()).startReview(anyLong());
    }

    @Test
    void shouldRejectEvaluatingOwnApplicationWithForbidden() throws Exception {
        when(service.isCurrentUserOwnerOf(1L)).thenReturn(true);

        mockMvc.perform(post("/api/loans/1/evaluate"))
                .andExpect(status().isForbidden());

        verify(service, never()).evaluate(anyLong());
    }

    @Test
    void shouldReturnNotFoundWhenApprovingMissingApplication() throws Exception {
        when(service.isCurrentUserOwnerOf(99L)).thenReturn(false);
        when(service.approve(99L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/loans/99/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldApproveApplicationForNonOwnerEmployee() throws Exception {
        LoanApplication approved = new LoanApplication();
        approved.setId(1L);
        approved.setStatus(LoanStatus.APPROVED);
        when(service.isCurrentUserOwnerOf(1L)).thenReturn(false);
        when(service.approve(1L)).thenReturn(Optional.of(approved));

        mockMvc.perform(put("/api/loans/1/approve"))
                .andExpect(status().isOk());

        verify(service).approve(1L);
    }

    @Test
    void shouldReturnNotFoundWhenAppealingMissingApplication() throws Exception {
        // Appeal has no ownership guard, so it goes straight to the service.
        when(service.appeal(eq(5L), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/loans/5/appeal"))
                .andExpect(status().isNotFound());
    }
}
