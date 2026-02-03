package com.smartcart.smartcart.modules.scraping.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.smartcart.modules.scraping.config.ScrapingConfig;
import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scraper para Mercadona usando su API interna JSON.
 *
 * La API de Mercadona requiere un código postal para mostrar precios.
 * Endpoints principales:
 * - GET /api/categories/ - Lista todas las categorías
 * - GET /api/categories/{id}/ - Detalle de categoría con productos
 */
@Slf4j
@Component
public class MercadonaScraper extends BaseScraper {

    private static final String STORE_NAME = "mercadona";
    private static final Long STORE_ID = 1L;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String apiBaseUrl;
    private String postalCode;

    public MercadonaScraper() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        initFromConfig();
        ScrapingConfig.StoreConfig mercadonaConfig = scrapingConfig.getStoreConfig(STORE_NAME);
        if (mercadonaConfig != null) {
            this.apiBaseUrl = mercadonaConfig.getApiUrl();
            this.postalCode = mercadonaConfig.getPostalCode();
        }
        if (this.apiBaseUrl == null) {
            this.apiBaseUrl = "https://tienda.mercadona.es/api";
        }
        if (this.postalCode == null) {
            this.postalCode = "28001"; // Madrid por defecto
        }
        log.info("[{}] Inicializado con API: {} y CP: {}", STORE_NAME, apiBaseUrl, postalCode);
    }

    @Override
    public String getStoreName() {
        return STORE_NAME;
    }

    @Override
    public Long getStoreId() {
        return STORE_ID;
    }

    @Override
    protected List<String> getCategoryUrls() {
        // Para Mercadona obtenemos los IDs dinámicamente de la API
        return new ArrayList<>();
    }

    @Override
    public ScrapingResult scrape() {
        log.info("[{}] Iniciando scraping via API...", STORE_NAME);

        ScrapingResult result = new ScrapingResult();
        result.setStoreName(STORE_NAME);
        result.setStoreId(STORE_ID);
        result.setStartTime(LocalDateTime.now());

        List<ScrapedProduct> allProducts = new ArrayList<>();
        int errors = 0;

        try {
            // Primero obtener lista de categorias
            List<CategoryInfo> categories = fetchCategories();
            log.info("[{}] Encontradas {} categorías principales", STORE_NAME, categories.size());

            for (CategoryInfo category : categories) {
                try {
                    rateLimiter.waitIfNeeded();
                    List<ScrapedProduct> products = fetchCategoryProducts(category.id, category.name);
                    allProducts.addAll(products);

                    log.info("[{}] Categoría '{}' procesada: {} productos",
                             STORE_NAME, category.name, products.size());

                } catch (Exception e) {
                    errors++;
                    log.error("[{}] Error en categoría {}: {}",
                              STORE_NAME, category.name, e.getMessage());
                    result.addError("category-" + category.id, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[{}] Error obteniendo categorías: {}", STORE_NAME, e.getMessage());
            errors++;
            result.addError("categories", e.getMessage());
        }

        result.setProducts(allProducts);
        result.setEndTime(LocalDateTime.now());
        result.setTotalProducts(allProducts.size());
        result.setTotalErrors(errors);

        log.info("[{}] Scraping completado: {} productos, {} errores en {}s",
                 STORE_NAME, allProducts.size(), errors, result.getDurationSeconds());

        return result;
    }

    /**
     * Obtiene las subcategorías (segundo nivel) de todas las categorías principales.
     * La API de Mercadona tiene estructura:
     * Categoría principal -> Subcategorías -> Sub-subcategorías con productos
     */
    private List<CategoryInfo> fetchCategories() {
        String url = apiBaseUrl + "/categories/";

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            List<CategoryInfo> subcategories = new ArrayList<>();

            JsonNode results = root.has("results") ? root.get("results") : root;

            if (results.isArray()) {
                for (JsonNode mainCategory : results) {
                    // Extraer subcategorías de cada categoría principal
                    if (mainCategory.has("categories") && mainCategory.get("categories").isArray()) {
                        for (JsonNode subCategory : mainCategory.get("categories")) {
                            String id = subCategory.has("id") ? subCategory.get("id").asText() : null;
                            String name = subCategory.has("name") ? subCategory.get("name").asText() : "Unknown";
                            if (id != null) {
                                subcategories.add(new CategoryInfo(id, name));
                            }
                        }
                    }
                }
            }

            log.info("[{}] Encontradas {} subcategorías", STORE_NAME, subcategories.size());
            return subcategories;

        } catch (Exception e) {
            log.error("[{}] Error fetching categories: {}", STORE_NAME, e.getMessage());
            return getKnownCategories();
        }
    }

    /**
     * Subcategorías conocidas como fallback.
     */
    private List<CategoryInfo> getKnownCategories() {
        return List.of(
            new CategoryInfo("112", "Aceite, vinagre y sal"),
            new CategoryInfo("115", "Especias"),
            new CategoryInfo("156", "Agua"),
            new CategoryInfo("135", "Aceitunas y encurtidos"),
            new CategoryInfo("118", "Arroz"),
            new CategoryInfo("120", "Pasta y fideos")
        );
    }

    /**
     * Obtiene todos los productos de una subcategoría.
     * Las subcategorías contienen sub-subcategorías con productos.
     */
    private List<ScrapedProduct> fetchCategoryProducts(String categoryId, String categoryName) {
        String url = apiBaseUrl + "/categories/" + categoryId + "/";

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<ScrapedProduct> products = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());

            // Las subcategorías tienen sub-subcategorías con productos
            if (root.has("categories") && root.get("categories").isArray()) {
                for (JsonNode subSubCategory : root.get("categories")) {
                    String subCatName = subSubCategory.has("name") ?
                        subSubCategory.get("name").asText() : categoryName;

                    if (subSubCategory.has("products") && subSubCategory.get("products").isArray()) {
                        for (JsonNode productNode : subSubCategory.get("products")) {
                            ScrapedProduct product = parseProductFromJson(productNode, subCatName, categoryId);
                            if (product != null) {
                                products.add(product);
                            }
                        }
                    }
                }
            }

            // También puede haber productos directos
            if (root.has("products") && root.get("products").isArray()) {
                for (JsonNode productNode : root.get("products")) {
                    ScrapedProduct product = parseProductFromJson(productNode, categoryName, categoryId);
                    if (product != null) {
                        products.add(product);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[{}] Error fetching products for category {}: {}", STORE_NAME, categoryId, e.getMessage());
        }

        return products;
    }

    /**
     * Parsea un producto desde el JSON de la API.
     * Estructura real:
     * {
     *   "id": "4241",
     *   "display_name": "Aceite de oliva 0,4º Hacendado",
     *   "thumbnail": "https://...",
     *   "share_url": "https://tienda.mercadona.es/product/4241/...",
     *   "packaging": "Garrafa",
     *   "price_instructions": {
     *     "unit_price": "19.75",
     *     "bulk_price": "3.95",
     *     "unit_size": 5.0,
     *     "size_format": "l",
     *     "reference_price": "3.950",
     *     "reference_format": "L",
     *     "previous_unit_price": null
     *   }
     * }
     */
    private ScrapedProduct parseProductFromJson(JsonNode node, String categoryName, String categoryId) {
        try {
            String id = getTextValue(node, "id");
            String name = getTextValue(node, "display_name");

            if (id == null || name == null) {
                return null;
            }

            // Extraer información de precio
            BigDecimal price = null;
            BigDecimal originalPrice = null;
            String pricePerUnit = null;
            String unit = null;
            boolean onSale = false;

            if (node.has("price_instructions")) {
                JsonNode priceNode = node.get("price_instructions");

                // Precio unitario (el que paga el cliente)
                price = getBigDecimalValue(priceNode, "unit_price");
                if (price == null) {
                    price = getBigDecimalValue(priceNode, "bulk_price");
                }

                // Precio anterior si hay descuento
                originalPrice = getBigDecimalValue(priceNode, "previous_unit_price");
                if (originalPrice != null && price != null && originalPrice.compareTo(price) > 0) {
                    onSale = true;
                }

                // Verificar también price_decreased flag
                if (priceNode.has("price_decreased") && priceNode.get("price_decreased").asBoolean()) {
                    onSale = true;
                }

                // Precio por unidad de referencia (ej: 3.95€/L)
                BigDecimal refPrice = getBigDecimalValue(priceNode, "reference_price");
                String refFormat = getTextValue(priceNode, "reference_format");
                if (refPrice != null && refFormat != null) {
                    pricePerUnit = refPrice.toPlainString() + "€/" + refFormat;
                }

                // Tamaño del producto (ej: "5.0 l")
                Double unitSize = priceNode.has("unit_size") && !priceNode.get("unit_size").isNull() ?
                    priceNode.get("unit_size").asDouble() : null;
                String sizeFormat = getTextValue(priceNode, "size_format");
                if (unitSize != null && sizeFormat != null) {
                    unit = unitSize + " " + sizeFormat;
                }
            }

            // Imagen
            String imageUrl = getTextValue(node, "thumbnail");

            // URL del producto
            String productUrl = getTextValue(node, "share_url");
            if (productUrl == null) {
                productUrl = "https://tienda.mercadona.es/product/" + id;
            }

            // Packaging como descripcion adicional
            String packaging = getTextValue(node, "packaging");
            String description = packaging;

            // Extraer marca del nombre (normalmente es "Producto Marca")
            String brand = extractBrandFromName(name);

            return ScrapedProduct.builder()
                .externalId(id)
                .name(name)
                .brand(brand)
                .description(description)
                .price(price)
                .originalPrice(originalPrice)
                .onSale(onSale)
                .pricePerUnit(pricePerUnit)
                .unit(unit)
                .imageUrl(imageUrl)
                .productUrl(productUrl)
                .categoryName(categoryName)
                .categoryId(categoryId)
                .build();

        } catch (Exception e) {
            log.warn("[{}] Error parsing product JSON: {}", STORE_NAME, e.getMessage());
            return null;
        }
    }

    /**
     * Intenta extraer la marca del nombre del producto.
     * Ejemplo: "Aceite de oliva 0,4º Hacendado" -> "Hacendado"
     */
    private String extractBrandFromName(String name) {
        if (name == null) return null;

        // Marcas conocidas de Mercadona
        String[] knownBrands = {"Hacendado", "Deliplus", "Bosque Verde", "Compy", "Cecotec"};
        for (String brand : knownBrands) {
            if (name.toLowerCase().contains(brand.toLowerCase())) {
                return brand;
            }
        }
        return null;
    }

    /**
     * Crea los headers HTTP necesarios para la API de Mercadona.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", userAgentRotator.getNext());
        headers.set("Accept", "application/json");
        headers.set("Accept-Language", "es-ES,es;q=0.9");
        headers.set("Origin", "https://tienda.mercadona.es");
        headers.set("Referer", "https://tienda.mercadona.es/");
        // Cookie con el código postal para obtener precios correctos
        headers.set("Cookie", "postal_code=" + postalCode);
        return headers;
    }

    /**
     * Extrae un valor de texto de un nodo JSON.
     */
    private String getTextValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    /**
     * Extrae un valor BigDecimal de un nodo JSON.
     */
    private BigDecimal getBigDecimalValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            try {
                return new BigDecimal(node.get(field).asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ========== METODOS PUBLICOS PARA ACCESO DIRECTO ==========

    /**
     * Obtiene productos de UNA categoria especifica (rapido).
     * Llama directamente a /api/categories/{id}/
     */
    public List<ScrapedProduct> scrapeCategory(String categoryId) {
        log.info("[{}] Obteniendo productos de categoria {}", STORE_NAME, categoryId);
        return fetchCategoryProducts(categoryId, "Categoria " + categoryId);
    }

    /**
     * Obtiene lista de todas las categorias con sus IDs.
     */
    public List<PublicCategoryInfo> fetchAllCategories() {
        String url = apiBaseUrl + "/categories/";

        HttpHeaders headers = createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        List<PublicCategoryInfo> allCategories = new ArrayList<>();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode results = root.has("results") ? root.get("results") : root;

            if (results.isArray()) {
                for (JsonNode mainCategory : results) {
                    // Categoria principal
                    String mainId = mainCategory.has("id") ? mainCategory.get("id").asText() : null;
                    String mainName = mainCategory.has("name") ? mainCategory.get("name").asText() : "Unknown";

                    // Subcategorias
                    if (mainCategory.has("categories") && mainCategory.get("categories").isArray()) {
                        for (JsonNode subCategory : mainCategory.get("categories")) {
                            String subId = subCategory.has("id") ? subCategory.get("id").asText() : null;
                            String subName = subCategory.has("name") ? subCategory.get("name").asText() : "Unknown";
                            if (subId != null) {
                                allCategories.add(new PublicCategoryInfo(subId, subName, mainName));
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("[{}] Error fetching categories: {}", STORE_NAME, e.getMessage());
        }

        return allCategories;
    }

    /**
     * Clase interna para almacenar información de categoría.
     */
    private record CategoryInfo(String id, String name) {}

    /**
     * Record publico para devolver informacion de categorias.
     */
    public record PublicCategoryInfo(String id, String name, String parentCategory) {}
}
