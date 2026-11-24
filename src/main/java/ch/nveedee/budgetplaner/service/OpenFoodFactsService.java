package ch.nveedee.budgetplaner.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Produktdaten von Open Food Facts anhand des Barcodes.
 * Open Food Facts verlangt einen aussagekräftigen User-Agent.
 */
@Service
public class OpenFoodFactsService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String OFF_URL =
        "https://world.openfoodfacts.org/api/v2/product/%s.json";
    private static final String USER_AGENT = "Budgetify/1.0 (contact@budgetify.app)";

    public Optional<OffProduct> lookup(String barcode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                String.format(OFF_URL, barcode), HttpMethod.GET, request, Map.class);

            Map body = response.getBody();
            if (body == null) return Optional.empty();

            // status == 1 -> Produkt gefunden
            Object status = body.get("status");
            if (status == null || !"1".equals(String.valueOf(status))) return Optional.empty();

            Object productObj = body.get("product");
            if (!(productObj instanceof Map)) return Optional.empty();
            Map product = (Map) productObj;

            // Name (Fallback: abbreviated_product_name)
            String name = asString(product.get("product_name"));
            if (name == null || name.isBlank()) {
                name = asString(product.get("abbreviated_product_name"));
            }

            // Kategorie: erstes Tag, "en:"-Präfix entfernen
            String kategorie = null;
            Object tags = product.get("categories_tags");
            if (tags instanceof List && !((List<?>) tags).isEmpty()) {
                kategorie = asString(((List<?>) tags).get(0));
                if (kategorie != null && kategorie.startsWith("en:")) {
                    kategorie = kategorie.substring(3);
                }
            }

            String bildUrl = asString(product.get("image_front_url"));

            OffProduct result = new OffProduct();
            result.name = name;
            result.kategorie = kategorie;
            result.bildUrl = bildUrl;
            return Optional.of(result);
        } catch (Exception e) {
            System.err.println("OpenFoodFacts Fehler: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    public static class OffProduct {
        public String name;
        public String kategorie;
        public String bildUrl;
    }
}
