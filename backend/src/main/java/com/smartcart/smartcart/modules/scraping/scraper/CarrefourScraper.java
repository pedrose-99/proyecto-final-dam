package com.smartcart.smartcart.modules.scraping.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.smartcart.modules.scraping.config.ScrapingConfig;
import com.smartcart.smartcart.modules.scraping.dto.ScrapedProduct;
import com.smartcart.smartcart.modules.scraping.dto.ScrapingResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.smartcart.smartcart.modules.product.entity.ProductStore;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class CarrefourScraper extends BaseScraper
{

    private static final String STORE_NAME = "carrefour";
    private static final Long STORE_ID = 3L;
    private static final String BASE_URL = "https://www.carrefour.es";

    private String baseUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CarrefourScraper()
    {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init()
    {
        initFromConfig();
        ScrapingConfig.StoreConfig config = scrapingConfig.getStoreConfig(STORE_NAME);
        if (config != null && config.getBaseUrl() != null)
        {
            this.baseUrl = config.getBaseUrl();
        }
        else
        {
            this.baseUrl = BASE_URL;
        }
        log.info("[{}] Inicializado con URL base: {}", STORE_NAME, baseUrl);
    }

    @Override
    public String getStoreName()
    {
        return STORE_NAME;
    }

    @Override
    public Long getStoreId()
    {
        return STORE_ID;
    }

    @Override
    protected List<String> getCategoryUrls()
    {
        return List.of(
            "/supermercado/lacteos-y-derivados/cat20003/c",
            "/supermercado/carnes/cat20004/c",
            "/supermercado/pescados-y-mariscos/cat20005/c",
            "/supermercado/frutas-y-verduras/cat20002/c",
            "/supermercado/bebidas/cat20008/c",
            "/supermercado/despensa/cat20007/c",
            "/supermercado/panaderia-y-pasteleria/cat20006/c",
            "/supermercado/congelados/cat20009/c",
            "/supermercado/charcuteria-y-quesos/cat20001/c",
            "/supermercado/alimentacion-infantil/cat20010/c"
        );
    }

    @Override
    public ScrapingResult scrape()
    {
        log.info("[{}] Iniciando scraping HTML (paralelo)...", STORE_NAME);

        ScrapingResult result = new ScrapingResult();
        result.setStoreName(STORE_NAME);
        result.setStoreId(STORE_ID);
        result.setStartTime(LocalDateTime.now());

        CopyOnWriteArrayList<ScrapedProduct> allProducts = new CopyOnWriteArrayList<>();
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger processedCategories = new AtomicInteger(0);

        try
        {
            List<String> categoryPaths = getCategoryUrls();
            log.info("[{}] Procesando {} categorias en paralelo...", STORE_NAME, categoryPaths.size());

            int parallelism = 5;
            ExecutorService executor = Executors.newFixedThreadPool(parallelism);

            List<CompletableFuture<Void>> futures = categoryPaths.stream()
                .map(categoryPath -> CompletableFuture.runAsync(() -> {
                    try
                    {
                        rateLimiter.waitIfNeeded();
                        String categoryId = extractCategoryId(categoryPath);
                        String categoryName = extractCategoryName(categoryPath);
                        String url = baseUrl + categoryPath;

                        List<ScrapedProduct> products = scrapeCategoryPages(url, categoryName, categoryId);
                        allProducts.addAll(products);

                        int count = processedCategories.incrementAndGet();
                        log.info("[{}] Categoria '{}' procesada: {} productos ({}/{})",
                                 STORE_NAME, categoryName, products.size(), count, categoryPaths.size());
                    }
                    catch (Exception e)
                    {
                        errors.incrementAndGet();
                        log.error("[{}] Error en categoria {}: {}",
                                  STORE_NAME, categoryPath, e.getMessage());
                        result.addError("category-" + categoryPath, e.getMessage());
                    }
                }, executor))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
        }
        catch (Exception e)
        {
            log.error("[{}] Error general: {}", STORE_NAME, e.getMessage());
            errors.incrementAndGet();
            result.addError("general", e.getMessage());
        }

        result.setProducts(new ArrayList<>(allProducts));
        result.setEndTime(LocalDateTime.now());
        result.setTotalProducts(allProducts.size());
        result.setTotalErrors(errors.get());

        log.info("[{}] Scraping completado: {} productos, {} errores en {}s",
                 STORE_NAME, allProducts.size(), errors.get(), result.getDurationSeconds());

        return result;
    }

    private List<ScrapedProduct> scrapeCategoryPages(String categoryUrl, String categoryName, String categoryId)
    {
        List<ScrapedProduct> products = new ArrayList<>();
        Set<String> seenProductIds = new HashSet<>();
        int page = 1;
        int maxPages = 10;
        int emptyPages = 0;

        while (page <= maxPages && emptyPages < 2)
        {
            try
            {
                String url = page == 1 ? categoryUrl : categoryUrl + "?page=" + page;
                log.debug("[{}] Fetching: {}", STORE_NAME, url);

                Document doc = fetchPage(url);
                String html = doc.html();

                List<ScrapedProduct> pageProducts = extractProducts(html, doc, categoryName, categoryId, seenProductIds);

                if (pageProducts.isEmpty())
                {
                    emptyPages++;
                }
                else
                {
                    products.addAll(pageProducts);
                    emptyPages = 0;
                }

                page++;

                if (page <= maxPages)
                {
                    rateLimiter.waitIfNeeded();
                }
            }
            catch (Exception e)
            {
                log.warn("[{}] Error en pagina {} de {}: {}", STORE_NAME, page, categoryUrl, e.getMessage());
                emptyPages++;
                page++;
            }
        }

        return products;
    }

    private Document fetchPage(String url)
    {
        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
            headers.set("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");
            headers.set("Cache-Control", "max-age=0");
            headers.set("Connection", "keep-alive");
            headers.set("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"");
            headers.set("Sec-Ch-Ua-Mobile", "?0");
            headers.set("Sec-Ch-Ua-Platform", "\"Linux\"");
            headers.set("Sec-Fetch-Dest", "document");
            headers.set("Sec-Fetch-Mode", "navigate");
            headers.set("Sec-Fetch-Site", "none");
            headers.set("Sec-Fetch-User", "?1");
            headers.set("Upgrade-Insecure-Requests", "1");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            String html = response.getBody();
            if (html == null)
            {
                html = "";
            }

            return Jsoup.parse(html, url);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error fetching page: " + url, e);
        }
    }

    /**
     * Intenta extraer productos usando multiples estrategias.
     * 1. JSON-LD (schema.org) - mas fiable y estandar
     * 2. JSON embebido en script tags
     * 3. Selectores CSS como fallback
     */
    private List<ScrapedProduct> extractProducts(String html, Document doc, String categoryName,
                                                  String categoryId, Set<String> seenIds)
    {
        // Estrategia 1: JSON-LD
        List<ScrapedProduct> products = extractFromJsonLd(doc, categoryName, categoryId, seenIds);
        if (!products.isEmpty())
        {
            log.debug("[{}] Extraidos {} productos via JSON-LD", STORE_NAME, products.size());
            return products;
        }

        // Estrategia 2: JSON embebido en scripts
        products = extractFromEmbeddedJson(html, categoryName, categoryId, seenIds);
        if (!products.isEmpty())
        {
            log.debug("[{}] Extraidos {} productos via JSON embebido", STORE_NAME, products.size());
            return products;
        }

        // Estrategia 3: Selectores CSS
        products = extractFromCssSelectors(doc, categoryName, categoryId, seenIds);
        if (!products.isEmpty())
        {
            log.debug("[{}] Extraidos {} productos via CSS selectors", STORE_NAME, products.size());
        }

        return products;
    }

    /**
     * Extrae productos de JSON-LD (schema.org) embebido en la pagina.
     * Carrefour suele incluir datos estructurados para SEO.
     */
    private List<ScrapedProduct> extractFromJsonLd(Document doc, String categoryName,
                                                    String categoryId, Set<String> seenIds)
    {
        List<ScrapedProduct> products = new ArrayList<>();

        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts)
        {
            try
            {
                String json = script.html();
                JsonNode root = objectMapper.readTree(json);

                // Puede ser un ItemList con productos
                if (root.has("@type") && "ItemList".equals(root.get("@type").asText()))
                {
                    JsonNode items = root.get("itemListElement");
                    if (items != null && items.isArray())
                    {
                        for (JsonNode item : items)
                        {
                            JsonNode productNode = item.has("item") ? item.get("item") : item;
                            ScrapedProduct product = parseJsonLdProduct(productNode, categoryName, categoryId);
                            if (product != null && !seenIds.contains(product.externalId()))
                            {
                                seenIds.add(product.externalId());
                                products.add(product);
                            }
                        }
                    }
                }
                // O puede ser un producto individual
                else if (root.has("@type") && "Product".equals(root.get("@type").asText()))
                {
                    ScrapedProduct product = parseJsonLdProduct(root, categoryName, categoryId);
                    if (product != null && !seenIds.contains(product.externalId()))
                    {
                        seenIds.add(product.externalId());
                        products.add(product);
                    }
                }
            }
            catch (Exception e)
            {
                log.debug("[{}] Error parseando JSON-LD: {}", STORE_NAME, e.getMessage());
            }
        }

        return products;
    }

    private ScrapedProduct parseJsonLdProduct(JsonNode node, String categoryName, String categoryId)
    {
        try
        {
            String name = getJsonText(node, "name");
            if (name == null || name.isBlank())
            {
                return null;
            }

            String sku = getJsonText(node, "sku");
            String ean = getJsonText(node, "gtin13");
            if (ean == null)
            {
                ean = getJsonText(node, "gtin");
            }

            BigDecimal price = null;
            BigDecimal originalPrice = null;
            boolean onSale = false;

            if (node.has("offers"))
            {
                JsonNode offers = node.get("offers");
                if (offers.isArray() && offers.size() > 0)
                {
                    offers = offers.get(0);
                }
                price = getJsonDecimal(offers, "price");
                originalPrice = getJsonDecimal(offers, "highPrice");
                if (originalPrice != null && price != null && originalPrice.compareTo(price) > 0)
                {
                    onSale = true;
                }
            }

            String imageUrl = getJsonText(node, "image");
            String productUrl = getJsonText(node, "url");
            String brand = null;
            if (node.has("brand"))
            {
                JsonNode brandNode = node.get("brand");
                brand = brandNode.isObject() ? getJsonText(brandNode, "name") : brandNode.asText();
            }

            if (brand == null)
            {
                brand = extractBrandFromName(name);
            }

            String externalId = sku != null ? sku : String.valueOf(name.hashCode());

            return ScrapedProduct.builder()
                .externalId(externalId)
                .ean(ean)
                .name(name)
                .brand(brand)
                .description(getJsonText(node, "description"))
                .price(price)
                .originalPrice(originalPrice)
                .onSale(onSale)
                .pricePerUnit(null)
                .unit(null)
                .imageUrl(imageUrl)
                .productUrl(productUrl != null && productUrl.startsWith("http") ? productUrl : baseUrl + productUrl)
                .categoryName(categoryName)
                .categoryId(categoryId)
                .origin(null)
                .build();
        }
        catch (Exception e)
        {
            log.debug("[{}] Error parseando producto JSON-LD: {}", STORE_NAME, e.getMessage());
            return null;
        }
    }

    /**
     * Extrae productos de JSON embebido en tags script.
     * Busca patrones comunes: __NEXT_DATA__, __INITIAL_STATE__, productData, etc.
     */
    private List<ScrapedProduct> extractFromEmbeddedJson(String html, String categoryName,
                                                          String categoryId, Set<String> seenIds)
    {
        List<ScrapedProduct> products = new ArrayList<>();

        // Patron 1: __NEXT_DATA__ (Next.js)
        Pattern nextDataPattern = Pattern.compile(
            "<script\\s+id=\"__NEXT_DATA__\"[^>]*>([\\s\\S]*?)</script>"
        );
        Matcher nextMatcher = nextDataPattern.matcher(html);
        if (nextMatcher.find())
        {
            products = parseNextData(nextMatcher.group(1), categoryName, categoryId, seenIds);
            if (!products.isEmpty())
            {
                return products;
            }
        }

        // Patron 2: Buscar objetos JSON con datos de producto
        // Formato: "name":"Producto","price":{"current":{"amount":"1.99"}}
        Pattern productPattern = Pattern.compile(
            "\"name\":\"([^\"]{5,100})\",\"price\":\\{\"current\":\\{\"amount\":\"([\\d.]+)\""
        );
        Matcher productMatcher = productPattern.matcher(html);

        while (productMatcher.find())
        {
            try
            {
                String name = productMatcher.group(1);
                String priceStr = productMatcher.group(2);
                String externalId = String.valueOf(name.hashCode());

                if (seenIds.contains(externalId))
                {
                    continue;
                }

                BigDecimal price = new BigDecimal(priceStr);
                if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(new BigDecimal("10000")) > 0)
                {
                    continue;
                }

                seenIds.add(externalId);

                ScrapedProduct product = ScrapedProduct.builder()
                    .externalId(externalId)
                    .ean(null)
                    .name(decodeUnicode(name))
                    .brand(extractBrandFromName(name))
                    .description(null)
                    .price(price)
                    .originalPrice(null)
                    .onSale(false)
                    .pricePerUnit(null)
                    .unit(null)
                    .imageUrl(null)
                    .productUrl(null)
                    .categoryName(categoryName)
                    .categoryId(categoryId)
                    .origin(null)
                    .build();

                products.add(product);
            }
            catch (Exception e)
            {
                log.debug("[{}] Error parseando producto embebido: {}", STORE_NAME, e.getMessage());
            }
        }

        return products;
    }

    private List<ScrapedProduct> parseNextData(String json, String categoryName,
                                                String categoryId, Set<String> seenIds)
    {
        List<ScrapedProduct> products = new ArrayList<>();

        try
        {
            JsonNode root = objectMapper.readTree(json);
            JsonNode props = root.path("props").path("pageProps");

            // Buscar arrays de productos en diferentes posibles ubicaciones
            JsonNode productNodes = findProductArray(props);
            if (productNodes != null && productNodes.isArray())
            {
                for (JsonNode node : productNodes)
                {
                    ScrapedProduct product = parseEmbeddedProduct(node, categoryName, categoryId);
                    if (product != null && !seenIds.contains(product.externalId()))
                    {
                        seenIds.add(product.externalId());
                        products.add(product);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.debug("[{}] Error parseando __NEXT_DATA__: {}", STORE_NAME, e.getMessage());
        }

        return products;
    }

    private JsonNode findProductArray(JsonNode node)
    {
        if (node == null || node.isMissingNode())
        {
            return null;
        }

        // Buscar campos comunes que contienen arrays de productos
        String[] productFields = {"products", "items", "results", "productList", "content"};
        for (String field : productFields)
        {
            if (node.has(field) && node.get(field).isArray())
            {
                return node.get(field);
            }
        }

        // Buscar recursivamente en primer nivel
        var fields = node.fields();
        while (fields.hasNext())
        {
            var entry = fields.next();
            if (entry.getValue().isObject())
            {
                JsonNode found = findProductArray(entry.getValue());
                if (found != null)
                {
                    return found;
                }
            }
        }

        return null;
    }

    private ScrapedProduct parseEmbeddedProduct(JsonNode node, String categoryName, String categoryId)
    {
        try
        {
            String name = getJsonText(node, "name");
            if (name == null)
            {
                name = getJsonText(node, "display_name");
            }
            if (name == null || name.isBlank())
            {
                return null;
            }

            String externalId = getJsonText(node, "id");
            if (externalId == null)
            {
                externalId = getJsonText(node, "product_id");
            }
            if (externalId == null)
            {
                externalId = String.valueOf(name.hashCode());
            }

            BigDecimal price = null;
            BigDecimal originalPrice = null;
            boolean onSale = false;

            // Intentar varios formatos de precio
            if (node.has("price"))
            {
                JsonNode priceNode = node.get("price");
                if (priceNode.isObject())
                {
                    if (priceNode.has("current"))
                    {
                        JsonNode currentPrice = priceNode.get("current");
                        price = currentPrice.isObject() ? getJsonDecimal(currentPrice, "amount") : new BigDecimal(currentPrice.asText());
                    }
                    if (priceNode.has("original"))
                    {
                        JsonNode origPrice = priceNode.get("original");
                        originalPrice = origPrice.isObject() ? getJsonDecimal(origPrice, "amount") : new BigDecimal(origPrice.asText());
                    }
                    if (price == null)
                    {
                        price = getJsonDecimal(priceNode, "price");
                    }
                }
                else
                {
                    price = new BigDecimal(priceNode.asText());
                }
            }
            if (price == null)
            {
                price = getJsonDecimal(node, "unit_price");
            }

            if (originalPrice != null && price != null && originalPrice.compareTo(price) > 0)
            {
                onSale = true;
            }

            String imageUrl = getJsonText(node, "image");
            if (imageUrl == null)
            {
                imageUrl = getJsonText(node, "thumbnail");
            }
            if (imageUrl == null && node.has("images") && node.get("images").isArray() && node.get("images").size() > 0)
            {
                imageUrl = node.get("images").get(0).asText();
            }

            String productUrl = getJsonText(node, "url");
            if (productUrl == null)
            {
                productUrl = getJsonText(node, "link");
            }

            String ean = getJsonText(node, "ean");
            String brand = getJsonText(node, "brand");
            if (brand == null)
            {
                brand = extractBrandFromName(name);
            }

            return ScrapedProduct.builder()
                .externalId(externalId)
                .ean(ean)
                .name(decodeUnicode(name))
                .brand(brand)
                .description(getJsonText(node, "description"))
                .price(price)
                .originalPrice(originalPrice)
                .onSale(onSale)
                .pricePerUnit(null)
                .unit(null)
                .imageUrl(imageUrl)
                .productUrl(productUrl != null ? (productUrl.startsWith("http") ? productUrl : baseUrl + productUrl) : null)
                .categoryName(categoryName)
                .categoryId(categoryId)
                .origin(null)
                .build();
        }
        catch (Exception e)
        {
            log.debug("[{}] Error parseando producto embebido: {}", STORE_NAME, e.getMessage());
            return null;
        }
    }

    /**
     * Extrae productos usando selectores CSS (fallback).
     * Basado en los selectores documentados de Carrefour.
     */
    private List<ScrapedProduct> extractFromCssSelectors(Document doc, String categoryName,
                                                          String categoryId, Set<String> seenIds)
    {
        List<ScrapedProduct> products = new ArrayList<>();

        // Selectores principales de Carrefour
        Elements productElements = doc.select(".product-card-item");

        // Selectores alternativos si el principal no encuentra nada
        if (productElements.isEmpty())
        {
            productElements = doc.select("[data-testid='product-card'], .product-card, .plp-food-search-product");
        }

        for (Element el : productElements)
        {
            try
            {
                String name = extractText(el,
                    ".product-card-item__title",
                    ".product-card__title",
                    "[data-testid='product-card-name']",
                    ".product-title");

                String priceText = extractText(el,
                    ".product-card-item__price",
                    ".product-card__price",
                    "[data-testid='product-card-price']",
                    ".product-price");

                String oldPriceText = extractText(el,
                    ".product-card-item__price--strikethrough",
                    ".product-card__price--old",
                    "[data-testid='product-card-original-price']");

                String imageUrl = extractAttr(el, "src",
                    ".product-card-item__image img",
                    ".product-card__image img",
                    "img[data-testid='product-card-image']",
                    "img");

                String productUrl = extractAttr(el, "href",
                    "a.product-card-item__link",
                    "a.product-card__link",
                    "a[data-testid='product-card-link']",
                    "a");

                String pricePerUnit = extractText(el,
                    ".product-card-item__price-per-unit",
                    ".product-card__price-per-unit");

                if (name == null || name.isBlank())
                {
                    continue;
                }

                BigDecimal price = parsePrice(priceText);
                if (price == null)
                {
                    continue;
                }

                String externalId = el.attr("data-product-id");
                if (externalId.isBlank())
                {
                    externalId = extractIdFromUrl(productUrl);
                }
                if (externalId == null || externalId.isBlank())
                {
                    externalId = String.valueOf(name.hashCode());
                }

                if (seenIds.contains(externalId))
                {
                    continue;
                }
                seenIds.add(externalId);

                BigDecimal originalPrice = parsePrice(oldPriceText);
                boolean onSale = originalPrice != null && originalPrice.compareTo(price) > 0;

                ScrapedProduct product = ScrapedProduct.builder()
                    .externalId(externalId)
                    .ean(null)
                    .name(name.trim())
                    .brand(extractBrandFromName(name))
                    .description(null)
                    .price(price)
                    .originalPrice(originalPrice)
                    .onSale(onSale)
                    .pricePerUnit(pricePerUnit)
                    .unit(null)
                    .imageUrl(imageUrl)
                    .productUrl(productUrl != null ? (productUrl.startsWith("http") ? productUrl : baseUrl + productUrl) : null)
                    .categoryName(categoryName)
                    .categoryId(categoryId)
                    .origin(null)
                    .build();

                products.add(product);
            }
            catch (Exception e)
            {
                log.debug("[{}] Error parseando producto CSS: {}", STORE_NAME, e.getMessage());
            }
        }

        return products;
    }

    // ==================== Metodos auxiliares ====================

    private String extractText(Element el, String... selectors)
    {
        for (String selector : selectors)
        {
            String text = el.select(selector).text();
            if (!text.isBlank())
            {
                return text;
            }
        }
        return null;
    }

    private String extractAttr(Element el, String attr, String... selectors)
    {
        for (String selector : selectors)
        {
            String value = el.select(selector).attr(attr);
            if (!value.isBlank())
            {
                return value;
            }
        }
        return null;
    }

    private String extractIdFromUrl(String url)
    {
        if (url == null)
        {
            return null;
        }
        // Extraer ID de URL tipo /supermercado/12345/p o /producto/nombre/R-12345
        Pattern idPattern = Pattern.compile("/(\\d{4,})/|/R-(\\d+)");
        Matcher matcher = idPattern.matcher(url);
        if (matcher.find())
        {
            return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }
        return null;
    }

    private String extractBrandFromName(String name)
    {
        if (name == null)
        {
            return null;
        }
        String nameLower = name.toLowerCase();

        String[] knownBrands = {
            "CARREFOUR", "CARREFOUR BIO", "CARREFOUR CLASSIC", "CARREFOUR EXTRA",
            "CARREFOUR NO GLUTEN", "CARREFOUR VEGGIE", "CARREFOUR BABY", "SIMPL",
            "CENTRAL LECHERA ASTURIANA", "PASCUAL", "PULEVA", "DANONE", "ACTIVIA",
            "NESTLÉ", "NESCAFÉ", "COCA-COLA", "PEPSI", "FONT VELLA", "BEZOYA",
            "MAHOU", "CRUZCAMPO", "ESTRELLA GALICIA", "PRESIDENT", "EL POZO",
            "CAMPOFRÍO", "OSCAR MAYER", "GALLO", "BUITONI", "CARBONELL"
        };

        for (String brand : knownBrands)
        {
            if (nameLower.contains(brand.toLowerCase()))
            {
                return brand;
            }
        }

        // Primera palabra si es todo mayusculas
        String[] words = name.split("\\s+");
        if (words.length > 0 && words[0].equals(words[0].toUpperCase()) && words[0].length() > 2)
        {
            return words[0];
        }

        return null;
    }

    private String extractCategoryId(String path)
    {
        Pattern pattern = Pattern.compile("/(cat\\d+)/");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return String.valueOf(path.hashCode());
    }

    private String extractCategoryName(String path)
    {
        // Extraer nombre de URL: /supermercado/lacteos-y-derivados/cat20003/c -> lacteos y derivados
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--)
        {
            if (!parts[i].startsWith("cat") && !parts[i].equals("c")
                && !parts[i].equals("supermercado") && !parts[i].isBlank())
            {
                return parts[i].replace("-", " ");
            }
        }
        return "Categoria";
    }

    private String decodeUnicode(String str)
    {
        if (str == null)
        {
            return null;
        }

        Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        Matcher matcher = unicodePattern.matcher(str);
        StringBuffer sb = new StringBuffer();

        while (matcher.find())
        {
            int codePoint = Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(sb, Character.toString((char) codePoint));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String getJsonText(JsonNode node, String field)
    {
        if (node != null && node.has(field) && !node.get(field).isNull())
        {
            return node.get(field).asText();
        }
        return null;
    }

    private BigDecimal getJsonDecimal(JsonNode node, String field)
    {
        if (node != null && node.has(field) && !node.get(field).isNull())
        {
            try
            {
                return new BigDecimal(node.get(field).asText());
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
        return null;
    }

    // ==================== Metodos publicos para el servicio ====================

    public List<ScrapedProduct> scrapeCategory(String categoryPath)
    {
        log.info("[{}] Obteniendo productos de categoria {}", STORE_NAME, categoryPath);
        String url = categoryPath.startsWith("http") ? categoryPath : baseUrl + categoryPath;
        String id = extractCategoryId(categoryPath);
        String name = extractCategoryName(categoryPath);
        return scrapeCategoryPages(url, name, id);
    }

    public List<PublicCategoryInfo> fetchAllCategories()
    {
        List<PublicCategoryInfo> categories = new ArrayList<>();
        for (String path : getCategoryUrls())
        {
            String id = extractCategoryId(path);
            String name = extractCategoryName(path);
            categories.add(new PublicCategoryInfo(id, name, baseUrl + path, "Supermercado"));
        }
        return categories;
    }

    // ==================== Enriquecimiento EAN desde paginas de detalle ====================

    public record ProductDetail(String externalId, String ean) {}

    /**
     * Obtiene el EAN de un producto de Carrefour accediendo a su pagina de detalle.
     * Extrae el campo gtin13/gtin del JSON-LD embebido en la pagina.
     */
    public ProductDetail fetchProductDetail(String externalId, String productUrl)
    {
        if (productUrl == null || productUrl.isBlank())
        {
            return null;
        }

        try
        {
            rateLimiter.waitIfNeeded();
            Document doc = fetchPage(productUrl);
            String html = doc.html();

            // Estrategia 1: JSON-LD (schema.org) - buscar gtin13/gtin en Product
            String ean = extractEanFromJsonLd(doc);

            // Estrategia 2: Buscar en JSON embebido (__NEXT_DATA__ u otros)
            if (ean == null)
            {
                ean = extractEanFromEmbeddedJson(html);
            }

            // Estrategia 3: Buscar directamente en el HTML con regex
            if (ean == null)
            {
                ean = extractEanFromHtml(html);
            }

            if (ean != null)
            {
                log.debug("[{}] EAN encontrado para {}: {}", STORE_NAME, externalId, ean);
            }
            else
            {
                log.debug("[{}] No se encontro EAN para {}", STORE_NAME, externalId);
            }

            return new ProductDetail(externalId, ean);
        }
        catch (Exception e)
        {
            log.warn("[{}] Error obteniendo detalle de {}: {}", STORE_NAME, productUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Obtiene detalles de multiples productos en paralelo.
     * Usa las URLs almacenadas en ProductStore para acceder a las paginas de detalle.
     */
    public Map<String, ProductDetail> fetchProductDetails(List<ProductStore> productStores)
    {
        ConcurrentHashMap<String, ProductDetail> details = new ConcurrentHashMap<>();

        List<ProductStore> withUrl = productStores.stream()
            .filter(ps -> ps.getUrl() != null && !ps.getUrl().isBlank())
            .filter(ps -> ps.getExternaId() != null)
            .toList();

        log.info("[{}] Obteniendo detalles de {} productos en paralelo ({} con URL)...",
                 STORE_NAME, productStores.size(), withUrl.size());

        if (withUrl.isEmpty())
        {
            return details;
        }

        int parallelism = 5;
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger errors = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = withUrl.stream()
            .map(ps -> CompletableFuture.runAsync(() -> {
                try
                {
                    ProductDetail detail = fetchProductDetail(ps.getExternaId(), ps.getUrl());
                    if (detail != null && detail.ean() != null)
                    {
                        details.put(ps.getExternaId(), detail);
                    }
                    int count = processed.incrementAndGet();
                    if (count % 50 == 0)
                    {
                        log.info("[{}] Progreso enriquecimiento: {}/{} productos procesados",
                                 STORE_NAME, count, withUrl.size());
                    }
                }
                catch (Exception e)
                {
                    errors.incrementAndGet();
                    log.warn("[{}] Error obteniendo detalle de {}: {}",
                             STORE_NAME, ps.getExternaId(), e.getMessage());
                }
            }, executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        log.info("[{}] Detalles obtenidos: {}/{} productos con EAN ({} errores)",
                 STORE_NAME, details.size(), withUrl.size(), errors.get());

        return details;
    }

    /**
     * Extrae EAN del JSON-LD (schema.org Product) de una pagina de detalle.
     */
    private String extractEanFromJsonLd(Document doc)
    {
        Elements jsonLdScripts = doc.select("script[type=application/ld+json]");
        for (Element script : jsonLdScripts)
        {
            try
            {
                String json = script.html();
                JsonNode root = objectMapper.readTree(json);

                // Producto directo
                if (root.has("@type") && "Product".equals(root.get("@type").asText()))
                {
                    return extractEanFromProductNode(root);
                }

                // Array de objetos (puede contener Product)
                if (root.isArray())
                {
                    for (JsonNode node : root)
                    {
                        if (node.has("@type") && "Product".equals(node.get("@type").asText()))
                        {
                            String ean = extractEanFromProductNode(node);
                            if (ean != null)
                            {
                                return ean;
                            }
                        }
                    }
                }

                // @graph (JSON-LD con grafo)
                if (root.has("@graph"))
                {
                    for (JsonNode node : root.get("@graph"))
                    {
                        if (node.has("@type") && "Product".equals(node.get("@type").asText()))
                        {
                            String ean = extractEanFromProductNode(node);
                            if (ean != null)
                            {
                                return ean;
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                log.debug("[{}] Error parseando JSON-LD para EAN: {}", STORE_NAME, e.getMessage());
            }
        }
        return null;
    }

    private String extractEanFromProductNode(JsonNode node)
    {
        // gtin13 es el campo estandar para EAN-13
        String ean = getJsonText(node, "gtin13");
        if (ean != null && isValidEan(ean))
        {
            return ean;
        }

        // gtin generico
        ean = getJsonText(node, "gtin");
        if (ean != null && isValidEan(ean))
        {
            return ean;
        }

        // gtin14
        ean = getJsonText(node, "gtin14");
        if (ean != null && ean.length() == 14 && ean.matches("\\d+"))
        {
            return ean;
        }

        // gtin8
        ean = getJsonText(node, "gtin8");
        if (ean != null && ean.length() == 8 && ean.matches("\\d+"))
        {
            return ean;
        }

        // sku a veces contiene el EAN
        String sku = getJsonText(node, "sku");
        if (sku != null && isValidEan(sku))
        {
            return sku;
        }

        // Buscar en offers
        if (node.has("offers"))
        {
            JsonNode offers = node.get("offers");
            if (offers.isArray() && offers.size() > 0)
            {
                offers = offers.get(0);
            }
            ean = getJsonText(offers, "gtin13");
            if (ean != null && isValidEan(ean))
            {
                return ean;
            }
            ean = getJsonText(offers, "gtin");
            if (ean != null && isValidEan(ean))
            {
                return ean;
            }
        }

        return null;
    }

    /**
     * Extrae EAN de JSON embebido en scripts (__NEXT_DATA__, etc.).
     */
    private String extractEanFromEmbeddedJson(String html)
    {
        // Buscar patron "ean":"1234567890123"
        Pattern eanPattern = Pattern.compile("\"ean\"\\s*:\\s*\"(\\d{8,14})\"");
        Matcher matcher = eanPattern.matcher(html);
        if (matcher.find())
        {
            String ean = matcher.group(1);
            if (isValidEan(ean))
            {
                return ean;
            }
        }

        // Buscar patron "gtin13":"1234567890123"
        Pattern gtinPattern = Pattern.compile("\"gtin13\"\\s*:\\s*\"(\\d{13})\"");
        matcher = gtinPattern.matcher(html);
        if (matcher.find())
        {
            return matcher.group(1);
        }

        // Buscar patron "gtin":"1234567890123"
        Pattern gtinGenPattern = Pattern.compile("\"gtin\"\\s*:\\s*\"(\\d{8,14})\"");
        matcher = gtinGenPattern.matcher(html);
        if (matcher.find())
        {
            String ean = matcher.group(1);
            if (isValidEan(ean))
            {
                return ean;
            }
        }

        return null;
    }

    /**
     * Busca EAN directamente en el HTML como ultimo recurso.
     */
    private String extractEanFromHtml(String html)
    {
        // Buscar en atributos data-ean
        Pattern dataEanPattern = Pattern.compile("data-ean=\"(\\d{8,14})\"");
        Matcher matcher = dataEanPattern.matcher(html);
        if (matcher.find())
        {
            String ean = matcher.group(1);
            if (isValidEan(ean))
            {
                return ean;
            }
        }

        // Buscar etiquetas con texto "EAN" seguido de un codigo
        Pattern labelPattern = Pattern.compile("(?:EAN|C.digo de barras|GTIN)[:\\s]+(\\d{8,14})");
        matcher = labelPattern.matcher(html);
        if (matcher.find())
        {
            String ean = matcher.group(1);
            if (isValidEan(ean))
            {
                return ean;
            }
        }

        return null;
    }

    private boolean isValidEan(String ean)
    {
        if (ean == null || !ean.matches("\\d+"))
        {
            return false;
        }
        // EAN-8, EAN-13 o GTIN-14
        return ean.length() == 8 || ean.length() == 13 || ean.length() == 14;
    }

    public record PublicCategoryInfo(String id, String name, String url, String parentCategory) {}
}
