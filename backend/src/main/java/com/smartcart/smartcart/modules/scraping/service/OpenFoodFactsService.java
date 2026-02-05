package com.smartcart.smartcart.modules.scraping.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Servicio para buscar productos en Open Food Facts y obtener EAN
 */
@Slf4j
@Service
public class OpenFoodFactsService {

    private static final String API_V2_URL = "https://world.openfoodfacts.org/api/v2/search";
    private static final String API_SEARCH_URL = "https://world.openfoodfacts.org/cgi/search.pl";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenFoodFactsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Busca un producto por nombre y marca, devuelve el EAN si lo encuentra
     * Usa múltiples estrategias de búsqueda para maximizar resultados
     */
    public Optional<String> findEanByNameAndBrand(String productName, String brand) {
        // Estrategia 1: Buscar por marca en API v2 (más preciso)
        if (brand != null && !brand.isBlank()) {
            Optional<String> ean = searchByBrandV2(productName, brand);
            if (ean.isPresent()) {
                return ean;
            }
        }

        // Estrategia 2: Búsqueda por texto tradicional
        return searchByText(productName, brand);
    }

    /**
     * Búsqueda por marca usando API v2 (mejor para marcas conocidas)
     */
    private Optional<String> searchByBrandV2(String productName, String brand) {
        try {
            String cleanBrand = brand.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

            String cleanName = cleanProductName(productName);

            // Buscar por marca en Open Food Facts
            String url = API_V2_URL + "?brands_tags=" + URLEncoder.encode(cleanBrand, StandardCharsets.UTF_8)
                + "&countries_tags=en:spain"
                + "&page_size=20"
                + "&fields=code,product_name,brands";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartCart/1.0 (https://smartcart.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode products = root.get("products");

                if (products != null && products.isArray()) {
                    // Buscar el mejor match por nombre
                    String bestEan = null;
                    int bestScore = 0;

                    for (JsonNode product : products) {
                        String ean = getTextValue(product, "code");
                        String offName = getTextValue(product, "product_name");

                        if (ean != null && ean.matches("\\d{8,13}") && offName != null) {
                            int score = calculateSimilarity(cleanName, offName.toLowerCase());
                            if (score > bestScore) {
                                bestScore = score;
                                bestEan = ean;
                            }
                        }
                    }

                    if (bestEan != null && bestScore >= 3) {
                        log.debug("EAN encontrado por marca '{}' para '{}': {} (score: {})",
                            brand, productName, bestEan, bestScore);
                        return Optional.of(bestEan);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error en búsqueda por marca para '{}': {}", productName, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Búsqueda tradicional por texto
     */
    private Optional<String> searchByText(String productName, String brand) {
        try {
            String cleanName = cleanProductName(productName);
            String searchTerms = brand != null && !brand.isBlank()
                ? brand + " " + cleanName
                : cleanName;

            String encodedTerms = URLEncoder.encode(searchTerms, StandardCharsets.UTF_8);
            String url = API_SEARCH_URL + "?search_terms=" + encodedTerms
                + "&search_simple=1&action=process&json=1&page_size=10";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SmartCart/1.0 (https://smartcart.com)");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode products = root.get("products");

                if (products != null && products.isArray() && products.size() > 0) {
                    for (JsonNode product : products) {
                        String ean = getTextValue(product, "code");
                        String offName = getTextValue(product, "product_name");
                        String offBrand = getTextValue(product, "brands");

                        if (ean != null && ean.matches("\\d{8,13}")) {
                            // Verificar que coincide la marca o el nombre
                            if ((brand != null && offBrand != null &&
                                 (offBrand.toLowerCase().contains(brand.toLowerCase()) ||
                                  brand.toLowerCase().contains(offBrand.toLowerCase()))) ||
                                (offName != null && isSimilar(cleanName, offName))) {
                                log.debug("EAN encontrado por texto para '{}': {}", productName, ean);
                                return Optional.of(ean);
                            }
                        }
                    }

                    // Fallback: primer resultado con EAN válido
                    for (JsonNode product : products) {
                        String ean = getTextValue(product, "code");
                        if (ean != null && ean.matches("\\d{8,13}")) {
                            log.debug("EAN encontrado (fallback) para '{}': {}", productName, ean);
                            return Optional.of(ean);
                        }
                    }
                }
            }

            log.debug("No se encontró EAN para: {}", productName);
            return Optional.empty();

        } catch (Exception e) {
            log.warn("Error buscando EAN para '{}': {}", productName, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calcula similitud entre dos nombres (número de palabras coincidentes)
     */
    private int calculateSimilarity(String name1, String name2) {
        if (name1 == null || name2 == null) return 0;

        String[] words1 = name1.toLowerCase().split("\\s+");
        String[] words2 = name2.toLowerCase().split("\\s+");

        int matches = 0;
        for (String w1 : words1) {
            if (w1.length() < 3) continue;
            for (String w2 : words2) {
                if (w1.equals(w2) || w1.contains(w2) || w2.contains(w1)) {
                    matches++;
                    break;
                }
            }
        }
        return matches;
    }

    /**
     * Limpia el nombre del producto para mejorar la búsqueda
     */
    private String cleanProductName(String name) {
        if (name == null) return "";

        return name
            // Quitar cantidades y unidades
            .replaceAll("\\d+\\s*(g|kg|ml|l|cl|uds?|unidades?|pack)\\b\\.?", "")
            // Quitar texto entre paréntesis
            .replaceAll("\\([^)]*\\)", "")
            // Quitar "Producto Alcampo" y similares
            .replaceAll("(?i)producto\\s+alcampo", "")
            // Quitar marcas duplicadas al inicio
            .replaceAll("^[A-ZÁÉÍÓÚ]+\\s+", "")
            // Quitar puntuación excesiva
            .replaceAll("[.,;:]+$", "")
            // Normalizar espacios
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Comprueba si dos nombres son similares
     */
    private boolean isSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        String n1 = name1.toLowerCase().trim();
        String n2 = name2.toLowerCase().trim();

        // Comprobar si uno contiene al otro
        return n1.contains(n2) || n2.contains(n1) ||
               // O si comparten palabras clave
               shareKeywords(n1, n2);
    }

    private boolean shareKeywords(String s1, String s2) {
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");

        int matches = 0;
        for (String w1 : words1) {
            if (w1.length() < 3) continue;
            for (String w2 : words2) {
                if (w1.equals(w2)) {
                    matches++;
                    break;
                }
            }
        }

        return matches >= 2;
    }

    private String getTextValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
}
