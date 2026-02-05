package com.smartcart.smartcart.modules.scraping.scraper;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AlcampoScraper extends BaseScraper
{

    private static final String STORE_NAME = "alcampo";
    private static final Long STORE_ID = 2L;
    private static final String BASE_URL = "https://www.compraonline.alcampo.es";

    private String baseUrl;
    private final RestTemplate restTemplate;

    public AlcampoScraper()
    {
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init()
    {
        initFromConfig();
        ScrapingConfig.StoreConfig alcampoConfig = scrapingConfig.getStoreConfig(STORE_NAME);
        if (alcampoConfig != null && alcampoConfig.getBaseUrl() != null)
        {
            this.baseUrl = alcampoConfig.getBaseUrl();
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
        // URLs con caracteres especiales URL-encoded - Ampliado para más productos
        return List.of(
            // Frescos y perecederos
            "/categories/frescos/OC2112",
            "/categories/carnes-aves-y-caza/OC2113",
            "/categories/pescados-y-mariscos/OC2114",
            "/categories/frutas-y-verduras/OC2115",
            "/categories/panader%C3%ADa-y-pasteler%C3%ADa/OC2116",
            "/categories/embutidos-y-quesos/OC2117",
            // Lácteos y bebidas
            "/categories/leche-huevos-l%C3%A1cteos-yogures-y-bebidas-vegetales/OC16",
            "/categories/agua-refrescos-y-zumos/OC19",
            "/categories/cervezas-vinos-y-licores/OC20",
            "/categories/caf%C3%A9s-t%C3%A9s-e-infusiones/OC18",
            // Despensa
            "/categories/arroz-legumbres-y-pasta/OC13",
            "/categories/aceites-vinagres-y-condimentos/OC12",
            "/categories/conservas-caldos-y-sopas/OC14",
            "/categories/galletas-boller%C3%ADa-y-cereales/OC15",
            "/categories/chocolates-dulces-y-frutos-secos/OC17",
            // Congelados y preparados
            "/categories/congelados/OC22",
            "/categories/comida-preparada/OC20022018",
            // Desayuno y especiales
            "/categories/desayuno-y-merienda/OC10",
            "/categories/supermercado-ecol%C3%B3gico/OC26112021",
            "/categories/veganos/OC09112021"
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
            List<CategoryInfo> categories = fetchCategories();
            log.info("[{}] Encontradas {} categorias, procesando en paralelo...", STORE_NAME, categories.size());

            int parallelism = 5;
            ExecutorService executor = Executors.newFixedThreadPool(parallelism);

            List<CompletableFuture<Void>> futures = categories.stream()
                .map(category -> CompletableFuture.runAsync(() -> {
                    try
                    {
                        rateLimiter.waitIfNeeded();
                        List<ScrapedProduct> products = scrapeCategoryPage(category.url, category.name, category.id);
                        allProducts.addAll(products);

                        int count = processedCategories.incrementAndGet();
                        log.info("[{}] Categoria '{}' procesada: {} productos ({}/{})",
                                 STORE_NAME, category.name, products.size(), count, categories.size());
                    }
                    catch (Exception e)
                    {
                        errors.incrementAndGet();
                        log.error("[{}] Error en categoria {}: {}",
                                  STORE_NAME, category.name, e.getMessage());
                        result.addError("category-" + category.id, e.getMessage());
                    }
                }, executor))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            executor.shutdown();
        }
        catch (Exception e)
        {
            log.error("[{}] Error obteniendo categorias: {}", STORE_NAME, e.getMessage());
            errors.incrementAndGet();
            result.addError("categories", e.getMessage());
        }

        result.setProducts(new ArrayList<>(allProducts));
        result.setEndTime(LocalDateTime.now());
        result.setTotalProducts(allProducts.size());
        result.setTotalErrors(errors.get());

        log.info("[{}] Scraping completado: {} productos, {} errores en {}s",
                 STORE_NAME, allProducts.size(), errors.get(), result.getDurationSeconds());

        return result;
    }

    private List<CategoryInfo> fetchCategories()
    {
        List<CategoryInfo> categories = new ArrayList<>();

        try
        {
            Document doc = fetchPage(baseUrl + "/categories");
            Elements categoryLinks = doc.select("a[href^='/categories/']");

            Set<String> seen = new HashSet<>();
            for (Element link : categoryLinks)
            {
                String href = link.attr("href");
                String name = link.text().trim();

                if (!href.isBlank() && !seen.contains(href) && href.contains("/OC"))
                {
                    seen.add(href);
                    String id = extractCategoryId(href);
                    String url = baseUrl + href;
                    if (name.isBlank())
                    {
                        name = extractCategoryName(href);
                    }
                    categories.add(new CategoryInfo(url, name, id));
                }
            }

            if (categories.isEmpty())
            {
                log.warn("[{}] No se encontraron categorias, usando lista predefinida", STORE_NAME);
                return getKnownCategories();
            }

            log.info("[{}] Encontradas {} categorias", STORE_NAME, categories.size());
            return categories;
        }
        catch (Exception e)
        {
            log.error("[{}] Error obteniendo categorias: {}", STORE_NAME, e.getMessage());
            return getKnownCategories();
        }
    }

    private List<CategoryInfo> getKnownCategories()
    {
        List<CategoryInfo> cats = getCategoryUrls().stream()
            .map(path -> {
                String id = extractCategoryId(path);
                String name = extractCategoryName(path);
                String url = baseUrl + path;
                log.info("[{}] Categoria conocida: {} -> {}", STORE_NAME, name, url);
                return new CategoryInfo(url, name, id);
            })
            .toList();
        log.info("[{}] Total categorias conocidas: {}", STORE_NAME, cats.size());
        return cats;
    }

    private List<ScrapedProduct> scrapeCategoryPage(String categoryUrl, String categoryName, String categoryId)
    {
        List<ScrapedProduct> products = new ArrayList<>();
        Set<String> seenProductIds = new HashSet<>();
        int page = 0;
        int maxPages = 15;  // Aumentado de 5 a 15 para obtener más productos
        int emptyPages = 0;

        while (page < maxPages && emptyPages < 2)
        {
            try
            {
                String url = categoryUrl + "?sortBy=favorite&page=" + page;
                log.debug("[{}] Fetching: {}", STORE_NAME, url);

                Document doc = fetchPage(url);
                String html = doc.html();

                List<ScrapedProduct> pageProducts = extractProductsFromJson(html, categoryName, categoryId, seenProductIds);

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

                if (page < maxPages)
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
            // Usar RestTemplate con headers identicos a Chrome para evitar deteccion de bots
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

            // Convertir el HTML string a Document de Jsoup para mantener compatibilidad
            return Jsoup.parse(html, url);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error fetching page: " + url, e);
        }
    }

    private List<ScrapedProduct> extractProductsFromJson(String html, String categoryName, String categoryId, Set<String> seenIds)
    {
        List<ScrapedProduct> products = new ArrayList<>();

        // Buscar productos en productEntities del HTML
        // Formato real: "productId":"uuid","retailerProductId":"77081","name":"ALCAMPO...","available":true,...,"price":{"current":{"amount":"2.45"
        Pattern pattern = Pattern.compile(
            "\"productId\":\"([a-f0-9-]{36})\",\"retailerProductId\":\"(\\d+)\",\"name\":\"([^\"]+)\",\"available\":(true|false)[^}]*?\"price\":\\{\"current\":\\{\"amount\":\"([\\d.]+)\""
        );

        Matcher matcher = pattern.matcher(html);

        log.info("[{}] Buscando productos en HTML de {} caracteres para categoria {}", STORE_NAME, html.length(), categoryName);

        // Debug: verificar que el HTML contiene productEntities
        boolean hasProductEntities = html.contains("\"productEntities\":");
        boolean hasRetailerId = html.contains("\"retailerProductId\":");
        log.info("[{}] HTML contiene 'productEntities': {}, contiene 'retailerProductId': {}", STORE_NAME, hasProductEntities, hasRetailerId);

        int matchCount = 0;
        while (matcher.find())
        {
            try
            {
                String productUuid = matcher.group(1);
                String externalId = matcher.group(2); // retailerProductId
                String name = matcher.group(3);
                String priceStr = matcher.group(5); // grupo 5 porque grupos 1-4 son uuid, retailerId, name, available

                // Saltar si ya lo vimos
                if (seenIds.contains(externalId))
                {
                    continue;
                }
                seenIds.add(externalId);

                // Validar datos
                if (name == null || name.length() < 5)
                {
                    continue;
                }

                BigDecimal price;
                try
                {
                    price = new BigDecimal(priceStr);
                    if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(new BigDecimal("10000")) > 0)
                    {
                        continue;
                    }
                }
                catch (Exception e)
                {
                    continue;
                }

                // Decodificar caracteres Unicode en el nombre
                name = decodeUnicode(name);

                String brand = extractBrandFromName(name);
                String productUrl = baseUrl + "/products/" + slugify(name) + "/" + productUuid;

                // Buscar imagen usando UUID del producto
                String imageUrl = extractImageForProduct(html, productUuid);

                ScrapedProduct product = ScrapedProduct.builder()
                    .externalId(externalId)
                    .ean(null)
                    .name(name)
                    .brand(brand)
                    .description(null)
                    .price(price)
                    .originalPrice(null)
                    .onSale(false)
                    .pricePerUnit(null)
                    .unit(null)
                    .imageUrl(imageUrl)
                    .productUrl(productUrl)
                    .categoryName(categoryName)
                    .categoryId(categoryId)
                    .origin(null)
                    .build();

                products.add(product);
                matchCount++;
            }
            catch (Exception e)
            {
                log.warn("[{}] Error parseando producto: {}", STORE_NAME, e.getMessage());
            }
        }

        log.info("[{}] Encontrados {} matches, {} productos validos en {}", STORE_NAME, matchCount, products.size(), categoryName);
        return products;
    }

    private String extractImageForProduct(String html, String productId)
    {
        // Buscar imagen en formato JSON de Alcampo: "image":{"src":"https:\u002F\u002F..."}
        // El productId está cerca del bloque de imagen en el JSON

        // Primero buscar el bloque del producto por su UUID
        int productIndex = html.indexOf("\"productId\":\"" + productId + "\"");
        if (productIndex == -1)
        {
            return null;
        }

        // Buscar la imagen dentro de un rango razonable después del productId
        String searchArea = html.substring(productIndex, Math.min(productIndex + 2000, html.length()));

        // Patrón para "image":{"src":"URL"} con posibles escapes Unicode
        Pattern imgPattern = Pattern.compile(
            "\"image\"\\s*:\\s*\\{\\s*\"src\"\\s*:\\s*\"([^\"]+)\""
        );
        Matcher matcher = imgPattern.matcher(searchArea);
        if (matcher.find())
        {
            String imageUrl = matcher.group(1);
            // Decodificar Unicode escapes como \u002F -> /
            imageUrl = decodeUnicode(imageUrl);
            return imageUrl;
        }

        // Formato alternativo: buscar directamente URL de imagen de Alcampo
        Pattern altPattern = Pattern.compile(
            "https?:\\\\u002F\\\\u002Fwww\\.compraonline\\.alcampo\\.es\\\\u002Fimages[^\"\\s]+"
        );
        Matcher altMatcher = altPattern.matcher(searchArea);
        if (altMatcher.find())
        {
            String imageUrl = altMatcher.group();
            imageUrl = decodeUnicode(imageUrl);
            return imageUrl;
        }

        return null;
    }

    private String decodeUnicode(String str)
    {
        if (str == null) return null;

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

    private String slugify(String name)
    {
        if (name == null) return "";
        return name.toLowerCase()
            .replaceAll("[áàäâ]", "a")
            .replaceAll("[éèëê]", "e")
            .replaceAll("[íìïî]", "i")
            .replaceAll("[óòöô]", "o")
            .replaceAll("[úùüû]", "u")
            .replaceAll("[ñ]", "n")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim();
    }

    private String extractBrandFromName(String name)
    {
        if (name == null) return null;
        String nameLower = name.toLowerCase();

        String[] knownBrands = {
            "AUCHAN", "ALCAMPO", "PITAS", "PASCUAL", "DANONE", "PULEVA",
            "CENTRAL LECHERA ASTURIANA", "NESTLÉ", "NESCAFÉ", "COCA-COLA", "PEPSI",
            "FONT VELLA", "BEZOYA", "MAHOU", "CRUZCAMPO", "ESTRELLA GALICIA",
            "PRESIDENT", "ACTIVIA", "L R", "MMM!"
        };

        for (String brand : knownBrands)
        {
            if (nameLower.contains(brand.toLowerCase()))
            {
                return brand;
            }
        }

        // Extraer primera palabra si parece marca (mayúsculas)
        String[] words = name.split("\\s+");
        if (words.length > 0 && words[0].equals(words[0].toUpperCase()) && words[0].length() > 2)
        {
            return words[0];
        }

        return null;
    }

    private String extractCategoryId(String path)
    {
        Pattern pattern = Pattern.compile("/(OC\\d+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return String.valueOf(path.hashCode());
    }

    private String extractCategoryName(String path)
    {
        String[] parts = path.split("/");
        for (int i = parts.length - 1; i >= 0; i--)
        {
            if (!parts[i].startsWith("OC") && !parts[i].isBlank())
            {
                return parts[i].replace("-", " ");
            }
        }
        return "Categoria";
    }

    public List<ScrapedProduct> scrapeCategory(String categoryPath)
    {
        log.info("[{}] Obteniendo productos de categoria {}", STORE_NAME, categoryPath);
        String url = categoryPath.startsWith("http") ? categoryPath : baseUrl + categoryPath;
        String id = extractCategoryId(categoryPath);
        String name = extractCategoryName(categoryPath);
        return scrapeCategoryPage(url, name, id);
    }

    public List<PublicCategoryInfo> fetchAllCategories()
    {
        List<PublicCategoryInfo> categories = new ArrayList<>();

        try
        {
            Document doc = fetchPage(baseUrl + "/categories");
            Elements categoryLinks = doc.select("a[href^='/categories/']");

            Set<String> seen = new HashSet<>();
            for (Element link : categoryLinks)
            {
                String href = link.attr("href");
                String name = link.text().trim();

                if (!href.isBlank() && !seen.contains(href) && href.contains("/OC"))
                {
                    seen.add(href);
                    String id = extractCategoryId(href);
                    String url = baseUrl + href;
                    if (name.isBlank())
                    {
                        name = extractCategoryName(href);
                    }
                    categories.add(new PublicCategoryInfo(id, name, url, null));
                }
            }

            if (categories.isEmpty())
            {
                for (String path : getCategoryUrls())
                {
                    String id = extractCategoryId(path);
                    String name = extractCategoryName(path);
                    categories.add(new PublicCategoryInfo(id, name, baseUrl + path, null));
                }
            }
        }
        catch (Exception e)
        {
            log.error("[{}] Error obteniendo categorias: {}", STORE_NAME, e.getMessage());
            for (String path : getCategoryUrls())
            {
                String id = extractCategoryId(path);
                String name = extractCategoryName(path);
                categories.add(new PublicCategoryInfo(id, name, baseUrl + path, null));
            }
        }

        return categories;
    }

    private record CategoryInfo(String url, String name, String id) {}

    public record PublicCategoryInfo(String id, String name, String url, String parentCategory) {}
}
