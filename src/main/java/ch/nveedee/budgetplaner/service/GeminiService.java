package ch.nveedee.budgetplaner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Map;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    public GeminiProduktInfo sucheProduktInfo(String barcode) {
        String prompt = String.format(
            "Suche Informationen zum Produkt mit Barcode %s. " +
            "Antworte NUR im folgenden JSON-Format ohne Markdown: " +
            "{\"name\": \"Produktname\", \"preis\": 1.99, \"kategorie\": \"Lebensmittel\"} " +
            "Preis in CHF. Falls nicht gefunden, gib null zurück.", barcode);

        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of(
                "parts", List.of(Map.of("text", prompt))
            ))
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                GEMINI_URL + apiKey, request, Map.class);

            if (response.getBody() != null) {
                List candidates = (List) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map candidate = (Map) candidates.get(0);
                    Map content = (Map) candidate.get("content");
                    List parts = (List) content.get("parts");
                    Map part = (Map) parts.get(0);
                    String text = (String) part.get("text");

                    // Einfaches Parsing – in Produktion mit Jackson machen
                    return parseGeminiResponse(text);
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini API Fehler: " + e.getMessage());
        }
        return null;
    }

    private GeminiProduktInfo parseGeminiResponse(String json) {
        try {
            // Cleanup falls Markdown-Blöcke dabei
            json = json.replace("```json", "").replace("```", "").trim();

            GeminiProduktInfo info = new GeminiProduktInfo();
            if (json.contains("\"name\"")) {
                info.name = extractJsonString(json, "name");
                info.kategorie = extractJsonString(json, "kategorie");
                String preisStr = extractJsonValue(json, "preis");
                if (preisStr != null) {
                    info.preis = Double.parseDouble(preisStr.trim());
                }
            }
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\": \"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        return end == -1 ? null : json.substring(start, end);
    }

    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\": ";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        return end == -1 ? null : json.substring(start, end).trim();
    }

    public static class GeminiProduktInfo {
        public String name;
        public Double preis;
        public String kategorie;
    }
}
