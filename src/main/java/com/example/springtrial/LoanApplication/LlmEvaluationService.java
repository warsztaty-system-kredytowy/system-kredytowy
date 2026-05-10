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
                "Jesteś analitykiem ryzyka kredytowego w banku. Twoim zadaniem jest ocena poniższego wniosku kredytowego.\n");
        sb.append("Przeanalizuj dane i zwróć odpowiedź w formacie JSON zawierającą dokładnie dwa pola:\n");
        sb.append(
                "1. \"score\" (typ numeryczny od 0.0 do 10.0, gdzie 10.0 to wniosek doskonały, a 0.0 to wniosek krytycznie zły).\n");
        sb.append(
                "2. \"explanation\" (typ string, zawierający merytoryczne uzasadnienie w języku polskim, dlaczego przyznano taką punktację. Wymień mocne i słabe strony klienta. UZASADNIENIE MUSI BYĆ KRÓTKIE I ZWIĘZŁE, MAKSYMALNIE 200 SŁÓW).\n\n");

        sb.append("Dane wniosku:\n");
        sb.append("- Wnioskowana kwota: ").append(application.getLoanAmount()).append(" PLN\n");
        sb.append("- Okres kredytowania: ").append(application.getLoanTermYears()).append(" lat\n");
        sb.append("- Cel kredytu: ").append(application.getLoanPurpose()).append("\n");
        sb.append("- Miesięczne dochody: ").append(application.getMonthlyIncome()).append(" PLN\n");
        sb.append("- Miesięczne zobowiązania (stałe koszty): ").append(application.getMonthlyLiabilities())
                .append(" PLN\n");

        sb.append("- Historia kredytowa (obecne obciążenia):\n");
        if (application.getCreditHistory() == null || application.getCreditHistory().isEmpty()) {
            sb.append("  Brak historii kredytowej.\n");
        } else {
            for (CreditHistoryEntry entry : application.getCreditHistory()) {
                sb.append("  * Pożyczono: ").append(entry.getAmountLoaned())
                        .append(" PLN, Pozostało do spłaty: ").append(entry.getAmountStillToPay())
                        .append(" PLN, Miesięczna rata: ").append(entry.getMonthlyCost())
                        .append(" PLN\n");
            }
        }

        sb.append("\nKryteria oceny:\n");
        sb.append(
                "1. Dochód rozporządzalny (dochody minus zobowiązania i raty) musi wystarczyć na spłatę nowej raty kredytu (która wynosi w przybliżeniu kwota / (okres * 12)).\n");
        sb.append(
                "2. Jeśli dochód rozporządzalny jest niższy niż szacowana nowa rata, wniosek należy ocenić nisko (0-3).\n");
        sb.append("3. Dobry cel i pozytywna historia kredytowa mogą podnieść punktację.\n");
        sb.append("\nZWRÓĆ TYLKO PRAWIDŁOWY OBIEKT JSON BEZ ZNACZNIKÓW MARKDOWN.");

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
