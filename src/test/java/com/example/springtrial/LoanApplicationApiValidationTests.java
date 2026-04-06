package com.example.springtrial;

import com.example.springtrial.LoanApplication.LoanApplicationController;
import com.example.springtrial.LoanApplication.LoanApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoanApplicationApiValidationTests {

    private MockMvc mockMvc;

    @Mock
    private LoanApplicationService loanApplicationService;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        LoanApplicationController controller = new LoanApplicationController(loanApplicationService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturn400AndNotSaveWhenRequiredFieldIsNull() throws Exception {
        String payload = """
                {
                  "loanAmount": null,
                  "loanTermYears": 10,
                  "monthlyIncome": 12000,
                  "monthlyLiabilities": 2000,
                  "loanPurpose": "Mieszkanie"
                }
                """;

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(loanApplicationService, never()).submit(any());
    }

    @Test
    void shouldReturn400AndNotSaveWhenFrontendSendsEmptyStringForNumber() throws Exception {
        String payload = """
                {
                  "loanAmount": "",
                  "loanTermYears": 10,
                  "monthlyIncome": 12000,
                  "monthlyLiabilities": 2000,
                  "loanPurpose": "Mieszkanie"
                }
                """;

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verify(loanApplicationService, never()).submit(any());
    }
}
