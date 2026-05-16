package com.example.springtrial.LoanApplication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LlmEvaluationService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent}")
    private String geminiApiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public LlmEvaluationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public void evaluateApplication(LoanApplication application) {
        if (geminiApiKey == null || geminiApiKey.trim().isEmpty() || "YOUR_API_KEY_HERE".equals(geminiApiKey)) {
            application.setLlmScore(new BigDecimal("0.0"));
            application.setLlmExplanation(
                    "LLM evaluation skipped: No API key provided. Please configure gemini.api.key in application.properties.");
            return;
        }

        try {
            String prompt = buildPrompt(application);

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);

            Map<String, Object> contentPart = new HashMap<>();
            contentPart.put("parts", List.of(textPart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("contents", List.of(contentPart));

            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String urlWithKey = geminiApiUrl + "?key=" + geminiApiKey;
            String responseStr = restTemplate.postForObject(urlWithKey, requestEntity, String.class);

            parseResponse(responseStr, application);

        } catch (Exception e) {
            e.printStackTrace();
            application.setLlmScore(new BigDecimal("0.0"));
            application.setLlmExplanation("Error during LLM evaluation: " + e.getMessage());
        }
    }

    private String buildPrompt(LoanApplication application) {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "You are a credit risk analyst at a bank. Your task is to evaluate the following loan application.\n");
        sb.append("Analyze the data and return a response in JSON format containing exactly two fields:\n");
        sb.append(
                "1. \"score\" (a numeric type from 0.0 to 10.0, where 10.0 is an excellent application and 0.0 is a critically bad application).\n");
        sb.append(
                "2. \"explanation\" (a string type containing a substantive justification in English for why such a score was given. List the applicant's strengths and weaknesses. THE EXPLANATION MUST BE SHORT AND CONCISE, MAXIMUM 200 WORDS).\n\n");

        sb.append("Application Data:\n");
        sb.append("- Requested Amount: ").append(application.getLoanAmount()).append(" PLN\n");
        sb.append("- Loan Term: ").append(application.getLoanTermYears()).append(" years\n");
        sb.append("- Loan Purpose: ").append(application.getLoanPurpose()).append("\n");
        sb.append("- Monthly Income: ").append(application.getMonthlyIncome()).append(" PLN\n");
        sb.append("- Monthly Liabilities (fixed costs): ").append(application.getMonthlyLiabilities())
                .append(" PLN\n");

        sb.append("- Credit History (current liabilities):\n");
        if (application.getCreditHistory() == null || application.getCreditHistory().isEmpty()) {
            sb.append("  No credit history.\n");
        } else {
            for (CreditHistoryEntry entry : application.getCreditHistory()) {
                sb.append("  * Loaned: ").append(entry.getAmountLoaned())
                        .append(" PLN, Remaining to pay: ").append(entry.getAmountStillToPay())
                        .append(" PLN, Monthly installment: ").append(entry.getMonthlyCost())
                        .append(" PLN\n");
            }
        }

        sb.append("\nEvaluation Criteria:\n");
        sb.append(
                "1. Disposable income (income minus liabilities and installments) must be sufficient to pay the new loan installment (which is approximately amount / (term * 12)).\n");
        sb.append(
                "2. If disposable income is lower than the estimated new installment, the application should be scored low (0-3).\n");
        sb.append("3. A good purpose and positive credit history can increase the score.\n");
        sb.append("\nRETURN ONLY A VALID JSON OBJECT WITHOUT MARKDOWN TAGS.");

        return sb.toString();
    }

    private void parseResponse(String responseStr, LoanApplication application) {
        try {
            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).path("content").path("parts");
                if (content.isArray() && !content.isEmpty()) {
                    String text = content.get(0).path("text").asText();
                    JsonNode resultJson = objectMapper.readTree(text);
                    if (resultJson.has("score")) {
                        application.setLlmScore(new BigDecimal(resultJson.get("score").asText()));
                    }
                    if (resultJson.has("explanation")) {
                        application.setLlmExplanation(resultJson.get("explanation").asText());
                    }
                }
            }
        } catch (Exception e) {
            application.setLlmScore(new BigDecimal("0.0"));
            application.setLlmExplanation("Failed to parse LLM response: " + e.getMessage());
        }
    }
}
